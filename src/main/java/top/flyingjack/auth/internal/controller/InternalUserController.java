package top.flyingjack.auth.internal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.flyingjack.auth.account.service.AccountService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.exception.BusinessException;

/**
 * 内部服务间接口 — 仅供同集群其他微服务调用，Istio NetworkPolicy 负责来源限制。
 *
 * @author Zumin Li
 */
@RestController
@RequestMapping("/internal")
public class InternalUserController {

    private final AccountService accountService;

    public InternalUserController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 按手机号查询用户 ID。
     *
     * @param phone 手机号
     * @return userId
     */
    @GetMapping("/users/by-phone")
    public ResponseEntity<ApiRes<Long>> getUserIdByPhone(@RequestParam String phone) {
        Long userId = accountService.findUserIdByPhone(phone);
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return ResponseEntity.ok(ApiRes.success(userId));
    }
}
