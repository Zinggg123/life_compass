package com.zing.compass.mapper;

import com.zing.compass.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    //查询用户信息
    User selectUserById(@Param("userId") String userId);

    //插入新用户
    void insertUser(User user);
}
