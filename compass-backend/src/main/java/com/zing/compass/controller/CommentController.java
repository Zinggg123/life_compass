package com.zing.compass.controller;

import com.zing.compass.service.CommentService;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/add")
    public Result addComment(String userId, String bizId, Integer score, String content) {
        boolean success = commentService.addComment(userId, bizId, score, content);

        if (success) {
            return Result.success("Comment added successfully");
        } else {
            return Result.failure("Failed to add comment");
        }
    }
}
