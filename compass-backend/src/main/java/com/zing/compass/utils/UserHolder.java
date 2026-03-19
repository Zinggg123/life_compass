package com.zing.compass.utils;

import com.zing.compass.dto.UserDTO;
import com.zing.compass.dto.MerchantDTO;

public class UserHolder {
    // ThreadLocal will hold UserDTO object for current request
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    // ThreadLocal will hold MerchantDTO object for current request
    private static final ThreadLocal<MerchantDTO> mtl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }

    public static void saveMerchant(MerchantDTO merchant){
        mtl.set(merchant);
    }

    public static MerchantDTO getMerchant(){
        return mtl.get();
    }

    public static void removeMerchant(){
        mtl.remove();
    }
}
