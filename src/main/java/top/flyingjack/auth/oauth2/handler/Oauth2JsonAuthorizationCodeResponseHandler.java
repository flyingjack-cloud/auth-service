package top.flyingjack.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.tool.ResponseUtils;
import top.flyingjack.auth.oauth2.entity.OAuthAuthorizationCodeRes;

import java.io.IOException;

/**
 * OAuth授权码模式，将默认的重定位返回改为json返回授权码
 *
 * @author Zumin Li
 * @date 2025/4/2 16:26
 */
public class Oauth2JsonAuthorizationCodeResponseHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthorizationCodeRequestAuthenticationToken authRequest = (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;

        // 构造 JSON 响应
        OAuthAuthorizationCodeRes codeRes = new OAuthAuthorizationCodeRes(authRequest.getAuthorizationCode().getTokenValue());
        ApiRes<OAuthAuthorizationCodeRes> res = ApiRes.success(codeRes);
        ResponseUtils.writeJsonResponse(response, res);
    }
}
