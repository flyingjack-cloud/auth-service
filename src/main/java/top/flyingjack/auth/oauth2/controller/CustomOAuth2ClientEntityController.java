package top.flyingjack.auth.oauth2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.tool.MessageTool;
import top.flyingjack.auth.oauth2.entity.CustomOauth2ClientEntity;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientEntityRepository;

import java.util.List;

/**
 * OAuth2 数据库储存操作Endpoints
 * CustomOAuth2ClientEntity Controller
 *
 * @author Zumin Li
 * @date 2025/4/4 16:51
 */
@RestController
@RequestMapping("/clients")
@Tag(name = "OAuth2 Client管理", description = "查询，修改，增加OAuth2注册的客户端")
public class CustomOAuth2ClientEntityController {
    private final CustomOAuth2ClientEntityRepository clientEntityRepository;
    private final MessageSource messageSource;

    public CustomOAuth2ClientEntityController(CustomOAuth2ClientEntityRepository clientEntityRepository, MessageSource messageSource) {
        this.clientEntityRepository = clientEntityRepository;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    @Operation(summary = "列出所有已经注册客户端")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiRes<List<CustomOauth2ClientEntity>>> clients()  {
        return ResponseEntity.ok(ApiRes.success(clientEntityRepository.findAll()));
    }

    @GetMapping("/{client_id}")
    @Operation(summary = "通过client_id查询客户端")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiRes<CustomOauth2ClientEntity>> getClientByClientId(
            @Parameter(description = "客户端id", required = true, example = "client")
            @PathVariable(name = "client_id")
            String clientId){
        return clientEntityRepository.findByClientId(clientId)
                .map( v -> ResponseEntity.ok(ApiRes.success(v)) )
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.error(HttpStatus.NOT_FOUND.value(),
                                MessageTool.getMessageByContext(messageSource, ErrorCode.CLIENT_NOT_FOUND.getId())))
                );
    }
}
