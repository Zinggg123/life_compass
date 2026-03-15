package com.zing.compass.service;

import com.zing.compass.entity.Comment;
import com.zing.compass.mapper.CommentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;

    public boolean addComment(String userId, String bizId, Integer score, String content) {
        String commentId = UUID.randomUUID().toString();
        Comment comment = new Comment(commentId, userId, bizId, score, content, LocalDateTime.now());

        return commentMapper.insertComment(comment) > 0;
    }
}
