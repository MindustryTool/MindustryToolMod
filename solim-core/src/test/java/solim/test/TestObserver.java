package solim.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import solim.core.ReactiveObserver;

/**
 * Standard test double for ReactiveObserver tracking observed dependencies and invalidations.
 */
public class TestObserver implements ReactiveObserver {
	private final String id;
	private final List<Object> dependencies = new ArrayList<>();
	private int invalidateCount = 0;

	public TestObserver() {
		this("test-observer");
	}

	public TestObserver(String id) {
		this.id = id;
	}

	@Override
	public void addDependency(Object observable) {
		dependencies.add(observable);
	}

	@Override
	public void invalidate() {
		invalidateCount++;
	}

	public String getId() {
		return id;
	}

	public List<Object> getDependencies() {
		return Collections.unmodifiableList(dependencies);
	}

	public int getInvalidateCount() {
		return invalidateCount;
	}

	public void clear() {
		dependencies.clear();
		invalidateCount = 0;
	}
}
