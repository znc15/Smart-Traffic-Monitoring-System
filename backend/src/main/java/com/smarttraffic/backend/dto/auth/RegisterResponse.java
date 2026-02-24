package com.smarttraffic.backend.dto.auth;

public class RegisterResponse {
    private String msg;

    public RegisterResponse() {
    }

    public RegisterResponse(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
