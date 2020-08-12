package com.lifeofcoder.autolimiter.dashboard;

/**
 * 响应结果
 *
 * @author xbc
 * @date 2020/4/20
 */
public class ResponseDto {
    private boolean success;
    private String errorMsg;
    private Object data;

    public static final ResponseDto SUCCESS = new ResponseDto(true, "", null);

    public ResponseDto(boolean success, String errorMsg, Object data) {
        this.success = success;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public <T> T getData() {
        return (T) data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static class Builder {
        public static ResponseDto succeeded(Object data) {
            return new ResponseDto(true, null, data);
        }

        public static ResponseDto succeeded() {
            return succeeded(null);
        }

        public static ResponseDto failed(String errorMsg) {
            return new ResponseDto(false, errorMsg, null);
        }
    }

    public static boolean hasData(ResponseDto responseDto) {
        return isSucceeded(responseDto) && responseDto.getData() != null;
    }

    public static boolean isSucceeded(ResponseDto responseDto) {
        return null != responseDto && responseDto.success;
    }

    public static String getErrorMsg(ResponseDto responseDto) {
        return responseDto == null ? "Null." : responseDto.getErrorMsg();
    }
}
