package com.photon.photonchain.exception;


/**
 * 自定义业务层异常
 *
 * @author hwh
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = -6421035720932214340L;
    private ErrorCode errorCode;

    /**
     * 自定义错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode._20000;
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
