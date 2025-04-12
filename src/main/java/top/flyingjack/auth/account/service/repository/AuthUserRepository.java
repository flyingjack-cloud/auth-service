package top.flyingjack.auth.account.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import top.flyingjack.auth.account.entity.AuthUser;

import java.util.Optional;

/**
 * @author Zumin Li
 * @date 2025/4/3 0:43
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findAuthUserByUsername(String username);
    Optional<AuthUser> findAuthUserByPhone(String phone);
    Optional<AuthUser> findAuthUserByEmail(String email);

    boolean existsAuthUserByEmail(String email);
    boolean existsAuthUserByPhone(String phone);
    boolean existsAuthUserByUsername(String username);

    @Query("update AuthUser a set a.password = :password where a.email = :email")
    @Modifying
    @Transactional
    void updatePasswordByEmail(String password, String email);

    @Query("update AuthUser a set a.password = :password where a.phone = :phone")
    @Modifying
    @Transactional
    void updatePasswordByPhone(String password, String phone);
}
