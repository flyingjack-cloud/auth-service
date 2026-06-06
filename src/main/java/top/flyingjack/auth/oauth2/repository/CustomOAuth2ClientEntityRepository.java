package top.flyingjack.auth.oauth2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.flyingjack.auth.oauth2.entity.CustomOauth2ClientEntity;

import java.util.Optional;

/**
 * Client Id 额外信息
 *
 * @author Zumin Li
 * @date 2025/4/4 11:43
 */
@Component
@Transactional
public interface CustomOAuth2ClientEntityRepository extends JpaRepository<CustomOauth2ClientEntity, Long> {
    Optional<CustomOauth2ClientEntity> findByClientId(String clientId);

    @Modifying
    void deleteByClientId(String clientId);

    boolean existsByClientId(String clientId);
}
