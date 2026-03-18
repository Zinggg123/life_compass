package com.zing.compass.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBehaviorMapper {
    List<String> selectRecentCommentBehavior(String userId, int limit);
}
