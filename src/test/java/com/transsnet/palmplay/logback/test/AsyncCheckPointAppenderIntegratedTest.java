package com.transsnet.palmplay.logback.test;

import com.transsnet.palmplay.logback.test.model.InfoResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jtool.apiclient.ApiClient.Api;

@SpringBootApplication
public class AsyncCheckPointAppenderIntegratedTest {

    private final String host = "http://localhost:8082";

    @BeforeClass
    public static void setup() {
        String[] args = new String[0];
        SpringApplication.run(AsyncCheckPointAppenderIntegratedTest.class, args);
    }

    @Test
    public void test() throws InterruptedException, IOException {

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 1; i++) {
            executorService.execute(new worker(Integer.valueOf(i).toString()));
        }

        shutDownGraceful(executorService);
        Thread.sleep(1000 * 60 * 5);

    }

    /**
     * 关闭测试用的线程池
     */
    private void shutDownGraceful(ExecutorService executorService) {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            System.out.println(Thread.currentThread().getName() + ",已完成关闭线程池");
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            executorService.shutdownNow();
        }
    }

    class worker implements Runnable {

        private String id = null;

        public worker(String id) {
            this.id = id;
        }

        public void run() {
            InfoResponse infoResponse = null;
            try {
                infoResponse = Api().get(host + "/login?info=" + id + "_" + Thread.currentThread().getName(),
                        InfoResponse.class);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Assert.assertEquals("0", infoResponse.getCode());
            System.out.println(Thread.currentThread().getName() + ",成功发送和接送请求，第" + id);
        }
    }
}
