package com.transsnet.palmplay.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import com.transsnet.palmplay.logback.checkpoint.CheckPointService;
import com.transsnet.palmplay.logback.checkpoint.KafkaProducerService;
import com.transsnet.palmplay.logback.model.CheckPointConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.concurrent.*;

/**
 * 异步appender，用来将日志通过打点接口发送到ES上
 *
 * @author zhouzilong
 */
public class AsyncCheckPointAppenderBase<E> extends UnsynchronizedAppenderBase<E> implements AppenderAttachable<E> {

    public static final int DEFAULT_QUEUE_SIZE = 256;
    public static final int DEFAULT_WORKER_NUM = 5;
    static final int UNDEFINED = -1;

    boolean active = false;
    int appenderCount = 0;

    int discardingThreshold = UNDEFINED;
    int queueSize = DEFAULT_QUEUE_SIZE;
    int workersNum = DEFAULT_WORKER_NUM;

    String kafkaBootstrapServers = null;
    String kafkaTopic = null;
    Layout layout;
    String logPath = null;
    String serviceName = null;

    AppenderAttachableImpl<E> aai = new AppenderAttachableImpl<E>();
    BlockingQueue<E> blockingQueue;
    Worker worker = new Worker();

    @Override
    public void addAppender(Appender<E> newAppender) {
        if (appenderCount == 0) {
            appenderCount++;
            addInfo("Attaching appender named [" + newAppender.getName() + "] to AsyncAppender.");
            aai.addAppender(newAppender);
        } else {
            addWarn("One and only one appender may be attached to AsyncAppender.");
            addWarn("Ignoring additional appender named [" + newAppender.getName() + "]");
        }
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<E> eAppender) {
        return aai.detachAppender(eAppender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    @Override
    public Appender<E> getAppender(String name) {
        return aai.getAppender(name);
    }

    public int getDiscardingThreshold() {
        return discardingThreshold;
    }

    public void setDiscardingThreshold(int discardingThreshold) {
        this.discardingThreshold = discardingThreshold;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getWorkersNum() {
        return workersNum;
    }

    public void setWorkersNum(int workersNum) {
        this.workersNum = workersNum;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isAttached(Appender<E> eAppender) {
        return aai.isAttached(eAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    /**
     * 启动appender，started状态为true时打点appender已进入生命周期，
     * 可调用append()方法处理日志事件，不在生命周期里，将无法调用append方法处理
     */
    @Override
    public void start() {

        if (this.layout == null) {
            addError("异步打点服务appender，必须配置引入layout");
            return;
        }
        if (appenderCount == 0) {
            addError("异步打点服务appender，必须配置引入其他apender");
            return;
        }
        if (queueSize < 1) {
            addError("异步打点服务appender配置了不合理的队列大小 [" + queueSize + "]");
            return;
        }
        if (!validateCheckPointConfig()) {
            active = false;
            addError("异步打点服务appender打点配置有误，将不能使用上报日志到打点接口，但不影响其他appender处理日志。");
        }

        blockingQueue = new ArrayBlockingQueue<E>(queueSize);

        if (discardingThreshold == UNDEFINED)
            discardingThreshold = queueSize / 5;
        addInfo("异步打点服务appender配置阈值为:" + discardingThreshold);

        addInfo("初始化kafka集群,集群地址为:" + kafkaBootstrapServers);
        KafkaProducerService.init(kafkaBootstrapServers);

        worker.setDaemon(true);
        worker.setName("AsyncAppender-Worker-" + worker.getName());
        super.start();
        worker.start();
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;
        super.stop();

        KafkaProducerService.close();

        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            addError("异步打点服务appender无法join工作线程", e);
        }
    }

    @Override
    protected void append(E eventObject) {
        if (!isStarted()) {
            return;
        }

        aai.appendLoopOnAppenders(eventObject);

        if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
            return;
        }
        preprocess(eventObject);
        put(eventObject);
    }

    protected boolean isDiscardable(E eventObject) {
        return false;
    }

    protected void preprocess(E eventObject) {
    }

    private boolean isQueueBelowDiscardingThreshold() {
        return (blockingQueue.remainingCapacity() <= discardingThreshold);
    }

    private void put(E eventObject) {
        if (!blockingQueue.offer(eventObject)) {
            addWarn("异步打点服务appender无法插入日志事件到队列，队列可能已满");
        }
    }

    /**
     * 校验打点服务是否配置正常
     */
    private boolean validateCheckPointConfig() {
        if (this.active) {

            if (this.workersNum <= 0) {
                return false;
            }

            String[] options = {this.serviceName, this.kafkaBootstrapServers, this.serviceName, this.logPath};
            for (String tmp : options) {
                if (StringUtils.isBlank(tmp)) {
                    return false;
                }
            }
        }

        return true;
    }

    class Worker extends Thread {

        @Override
        public void run() {
            AsyncCheckPointAppenderBase<E> parent = AsyncCheckPointAppenderBase.this;
            AppenderAttachableImpl<E> aai = parent.aai;
            ExecutorService executorService = Executors.newFixedThreadPool(parent.workersNum);

            while (parent.isStarted()) {
                try {
                    E e = parent.blockingQueue.take();
                    submit(executorService, e);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            for (E e : parent.blockingQueue) {
                submit(executorService, e);
                parent.blockingQueue.remove(e);
            }

            shutDownGraceful(executorService);

            addInfo("异步打点服务appender正在关闭，还剩下：" + parent.blockingQueue.remainingCapacity());
            aai.detachAndStopAllAppenders();
        }

        /**
         * 发送至打点服务
         */
        private void sendToCheckPoint(E e) {
            addInfo(Thread.currentThread().getName() + ":worker正在上传日志至打点服务");
            CheckPointConfig config = new CheckPointConfig(active, kafkaBootstrapServers, kafkaTopic,
                    logPath, serviceName);
            CheckPointService.send(config, (ILoggingEvent) e, layout);
        }

        /**
         * 关闭线程池
         */
        private void shutDownGraceful(ExecutorService executorService) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                executorService.shutdownNow();
            }
        }

        /**
         * 批量发送日志到打点接口
         */
        private void submit(ExecutorService executorService, E e) {
            executorService.submit(() -> sendToCheckPoint(e));
        }
    }
}
