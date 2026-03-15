package com.zing.compass.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 统一返回前端的结果
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private int code;
    private String message;
    private Object data;

    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success(String message, Object data) {
        return new Result(200, message, data);
    }

    public static Result error(String message) {
        return new Result(500, message, null);
    }

    public static Result failure(String message) {
        return new Result(400, message, null);
    }
}
