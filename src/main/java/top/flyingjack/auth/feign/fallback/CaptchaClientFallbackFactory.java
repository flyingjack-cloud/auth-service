package top.flyingjack.auth.feign.fallback;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.error.ErrorCode;

/**
 * CaptchaClient熔断/降级策略
 *
 * @author Zumin Li
 * @date 2025/4/14 13:12
 */
@Component
public class CaptchaClientFallbackFactory implements FallbackFactory<CaptchaClient> {
    private static final Logger log = LoggerFactory.getLogger(CaptchaClientFallbackFactory.class);

    @Override
    public CaptchaClient create(Throwable cause) {
        return new CaptchaClient() {
            @Override
            public boolean verify(CaptchaRequest request) {
                if (cause instanceof DegradeException) {
                    log.warn("Captcha service has been degraded, pass directly - {}", cause.toString());
                    // 熔断: 熔断后为了保证登录可用性，默认验证码可以直接通过
                    return true;
                } else if (cause instanceof FlowException) {
                    log.warn("Flow control on captcha client in auth service with rule - {}",
                            cause.toString());
                    // 流控： 抛出异常
                    throw new BusinessException(ErrorCode.FLOW_CONTROL);
                } else {
                    throw new BusinessException(ErrorCode.BUSINESS_DEFAULT);
                }
            }
        };
    }
}
