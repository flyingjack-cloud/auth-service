package top.flyingjack.auth.feign.fallback;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FallbackFactory;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.common.error.exception.BusinessException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CaptchaClient熔断测试
 *
 * @author Zumin Li
 * @date 2025/4/14 22:26
 */
class CaptchaClientFallbackFactoryTest {
    private FallbackFactory<CaptchaClient> factory = new CaptchaClientFallbackFactory();

    @Test
    public void should_pass_when_degrade() {
        CaptchaClient captchaClient = factory.create(new DegradeException("degrade"));
        assertTrue(captchaClient.verify(new CaptchaRequest(UUID.randomUUID().toString(), "test")));
    }

    @Test
    public void should_throw_when_flow_control() {
        CaptchaClient captchaClient = factory.create(new FlowException("degrade"));
        assertThrows(BusinessException.class, () -> captchaClient.verify(new CaptchaRequest(UUID.randomUUID().toString(),
                "test")));
    }
}