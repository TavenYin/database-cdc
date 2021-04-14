package com.github.taven.common.consumer;

import java.util.concurrent.*;

public class ConsumerThreadPool {

    ThreadPoolExecutor consumerThreadPool = new ThreadPoolExecutor(1, 1,
            60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

    public void syncConsume(Runnable runnable) {
        Future<?> future = consumerThreadPool.submit(runnable);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void asyncConsume(Runnable runnable) {
        consumerThreadPool.execute(runnable);
    }

}
