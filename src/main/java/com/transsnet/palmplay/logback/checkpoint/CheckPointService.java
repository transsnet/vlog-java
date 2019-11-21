package com.transsnet.palmplay.logback.checkpoint;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import com.alibaba.fastjson.JSON;
import com.transsnet.palmplay.logback.model.CheckPointConfig;
import com.transsnet.palmplay.logback.model.CheckPointMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CheckPointService {

    /**
     * 发送打点数据
     */
    public static void send(CheckPointConfig config, ILoggingEvent event, Layout layout) {
        if (!config.isActive()) {
            return;
        }
        try {
            CheckPointMessage message = initCheckPointMessage(config, event, layout);
            send(config.getKafkaTopic(), message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 发送数据到打点接口
     */
    private static void send(String topic, CheckPointMessage message) {
        KafkaProducerService.produceMsg(topic, JSON.toJSONString(message));
    }

    // 获得创建时间
    private static String getCreateTime(ILoggingEvent event) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(event.getTimeStamp()));
    }

    /**
     * 初始化打点数据体
     */
    private static CheckPointMessage initCheckPointMessage(CheckPointConfig config, ILoggingEvent event,
                                                           Layout layout) {

        CheckPointMessage message = new CheckPointMessage();
        message.setTs(getCreateTime(event));
        message.setStacktrace(layout.doLayout(event));
        message.setMsg(event.getMessage());
        message.setServiceName(config.getServiceName());
        message.setLevel(event.getLevel().levelStr.toLowerCase());
        message.setCaller(event.getLoggerName());
        message.setLogPath(config.getLogPath());
        return message;
    }
}
