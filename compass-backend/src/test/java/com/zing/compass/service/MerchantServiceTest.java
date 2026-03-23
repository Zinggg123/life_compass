package com.zing.compass.service;

import com.zing.compass.dto.MerchantDTO;
import com.zing.compass.entity.Merchant;
import com.zing.compass.mapper.MerchantMapper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void login_Success() {
        String merId = "m1";
        String password = "password";
        Merchant merchant = new Merchant();
        merchant.setMerchantId(merId);
        merchant.setPassword(sha256(password)); // Mock matching password
        merchant.setName("Test Merchant");
        merchant.setBizId("b1");
        merchant.setYelpingSince(LocalDateTime.now());

        when(merchantMapper.selectMerchantById(merId)).thenReturn(merchant);

        Map<String, Object> result = merchantService.login(merId, password);

        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("merchant"));
        MerchantDTO dto = (MerchantDTO) result.get("merchant");
        assertEquals(merId, dto.getMerchantId());

        verify(hashOperations).putAll(anyString(), anyMap());
        verify(redisTemplate).expire(anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void login_Fail_NotFound() {
        when(merchantMapper.selectMerchantById("m1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> merchantService.login("m1", "pwd"));
    }

    @Test
    void login_Fail_WrongPassword() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId("m1");
        merchant.setPassword(sha256("correct"));

        when(merchantMapper.selectMerchantById("m1")).thenReturn(merchant);
        
        Exception ex = assertThrows(RuntimeException.class, () -> merchantService.login("m1", "wrong"));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    void register_Success() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId("m1");
        merchant.setPassword("password");

        when(merchantMapper.selectMerchantById("m1")).thenReturn(null);

        Merchant result = merchantService.register(merchant);
        
        // Check password hashed
        assertNotEquals("password", result.getPassword());
        assertEquals(sha256("password"), result.getPassword());
        assertNotNull(result.getYelpingSince());
        
        verify(merchantMapper).insertMerchant(merchant);
    }
    
    @Test
    void register_Fail_Exists() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId("m1");
        
        when(merchantMapper.selectMerchantById("m1")).thenReturn(new Merchant());
        
        assertThrows(RuntimeException.class, () -> merchantService.register(merchant));
    }

    @Test
    void register_Fail_NoPassword() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId("m1");
        // password is null

        when(merchantMapper.selectMerchantById("m1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> merchantService.register(merchant));
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


