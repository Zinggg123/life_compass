package com.zing.compass.service;

import com.zing.compass.entity.User;
import com.zing.compass.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    //登录
    public User login(String userId) {
        User user = userMapper.selectUserById(userId);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    //注册
    public User register(User user) {
        User existingUser = userMapper.selectUserById(user.getUserId());
        if(existingUser != null){
            throw new RuntimeException("用户已存在");
        }
        user.setYelpingSince(LocalDateTime.now());

        userMapper.insertUser(user);
        return user;
    }

    //获取用户信息
    public User getUserInfo(String userId) {
        User user = userMapper.selectUserById(userId);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
}
