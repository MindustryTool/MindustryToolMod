package mindustrytool.services;

import java.time.Duration;
import arc.util.Nullable;

@FunctionalInterface
public interface RetryStrategy {

    @Nullable
    Duration nextRetryDelay(int attempt, String method, Throwable failure);

    static RetryStrategy none() {
        return (attempt, method, failure) -> null;
    }

    static RetryStrategy defaultStrategy() {
        return new DefaultRetryStrategy(3, Duration.ofMillis(200), 2.0, false);
    }
}
