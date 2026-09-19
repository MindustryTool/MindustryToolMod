package solim.test;

import arc.scene.Element;
import java.util.ArrayList;
import java.util.List;
import solim.core.BaseComponent;
import solim.core.Disposable;
import solim.runtime.ComponentContext;

/**
 * Standard test double for BaseComponent verifying build count, single materialization,
 * owned resource registration, and failure injection.
 */
public class TestComponent extends BaseComponent {
	private final String id;
	private final boolean throwOnBuild;
	private final boolean throwOnDispose;
	private final List<Disposable> initialDisposables = new ArrayList<>();

	private int buildCount = 0;
	private int disposeCount = 0;
	private Element createdElement;

	public TestComponent() {
		this("test-component", false, false);
	}

	public TestComponent(String id) {
		this(id, false, false);
	}

	public TestComponent(String id, boolean throwOnBuild, boolean throwOnDispose) {
		this.id = id;
		this.throwOnBuild = throwOnBuild;
		this.throwOnDispose = throwOnDispose;
	}

	public static TestComponent failingBuild(String id) {
		return new TestComponent(id, true, false);
	}

	public static TestComponent failingDispose(String id) {
		return new TestComponent(id, false, true);
	}

	public TestComponent withDisposable(Disposable disposable) {
		initialDisposables.add(disposable);
		return this;
	}

	@Override
	protected Element build() {
		buildCount++;
		for (Disposable d : initialDisposables) {
			ComponentContext.register(d);
		}
		if (throwOnBuild) {
			throw new RuntimeException("Simulated build failure for " + id);
		}
		createdElement = new Element();
		createdElement.name = id;
		return createdElement;
	}

	@Override
	public void dispose() {
		disposeCount++;
		super.dispose();
		if (throwOnDispose) {
			throw new RuntimeException("Simulated dispose failure for " + id);
		}
	}

	public String getId() {
		return id;
	}

	public int getBuildCount() {
		return buildCount;
	}

	public int getDisposeCount() {
		return disposeCount;
	}

	public Element getCreatedElement() {
		return createdElement;
	}
}
