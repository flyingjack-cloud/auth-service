package top.flyingjack.auth.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.flyingjack.auth.feign.fallback.CaptchaClientFallbackFactory;
import top.flyingjack.common.dto.CaptchaRequest;

/**
 * 调用验证码服务
 *
 * @author Zumin Li
 * @date 2025/4/14 1:29
 */
@FeignClient(name = "thirdparty-service",
        path = "/captcha",
        fallbackFactory = CaptchaClientFallbackFactory.class
)
public interface CaptchaClient {
    @PostMapping("/verify")
    boolean verify(@RequestBody CaptchaRequest request);
}
