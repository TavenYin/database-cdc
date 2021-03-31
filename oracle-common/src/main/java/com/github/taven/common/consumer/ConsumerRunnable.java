package com.github.taven.common.consumer;

import com.github.taven.common.oracle.DatabaseRecord;

import java.util.Queue;

public class ConsumerRunnable implements Runnable {
    private Queue<DatabaseRecord> queue;

    public ConsumerRunnable(Queue<DatabaseRecord> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {

    }
}
