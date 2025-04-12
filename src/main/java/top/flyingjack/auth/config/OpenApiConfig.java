package top.flyingjack.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.auth.oauth2.entity.OAuthAuthorizationCodeRes;


/**
 * Swagger服务配置 TODO 后续应当设置为只在开发和测试环境启用
 *
 * @author Zumin Li
 * @date 2025/4/2 14:06
 */
@Configuration
@Profile({"dev", "beta"}) // 只在开发和beta环境下可以激活
public class OpenApiConfig {
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理")     // 分组名称
                .pathsToMatch("/account/**")  // 匹配的接口路径
                .build();
    }

    @Bean
    public GroupedOpenApi oauthApi() {
        return GroupedOpenApi.builder()
                .group("OAuth2客户端管理")
                .pathsToMatch("/clients/**")
                .build();
    }

    /*
        添加oauth2接口到springdoc
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        // 配置API的信息
        openAPI.info(new Info()
                .title("OAuth2 Server API")
                .version("1.0")
                .description("Oauth2授权服务器接口"));

        Paths paths = new Paths();

        // 添加Oauth2 endpoints到文档方便测试
        // 1. OAuth2 Authorization 端点
        Operation authorizeOperation = new Operation()
                .summary("OAuth2 Authorization Endpoint")
                .description("用户授权端点（Authorization Code 模式）, 请保证已经是登录状态")
                .addTagsItem("OAuth2")
                // 入参
                .addParametersItem(new QueryParameter()
                        .name("response_type")
                        .description("授权类型")
                        .in("query")
                        .required(true)
                        .example("code"))
                .addParametersItem(new QueryParameter()
                        .name("client_id")
                        .description("客户端ID")
                        .in("query")
                        .required(true)
                        .example("swagger-client"))
                .addParametersItem(new QueryParameter()
                        .name("scope")
                        .description("授权scope")
                        .in("query")
                        .required(true)
                        .example("openid"))
                .addParametersItem(new QueryParameter()
                        .name("redirect_uri")
                        .description("跳转地址(按照实际地址拼接，必须完全一致)")
                        .in("query")
                        .required(true)
                        .example("http://localhost:9000/swagger-ui/oauth2-redirect.html"))
                .addParametersItem(new QueryParameter()
                        .name("code_challenge")
                        .description("code_verifier的变换值")
                        .in("query")
                        .required(true)
                        .example("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&"))
                .addParametersItem(new QueryParameter()
                        .name("code_challenge_method")
                        .description("变换方法， 务必使用S256保证安全")
                        .in("query")
                        .required(true)
                        .example("S256"))
                // 配置返回
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("成功获取用户列表")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                        .schema(new Schema<ApiRes<OAuthAuthorizationCodeRes>>())
                                                .example(ApiRes.success(
                                                        new OAuthAuthorizationCodeRes("8mQXCFYyy_Zmfkdq-HZ-GLlZ2FXf1CNz5qoET4hiHVuFqy3FSX6oU657rRoHwZGJBeGkhshnWjuU9EBAeXf9PnfsuWND2E7pfA0nttG_8U-JbeNzavslx7X6YUkJPFFg")
                                                ))))
                        )
                );
        paths.addPathItem("/oauth2/authorize", new PathItem().get(authorizeOperation));
        return openAPI.paths(paths);
    }
}
