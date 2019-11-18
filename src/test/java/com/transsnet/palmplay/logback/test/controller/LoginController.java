package com.transsnet.palmplay.logback.test.controller;

import com.alibaba.fastjson.JSON;
import com.transsnet.palmplay.logback.test.model.InfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/login", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String login(String info) {
        log.info("收到一个/login");
        log.error("line1>" + info);
        log.info("异常被处理了");
        log.error("line2>" + info, new Exception(info));

        String[] ref = new String[]{"1", "2"};
        try {
            System.out.println(ref[3]);
        } catch (Exception e) {
            log.error("line3>" + info, e);
        }

        InfoResponse infoResponse = new InfoResponse();
        infoResponse.setCode("0");

        return JSON.toJSONString(infoResponse);
    }

}
