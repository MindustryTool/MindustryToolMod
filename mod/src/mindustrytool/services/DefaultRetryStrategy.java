package mindustrytool.services;

import java.time.Duration;
import java.util.Locale;
import arc.util.Nullable;

public class DefaultRetryStrategy implements RetryStrategy {

    private final int maxRetries;
    private final Duration initialDelay;
    private final double backoffMultiplier;
    private final boolean retryPost;

    public DefaultRetryStrategy(int maxRetries, Duration initialDelay, double backoffMultiplier, boolean retryPost) {
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay != null ? initialDelay : Duration.ofMillis(200);
        this.backoffMultiplier = backoffMultiplier > 0 ? backoffMultiplier : 2.0;
        this.retryPost = retryPost;
    }

    public DefaultRetryStrategy withRetryPost(boolean retryPost) {
        return new DefaultRetryStrategy(maxRetries, initialDelay, backoffMultiplier, retryPost);
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Duration initialDelay() {
        return initialDelay;
    }

    public double backoffMultiplier() {
        return backoffMultiplier;
    }

    public boolean retryPost() {
        return retryPost;
    }

    @Override
    public @Nullable Duration nextRetryDelay(int attempt, String method, Throwable failure) {
        if (attempt <= 0 || attempt > maxRetries) {
            return null;
        }

        if (!isMethodEligible(method)) {
            return null;
        }

        if (!isServerError(failure)) {
            return null;
        }

        long baseMillis = initialDelay.toMillis();
        long delayMillis = Math.round(baseMillis * Math.pow(backoffMultiplier, attempt - 1));
        return Duration.ofMillis(Math.max(0, delayMillis));
    }

    protected boolean isMethodEligible(@Nullable String method) {
        if (method == null) {
            return false;
        }
        String upper = method.toUpperCase(Locale.ROOT);
        return retryPost || "GET".equals(upper) || "PUT".equals(upper) || "DELETE".equals(upper) || "HEAD".equals(upper);
    }

    protected boolean isServerError(Throwable failure) {
        if (failure instanceof HttpException) {
            HttpException httpEx = (HttpException) failure;
            return httpEx.statusCode() >= 500;
        }
        return false;
    }
}
