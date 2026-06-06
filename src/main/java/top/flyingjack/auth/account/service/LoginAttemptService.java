package top.flyingjack.auth.account.service;

import org.springframework.stereotype.Service;
import top.flyingjack.common.cache.CacheService;

/**
 * @author Zumin Li
 * @date 2025/4/14 22:42
 */
@Service
public class LoginAttemptService {
    private static final String HASH_KEY_HEADER = "LOGIN_ATTEMPT:";
    private static final long MONITOR_WINDOW = 600; // 观察窗口 600s

    private final CacheService cacheService;

    public LoginAttemptService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 当登录失败时原子性地递增失败计数并返回当前值
     *
     * @param principal    登录参数
     * @param resetWindow  是否重置过期窗口
     */
    public int record(String principal, boolean resetWindow) {
        String key = key(principal);
        // 使用 Redis INCR 原子递增，消除并发场景下的竞态条件
        long count = cacheService.incr(key, 1);
        // 首次记录时必须设置 TTL；resetWindow=true 时重置窗口
        if (resetWindow || count == 1) {
            cacheService.expire(key, MONITOR_WINDOW);
        }
        return (int) count;
    }

    public int record(String principal) {
        return record(principal, true);
    }

    /**
     * 登录成功后清除失败记录
     *
     * @param principal 登录参数
     */
    public void clear(String principal) {
        cacheService.del(key(principal));
    }

    public int count(String principal) {
        Object value = cacheService.get(key(principal));
        if (value == null) return 0;
        return ((Number) value).intValue(); // 兼容 Integer 和 Long 返回类型
    }

    public long expireRemain(String principal) {
        String key = key(principal);
        if (cacheService.hasKey(key)) {
            return cacheService.getExpire(key);
        }
        return 0;
    }

    private String key(String principal) {
        return HASH_KEY_HEADER + principal;
    }
}
