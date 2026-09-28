package com.memopoly.network.services;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Служба таймера аукциона: управляет временем ожидания ставок на сервере и автоматически закрывает торги по истечении времени.
 */
public class AuctionTimerService {
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    public AuctionTimerService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public void start(Runnable tickAction) {
        cancel();
        if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
            try {
                task = executor.scheduleAtFixedRate(tickAction, 1, 1, TimeUnit.SECONDS);
            } catch (RejectedExecutionException ignored) {
                task = null;
            }
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }
}
