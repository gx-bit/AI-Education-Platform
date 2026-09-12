package com.edu.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.user.dto.*;
import com.edu.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     * @param request 注册请求
     * @return 新用户VO
     */
    UserVO register(RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（含Token）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出（将Token加入黑名单）
     * @param token JWT Token
     */
    void logout(String token);

    /**
     * 根据ID获取用户信息
     * @param userId 用户ID
     * @return 用户VO
     */
    UserVO getUserById(Long userId);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户VO
     */
    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改当前用户密码
     * @param userId 用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 分页查询用户列表（管理员）
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @return 分页用户列表
     */
    Page<UserVO> listUsers(int page, int size, String keyword);

    /**
     * 修改用户状态（管理员）
     * @param userId 用户ID
     * @param status 状态：0-禁用 1-正常
     */
    void updateStatus(Long userId, Integer status);
}
