package com.memopoly.network.services;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AuctionTimerService {
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    public AuctionTimerService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public void start(Runnable tickAction) {
        cancel();
        task = executor.scheduleAtFixedRate(tickAction, 1, 1, TimeUnit.SECONDS);
    }

    public void cancel() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }
}
