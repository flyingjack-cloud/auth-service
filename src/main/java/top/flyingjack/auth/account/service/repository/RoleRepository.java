package top.flyingjack.auth.account.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.flyingjack.auth.account.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {}
