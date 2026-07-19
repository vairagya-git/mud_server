package com.rama.mudstock.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.rama.mudstock.config.ApplicationProperties;

@Component
public class MassiveRestApiCallLimiter {

    private final int callsPerMinute;
    private final AtomicInteger callsThisWindow = new AtomicInteger(0);
    private volatile long windowStartMillis = System.currentTimeMillis();

    public MassiveRestApiCallLimiter(ApplicationProperties applicationProperties) {
        this.callsPerMinute = applicationProperties.getEarnings().getApi().getCallsPerMinute();
    }

    private synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        if (now - windowStartMillis >= 60_000) {
            windowStartMillis = now;
            callsThisWindow.set(0);
        }
        if (callsThisWindow.get() < callsPerMinute) {
            callsThisWindow.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Blocks until a call permit is available or the thread is interrupted.
     */
    public void acquireOrWait() throws InterruptedException {
        while (!tryAcquire()) {
            long now = System.currentTimeMillis();
            long elapsed = now - windowStartMillis;
            long waitMs = 60_000 - elapsed;
            if (waitMs <= 0) continue;
            Thread.sleep(waitMs);
        }
    }
}
