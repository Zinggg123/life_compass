package com.zing.compass.mapper;

import com.zing.compass.entity.Comment;
import com.zing.compass.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    int insertComment(Comment comment);

    List<Comment> selectCommentsByBizId(String bizId);

    List<Comment> selectCommentsByUserId(String userId);

    List<UserBehavior> selectRecentCommentBiz(String userId, int limit);

    List<Comment> selectCommentsByBizIdPage(@Param("bizId") String bizId,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    Long countCommentsByBizId(@Param("bizId") String bizId);
}
