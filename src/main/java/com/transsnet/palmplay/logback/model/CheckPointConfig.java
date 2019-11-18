package com.transsnet.palmplay.logback.model;

public class CheckPointConfig {
    private boolean active;
    private String kafkaBootstrapServers;
    private String kafkaTopic;
    private String logPath;
    private String serviceName;

    public CheckPointConfig(boolean active, String kafkaBootstrapServers, String kafkaTopic,
                            String logPath, String serviceName) {
        this.active = active;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaTopic = kafkaTopic;
        this.logPath = logPath;
        this.serviceName = serviceName;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public String getLogPath() {
        return logPath;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isActive() {
        return active;
    }
}
