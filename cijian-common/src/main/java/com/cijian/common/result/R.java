package com.cijian.common.result;

import com.cijian.common.enums.ResultCode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> success(String message) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> success(T data) {
        return success("success", data);
    }

    public static <T> R<T> success(String message, T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> R<T> error(String message) {
        R<T> r = new R<>();
        r.setCode(ResultCode.INTERNAL_ERROR.getCode());
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
