package solim.test;

import java.util.concurrent.atomic.AtomicInteger;
import solim.core.Disposable;

/**
 * Standard test double for Disposable implementations to verify disposal state,
 * disposal invocation count, order sequence, and error simulation.
 */
public class TestDisposable implements Disposable {
	private static final AtomicInteger globalSequence = new AtomicInteger(0);

	private final String id;
	private final boolean throwOnDispose;
	private boolean disposed = false;
	private int disposeCount = 0;
	private int disposalOrder = -1;
	private long disposedTimestamp = -1L;

	public TestDisposable() {
		this("test-disposable", false);
	}

	public TestDisposable(String id) {
		this(id, false);
	}

	public TestDisposable(String id, boolean throwOnDispose) {
		this.id = id;
		this.throwOnDispose = throwOnDispose;
	}

	public static TestDisposable failing(String id) {
		return new TestDisposable(id, true);
	}

	@Override
	public void dispose() {
		disposeCount++;
		disposed = true;
		if (disposalOrder == -1) {
			disposalOrder = globalSequence.incrementAndGet();
			disposedTimestamp = System.currentTimeMillis();
		}
		if (throwOnDispose) {
			throw new RuntimeException("Simulated disposal failure for " + id);
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	public String getId() {
		return id;
	}

	public int getDisposeCount() {
		return disposeCount;
	}

	public int getDisposalOrder() {
		return disposalOrder;
	}

	public long getDisposedTimestamp() {
		return disposedTimestamp;
	}

	public static void resetSequence() {
		globalSequence.set(0);
	}
}
