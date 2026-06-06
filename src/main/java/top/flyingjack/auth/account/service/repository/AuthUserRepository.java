package top.flyingjack.auth.account.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<AuthUser> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    @Query("update AuthUser a set a.password = :password where a.email = :email")
    @Modifying
    @Transactional
    void updatePasswordByEmail(String password, String email);

    @Query("update AuthUser a set a.password = :password where a.phone = :phone")
    @Modifying
    @Transactional
    void updatePasswordByPhone(String password, String phone);

    @Query("update AuthUser a set a.password = :password where a.id = :id")
    @Modifying
    @Transactional
    void updatePasswordById(String password, Long id);

    @Query("update AuthUser a set a.username = :username where a.id = :id")
    @Modifying
    @Transactional
    void updateUsernameById(String username, Long id);
}
