package com.edu.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;
import com.edu.common.core.util.BeanCopyUtil;
import com.edu.common.security.model.LoginUser;
import com.edu.common.security.util.JwtUtil;
import com.edu.user.dto.*;
import com.edu.user.entity.User;
import com.edu.user.mapper.UserMapper;
import com.edu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis中存储黑名单Token的前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        // 检查用户名是否已存在
        long usernameCount = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查邮箱是否已存在
        long emailCount = count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (emailCount > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("student"); // 默认角色
        user.setStatus(1); // 默认正常状态
        user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + request.getUsername());

        save(user);
        log.info("新用户注册成功: username={}, id={}", user.getUsername(), user.getId());

        return BeanCopyUtil.copyBean(user, UserVO.class);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 根据用户名或邮箱查询用户
        User user = userMapper.findByUsernameOrEmail(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成JWT Token
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(user.getEmail())
                .build();

        String token = jwtUtil.generateToken(loginUser);
        log.info("用户登录成功: username={}", user.getUsername());

        UserVO userVO = BeanCopyUtil.copyBean(user, UserVO.class);
        return new LoginResponse(token, jwtExpiration, userVO);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        // 将token加入Redis黑名单，剩余有效时间内不可使用
        Date expiration = jwtUtil.getExpiration(token);
        if (expiration != null) {
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + token,
                        "1",
                        ttl,
                        TimeUnit.MILLISECONDS
                );
            }
        }
        log.info("用户登出，Token已加入黑名单");
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return BeanCopyUtil.copyBean(user, UserVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getBio())) {
            user.setBio(request.getBio());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }

        updateById(user);
        return BeanCopyUtil.copyBean(user, UserVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    @Override
    public Page<UserVO> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getRealName, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> users = page(pageParam, wrapper);
        Page<UserVO> result = new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        result.setRecords(users.getRecords().stream().map(user -> BeanCopyUtil.copyBean(user, UserVO.class)).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        updateById(user);
    }
}
