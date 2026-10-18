package solim.reactive;

final class TimeUtil {

    private TimeUtil() {
    }

    static long nanosToMillis(long nanos) {
        return nanos <= 0L ? 0L : nanos / 1_000_000L;
    }

    static float millisToSeconds(long millis) {
        return millis <= 0L ? 0.001f : millis / 1000f;
    }
}