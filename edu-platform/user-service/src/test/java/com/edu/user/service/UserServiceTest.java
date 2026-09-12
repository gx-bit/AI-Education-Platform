package com.edu.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.security.util.JwtUtil;
import com.edu.user.dto.LoginRequest;
import com.edu.user.dto.LoginResponse;
import com.edu.user.dto.RegisterRequest;
import com.edu.user.dto.UserVO;
import com.edu.user.entity.User;
import com.edu.user.mapper.UserMapper;
import com.edu.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("$2a$10$encodedPassword");
        mockUser.setEmail("test@edu.com");
        mockUser.setRole("student");
        mockUser.setStatus(1);
    }

    @Test
    @DisplayName("登录成功 - 用户名密码正确")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userMapper.findByUsernameOrEmail("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("password123", mockUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("mock.jwt.token");

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userMapper, times(1)).findByUsernameOrEmail("testuser");
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(userMapper.findByUsernameOrEmail("nonexistent")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userMapper.findByUsernameOrEmail("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("wrongpassword", mockUser.getPassword())).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("登录失败 - 账号被禁用")
    void testLogin_UserDisabled() {
        mockUser.setStatus(0); // 禁用

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userMapper.findByUsernameOrEmail("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("password123", mockUser.getPassword())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("获取用户信息 - 用户存在")
    void testGetUserById_Success() {
        when(userMapper.selectById(1L)).thenReturn(mockUser);

        UserVO userVO = userService.getUserById(1L);

        assertNotNull(userVO);
        assertEquals("testuser", userVO.getUsername());
        assertEquals("test@edu.com", userVO.getEmail());
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在")
    void testGetUserById_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.getUserById(999L));
    }
}
