package com.transsnet.palmplay.logback.checkpoint;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 发送日志到kafka
 */
public class KafkaProducerService {

    private static Producer<String, String> producer;

    /**
     * 关闭kafka生产者,超时时长1秒,没发送完的日志将丢弃
     */
    public static void close() {
        producer.close(1, TimeUnit.SECONDS);
    }

    /**
     * 初始化kafka配置
     */
    public static void init(String kafkaServers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaServers);

        //当retries > 0 时，如果发送失败，会自动尝试重新发送数据。发送次数为retries设置的值。
        props.put("retries", 1);
        // batch.size是producer批量发送的基本单位，默认是16384Bytes，即16kB；
        props.put("batch.size", 16384);
        // lingger.ms是sender线程在检查batch是否ready时候，判断有没有过期的参数，默认大小是0ms。
        props.put("linger.ms", 100);

        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);
    }

    /**
     * 发送消息
     * send方法是异步的。当它被调用时，它会将消息记录添加到待发送缓冲区并立即返回。
     * 使用这种方式可以使生产者聚集一批消息记录后一起发送，从而提高效率。
     */
    static void produceMsg(String topic, String msg) {
        try {
            producer.send(new ProducerRecord<>(topic, msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
