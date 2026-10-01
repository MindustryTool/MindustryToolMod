package mindustrytool.services;

import java.time.Duration;
import arc.util.Nullable;

@FunctionalInterface
public interface RetryStrategy {

    /**
     * Determines whether to retry and calculates the delay before the next retry attempt.
     *
     * @param attempt 1-based retry attempt number (1 for the first retry, 2 for the second, etc.)
     * @param method HTTP method (e.g. "GET", "POST", "PUT", "DELETE")
     * @param failure the error thrown during execution (typically {@link HttpException})
     * @return Duration delay to wait before retrying, or null if no further retry should occur
     */
    @Nullable Duration nextRetryDelay(int attempt, String method, Throwable failure);

    static RetryStrategy none() {
        return (attempt, method, failure) -> null;
    }

    static RetryStrategy defaultStrategy() {
        return new DefaultRetryStrategy(3, Duration.ofMillis(200), 2.0, false);
    }
}
