package com.wl.essql.common;

import lombok.Data;

@Data
public class CommonResult<T> {

    private int code;

    private T data;

    private String message;

    public CommonResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    private static int SUCCESS = 200;
    private static int ERROR = 1000;

    public static CommonResult success(Object data) {
        return new CommonResult(SUCCESS, data, "success");
    }

    public static CommonResult fail(Object data) {
        return new CommonResult(ERROR, data, "error");
    }
}
