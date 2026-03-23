package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.Comment;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.mapper.CommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void addComment_Success() {
        String userId = "u1";
        String bizId = "b1";
        Integer score = 5;
        String content = "Good!";

        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);

        boolean result = commentService.addComment(userId, bizId, score, content);
        assertTrue(result);
        verify(commentMapper).insertComment(any(Comment.class));
    }

    @Test
    void addComment_Fail() {
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(0);
        assertFalse(commentService.addComment("u1", "b1", 5, "ok"));
    }

    @Test
    void getUserRecentCommentBiz_CacheHit() {
        String userId = "u1";
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");
        
        when(listOperations.range(eq("user:comment:" + userId), eq(0L), eq(4L)))
                .thenReturn(Collections.singletonList(JSON.toJSONString(ub)));

        List<UserBehavior> result = commentService.getUserRecentCommentBiz(userId, 5);
        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getBizId());
        
        verify(commentMapper, never()).selectRecentCommentBiz(anyString(), anyInt());
    }

    @Test
    void getUserRecentCommentBiz_CacheMiss() {
        String userId = "u1";
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");

        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);
        when(commentMapper.selectRecentCommentBiz(userId, 5)).thenReturn(Collections.singletonList(ub));

        List<UserBehavior> result = commentService.getUserRecentCommentBiz(userId, 5);
        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getBizId());

        verify(listOperations).rightPushAll(anyString(), anyList()); // Using anyList because toList returns List.
        // Actually rightPushAll signature varies for StringRedisTemplate. It's usually (key, Collection<String>) or (key, String...)
        // Let's verify carefully. The code uses `rightPushAll(key, ...toList())`. 
        // toList() returns List<String>. Method accepts (key, Collection<String> values).
        // Mockito verify(listOperations).rightPushAll(string, collection)
    }
}


