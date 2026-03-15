package com.zing.compass.mapper;

import com.zing.compass.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    int insertComment(Comment comment);

    List<Comment> selectCommentsByBizId(String bizId);

    List<Comment> selectCommentsByUserId(String userId);
}
