package top.flyingjack.auth.account.service;

import org.springframework.stereotype.Service;
import top.flyingjack.common.cache.CacheService;

/**
 * @author Zumin Li
 * @date 2025/4/14 22:42
 */
@Service
public class LoginAttemptService {
    // 储存在缓存时，key的前缀
    private final String HASH_KEY_HEADER = "LOGIN_ATTEMPT:";

    // 观察窗口600s
    private final long MONITOR_WINDOW = 600;

    private final CacheService cacheService;

    public LoginAttemptService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 当登录失败时记录失败并返回当前失败次数
     *
     * @param principal 登录参数
     * @param resetWindow 是否要重置过期时间
     */
    public int record(String principal, boolean resetWindow) {
        String key = key(principal);

        int attempCount = 1;
        if (cacheService.hasKey(key)) {
            attempCount += (Integer) cacheService.get(key);
        }

        cacheService.set(key, attempCount);

        if (resetWindow) {
            cacheService.expire(key, MONITOR_WINDOW);
        }

        return attempCount;
    }

    public int record(String principal) {
        return record(principal, true);
    }


    /**
     * 如果记录信息存在，删除
     *
     * @param principal 传入的参数
     */
    public void clear(String principal) {
        String key = key(principal);
        if (cacheService.hasKey(key)) {
            cacheService.del(key);
        }
    }

    public int count(String principal) {
        String key = key(principal);
        if (cacheService.hasKey(key)) {
            return (Integer) cacheService.get(key);
        }
        return 0;
    }

    public long expireRemain(String principal){
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