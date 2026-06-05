package com.cijian.common.exception;

import com.cijian.common.enums.ResultCode;

public class BizException extends RuntimeException {

    private final int code;
    private final String message;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
        this.message = message;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
