package com.zing.compass.service;

import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.User;
import com.zing.compass.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void login_Success() {
        String userId = "u1";
        String password = "password";
        User user = new User();
        user.setUserId(userId);
        user.setPassword(sha256(password)); // Mock matching password
        user.setName("Test User");
        user.setYelpingSince(LocalDateTime.now());

        when(userMapper.selectUserById(userId)).thenReturn(user);

        Map<String, Object> result = userService.login(userId, password);

        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("user"));
        UserDTO dto = (UserDTO) result.get("user");
        assertEquals(userId, dto.getUserId());

        verify(hashOperations).putAll(anyString(), anyMap());
        verify(redisTemplate).expire(anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void login_Fail_NotFound() {
        when(userMapper.selectUserById("u1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> userService.login("u1", "pwd"));
    }

    @Test
    void login_Fail_WrongPassword() {
        User user = new User();
        user.setUserId("u1");
        user.setPassword(sha256("correct"));

        when(userMapper.selectUserById("u1")).thenReturn(user);
        
        Exception ex = assertThrows(RuntimeException.class, () -> userService.login("u1", "wrong"));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    void register_Success() {
        User user = new User();
        user.setUserId("u1");
        user.setPassword("password");

        when(userMapper.selectUserById("u1")).thenReturn(null);

        User result = userService.register(user);
        
        // Check password hashed
        assertNotEquals("password", result.getPassword());
        assertEquals(sha256("password"), result.getPassword());
        assertNotNull(result.getYelpingSince());
        
        verify(userMapper).insertUser(user);
    }
    
    @Test
    void register_Fail_Exists() {
        User user = new User();
        user.setUserId("u1");
        
        when(userMapper.selectUserById("u1")).thenReturn(new User());
        
        assertThrows(RuntimeException.class, () -> userService.register(user));
    }

    @Test
    void register_Fail_NoPassword() {
        User user = new User();
        user.setUserId("u1");
        // password is null

        when(userMapper.selectUserById("u1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> userService.register(user));
    }

    @Test
    void getUserInfo_Success() {
        User user = new User();
        user.setUserId("u1");
        when(userMapper.selectUserById("u1")).thenReturn(user);

        User result = userService.getUserInfo("u1");
        assertEquals("u1", result.getUserId());
    }

    @Test
    void getUserInfo_Fail() {
        when(userMapper.selectUserById("u1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> userService.getUserInfo("u1"));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}


