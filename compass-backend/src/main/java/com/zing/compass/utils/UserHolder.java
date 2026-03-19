package com.zing.compass.utils;

import com.zing.compass.dto.UserDTO;

public class UserHolder {
    // ThreadLocal will hold UserDTO object for current request
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}

