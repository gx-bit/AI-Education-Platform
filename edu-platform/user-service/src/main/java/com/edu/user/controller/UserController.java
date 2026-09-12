package com.edu.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.Result;
import com.edu.common.security.context.UserContext;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;
import com.edu.user.dto.*;
import com.edu.user.entity.User;
import com.edu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户注册、登录、信息管理相关接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO userVO = userService.register(request);
        return Result.success("注册成功", userVO);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    @Operation(summary = "用户登出", security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息", security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    @Operation(summary = "更新个人信息", security = @SecurityRequirement(name = "Bearer"))
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContext.getCurrentUserId();
        UserVO userVO = userService.updateProfile(userId, request);
        return Result.success("更新成功", userVO);
    }

    @Operation(summary = "修改当前用户密码", security = @SecurityRequirement(name = "Bearer"))
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.changePassword(userId, request);
        return Result.success();
    }

    @Operation(summary = "根据ID获取用户信息（服务间调用）")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    // ==================== 管理员接口 ====================

    @Operation(summary = "分页查询用户列表（管理员）", security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/admin/list")
    public Result<PageResult<UserVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(name = "keyword", required = false) String keyword) {
        requireAdmin();
        Page<UserVO> pageResult = userService.listUsers(page, size, keyword);
        PageResult<UserVO> result = PageResult.of(
                pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal(), pageResult.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "修改用户状态（管理员）", security = @SecurityRequirement(name = "Bearer"))
    @PutMapping("/admin/{userId}/status")
    public Result<Void> updateStatus(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "status") Integer status) {
        requireAdmin();
        userService.updateStatus(userId, status);
        return Result.success();
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
