package com.sre.userservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class UserServiceMetrics {

    private final Counter totalRequests;
    private final Counter successCount;
    private final Counter failureCount;
    private final Timer requestLatency;

    public UserServiceMetrics(MeterRegistry registry) {
        this.totalRequests  = Counter.builder("total_requests").register(registry);
        this.successCount   = Counter.builder("success_count").register(registry);
        this.failureCount   = Counter.builder("failure_count").register(registry);
        this.requestLatency = Timer.builder("request_latency_ms").register(registry);
    }

    public void incrementTotal()   { totalRequests.increment(); }
    public void incrementSuccess() { successCount.increment(); }
    public void incrementFailure() { failureCount.increment(); }

    public void recordLatency(long startTimeMs) {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        requestLatency.record(elapsed, TimeUnit.MILLISECONDS);
    }
}