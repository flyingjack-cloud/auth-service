package top.flyingjack.auth.oauth2.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;
import top.flyingjack.auth.oauth2.entity.CustomOauth2ClientEntity;
import top.flyingjack.auth.oauth2.entity.RegisteredClientMapper;

/**
 * JdbcRegisteredClientRepository拓展
 * - save时同步保存detail
 *
 * @author Zumin Li
 * @date 2025/4/4 11:45
 */
@Transactional
public class CustomOAuth2ClientRepository implements RegisteredClientRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2ClientRepository.class);

    private final CustomOAuth2ClientEntityRepository clientEntityRepository;
    // 数据库entity和实际对象转化mapper
    private final RegisteredClientMapper mapper;

    public CustomOAuth2ClientRepository(CustomOAuth2ClientEntityRepository clientEntityRepository,
                                        ObjectMapper objectMapper) {
        this.clientEntityRepository = clientEntityRepository;
        this.mapper = new RegisteredClientMapper(objectMapper);
    }

    /**
     * 保存client到数据库
     *
     * @param registeredClient the {@link RegisteredClient}
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        CustomOauth2ClientEntity entity = mapper.toEntity(registeredClient);

        // 如果已经存在，则更新
        clientEntityRepository.findByClientId(registeredClient.getClientId()).ifPresent(value ->{
            logger.debug("{} is existing, updating instead of creating", value.getClientId());
            entity.setId(value.getId());
            entity.setClientIdIssuedAt(value.getClientIdIssuedAt());
        });

        clientEntityRepository.save(entity);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientEntityRepository.findById(Long.parseLong(id)).map(mapper::toDomain).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientEntityRepository.findByClientId(clientId).map(mapper::toDomain).orElse(null);
    }

    public void deleteByClientId(String clientId) {
        clientEntityRepository.deleteByClientId(clientId);
    }
}
