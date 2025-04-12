package top.flyingjack.auth.oauth2.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 授权码返回
 *
 * @author Zumin Li
 * @date 2025/4/2 17:29
 */
@Schema(name = "authorization_code_res", description = "授权码返回")
public record OAuthAuthorizationCodeRes(
        @Schema(name = "authorization_code", description = "授权码")
        String code
) { }
