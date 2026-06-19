package top.flyingjack.auth.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.auth.admin.entity.dto.AdminUserDto;
import top.flyingjack.auth.admin.entity.dto.UpdateUserRolesDto;
import top.flyingjack.auth.admin.entity.dto.UpdateUserStatusDto;
import top.flyingjack.auth.admin.service.AdminUserService;
import top.flyingjack.common.dto.ApiRes;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理员用户管理", description = "用户查询、状态管理、角色管理")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户列表")
    public ResponseEntity<ApiRes<Page<AdminUserDto>>> listUsers(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "按用户名模糊搜索") @RequestParam(required = false) String search
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(ApiRes.success(adminUserService.findAllUsers(pageable, search)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public ResponseEntity<ApiRes<AdminUserDto>> getUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiRes.success(adminUserService.findUserById(id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户账号状态（启用/禁用/锁定）")
    public ResponseEntity<ApiRes<Void>> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UpdateUserStatusDto dto) {
        adminUserService.updateUserStatus(id, dto);
        return ResponseEntity.ok(ApiRes.success());
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "更新用户角色")
    public ResponseEntity<ApiRes<Void>> updateRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UpdateUserRolesDto dto) {
        adminUserService.updateUserRoles(id, dto);
        return ResponseEntity.ok(ApiRes.success());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<ApiRes<Void>> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success());
    }
}
