package com.transsnet.palmplay.logback.test.model;

import com.alibaba.fastjson.JSON;

public class InfoResponse {

    private String code;

    public String getCode() {
        return code;
    }

    public InfoResponse setCode(String code) {
        this.code = code;
        return this;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
