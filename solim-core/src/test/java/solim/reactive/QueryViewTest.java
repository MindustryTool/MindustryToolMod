package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.display.Text;
import solim.input.Button;
import solim.runtime.SignalDispatcher;
import solim.test.SolimTestHarness;

import static org.junit.jupiter.api.Assertions.*;

class QueryViewTest extends SolimTestHarness {

	private QueryCache cache;

	@BeforeEach
	void setUp() {
		setUpHarness();
		cache = new QueryCache();
		QueryCache.setInstanceForTest(cache);
		SignalDispatcher.resetForTests();
	}

	private static Component label(String text) {
		return new Text(text);
	}

	private static String getText(QueryView<?> view) {
		Table t = view.container();
		if (t.getChildren().size == 0) return "";
		return extractText(t);
	}

	private static String extractText(Element el) {
		if (el instanceof Label) {
			return ((Label) el).getText().toString();
		}
		if (el instanceof Table) {
			Table t = (Table) el;
			StringBuilder sb = new StringBuilder();
			for (Element child : t.getChildren()) {
				String text = extractText(child);
				if (!text.isEmpty()) {
					if (sb.length() > 0) sb.append(" ");
					sb.append(text);
				}
			}
			return sb.toString();
		}
		return "";
	}

	// =========================================================================
	// 1.4 State Transitions Tests
	// =========================================================================

	@Test
	void initialStateAndLoading() {
		QueryKey key = QueryKey.of("qv-initial");
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(key, () -> future);

		QueryView<String> view = QueryView.of(query)
				.loading(() -> label("Custom Loading..."))
				.data(QueryViewTest::label);

		view.element(); // build
		assertEquals("Custom Loading...", getText(view));
	}

	@Test
	void defaultLoadingAndError() {
		QueryKey key = QueryKey.of("qv-default");
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(key, () -> future).retry(0);

		QueryView<String> view = QueryView.of(query).data(QueryViewTest::label);
		view.element();

		// Default loading
		assertTrue(getText(view).contains("Loading"));

		// Transition to default error with retry button
		future.completeExceptionally(new RuntimeException("Fail 123"));
		SignalDispatcher.flush();

		assertTrue(getText(view).contains("Fail 123"));
		assertTrue(getText(view).contains("Retry"), "Default error view must contain a retry button");
	}

	@Test
	void loadingToSuccessTransition() {
		QueryKey key = QueryKey.of("qv-load-to-success");
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(key, () -> future);

		QueryView<String> view = QueryView.of(query)
				.loading(() -> label("Loading..."))
				.data(QueryViewTest::label);

		view.element();
		assertEquals("Loading...", getText(view));

		future.complete("Loaded Data");
		SignalDispatcher.flush();

		assertEquals("Loaded Data", getText(view));
	}

	@Test
	void loadingToErrorTransition() {
		QueryKey key = QueryKey.of("qv-load-to-error");
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(key, () -> future).retry(0);

		QueryView<String> view = QueryView.of(query)
				.loading(() -> label("Loading..."))
				.error(err -> label("Error: " + err.getMessage()))
				.data(QueryViewTest::label);

		view.element();
		assertEquals("Loading...", getText(view));

		future.completeExceptionally(new RuntimeException("Boom"));
		SignalDispatcher.flush();

		assertEquals("Error: Boom", getText(view));
	}

	@Test
	void errorToRetryToSuccessTransition() {
		QueryKey key = QueryKey.of("qv-retry-success");
		CompletableFuture<String> f1 = new CompletableFuture<>();
		AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(f1);

		Query<String> query = Query.of(key, futureRef::get).retry(0);
		f1.completeExceptionally(new RuntimeException("First fail"));

		QueryView<String> view = QueryView.of(query)
				.loading(() -> label("Loading..."))
				.error(err -> {
					Table table = new Table();
					table.add(new Text("Failed").element());
					Button btn = new Button(query::refetch);
					btn.table().add(new Text("Retry").element());
					table.add(btn.element());
					return () -> table;
				})
				.data(QueryViewTest::label);

		view.element();
		SignalDispatcher.flush();
		assertTrue(getText(view).contains("Failed"));

		// Prepare success for retry
		futureRef.set(CompletableFuture.completedFuture("Retry Success"));
		cache.getOrCreateEntry(key).clear();
		query.refetch();
		SignalDispatcher.flush();

		assertEquals("Retry Success", getText(view));
	}

	// =========================================================================
	// 1.5 Stale-While-Revalidate (SWR) Tests
	// =========================================================================

	@Test
	void successToRefetchToFetchingRetainsData() {
		QueryKey key = QueryKey.of("qv-swr-fetching");
		AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(
				CompletableFuture.completedFuture("v1"));

		Query<String> query = Query.of(key, futureRef::get);

		QueryView<String> view = QueryView.of(query)
				.loading(() -> label("Loading..."))
				.data((data, isFetching) -> label(data + (isFetching ? " (refreshing)" : "")));

		view.element();
		SignalDispatcher.flush();
		assertEquals("v1", getText(view));

		// Background refetch with in-flight future
		CompletableFuture<String> f2 = new CompletableFuture<>();
		futureRef.set(f2);
		cache.getOrCreateEntry(key).clear();
		query.refetch();
		SignalDispatcher.flush();

		assertEquals("v1 (refreshing)", getText(view), "Data must remain visible during background refetch");

		f2.complete("v2");
		SignalDispatcher.flush();
		assertEquals("v2", getText(view));
	}

	@Test
	void successToRefetchToErrorRetainsStaleData() {
		QueryKey key = QueryKey.of("qv-swr-error");
		AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(
				CompletableFuture.completedFuture("initial-data"));

		Query<String> query = Query.of(key, futureRef::get).retry(0);

		QueryView<String> view = QueryView.of(query)
				.error(err -> label("Error View"))
				.data(QueryViewTest::label);

		view.element();
		SignalDispatcher.flush();
		assertEquals("initial-data", getText(view));

		// Refetch fails in background
		CompletableFuture<String> f2 = new CompletableFuture<>();
		futureRef.set(f2);
		cache.getOrCreateEntry(key).clear();
		query.refetch();
		f2.completeExceptionally(new RuntimeException("Background fail"));
		SignalDispatcher.flush();

		assertEquals("initial-data", getText(view), "Stale data must be retained when background refetch fails");
	}

	// =========================================================================
	// 1.6 Data Reconciliation Tests
	// =========================================================================

	@Test
	void dataChangesUpdatesView() {
		Signal<Integer> id = Signal.of(1);
		Query<String> query = Query.of(() -> CompletableFuture.completedFuture("User " + id.get()));

		QueryView<String> view = QueryView.of(query).data(QueryViewTest::label);
		view.element();
		SignalDispatcher.flush();
		assertEquals("User 1", getText(view));

		id.set(2);
		SignalDispatcher.flush();
		assertEquals("User 2", getText(view));
	}

	@Test
	void emptyDataHandling() {
		Query<List<String>> query = Query.of(() -> CompletableFuture.completedFuture(Collections.emptyList()));

		QueryView<List<String>> view = QueryView.of(query)
				.data(list -> label("Count: " + list.size()));

		view.element();
		SignalDispatcher.flush();
		assertEquals("Count: 0", getText(view));
	}

	@Test
	void keyedForEachReconciliationWithinQueryView() {
		List<String> itemsV1 = Arrays.asList("A", "B", "C");

		Signal<List<String>> itemsSignal = Signal.of(itemsV1);
		Query<List<String>> query = Query.of(() -> CompletableFuture.completedFuture(itemsSignal.get()));

		List<Element> mountedElements = new ArrayList<>();
		QueryView<List<String>> view = QueryView.of(query).data(items -> () -> {
			Table t = new Table();
			for (String item : items) {
				Text txt = new Text(item);
				mountedElements.add(txt.element());
				t.add(txt.element());
			}
			return t;
		});

		view.element();
		SignalDispatcher.flush();

		assertEquals(3, mountedElements.size());
	}

	@Test
	void noUnnecessaryRebuilding() {
		AtomicInteger buildCount = new AtomicInteger(0);
		Signal<String> dep = Signal.of("same");

		Query<String> query = Query.of(() -> {
			dep.get();
			return CompletableFuture.completedFuture("static-value");
		});

		QueryView<String> view = QueryView.of(query).data(data -> {
			buildCount.incrementAndGet();
			return label(data);
		});

		view.element();
		SignalDispatcher.flush();
		assertEquals(1, buildCount.get());

		// Mutating dep causes query refetch, but data returned is equal ("static-value")
		dep.set("same2");
		SignalDispatcher.flush();

		assertEquals(1, buildCount.get(), "QueryView should skip rebuilding when data is equal");
	}

	// =========================================================================
	// 1.7 Concurrency and Edge Cases Tests
	// =========================================================================

	@Test
	void staleLateRequestsDiscarded() {
		CompletableFuture<String> slow = new CompletableFuture<>();
		CompletableFuture<String> fast = CompletableFuture.completedFuture("fast");

		AtomicInteger count = new AtomicInteger(0);
		QueryKey key = QueryKey.of("qv-race");
		Query<String> query = Query.of(key, () -> count.incrementAndGet() == 1 ? slow : fast);

		QueryView<String> view = QueryView.of(query).data(QueryViewTest::label);
		view.element();

		// Trigger fast refetch
		cache.getOrCreateEntry(key).clear();
		query.refetch();
		SignalDispatcher.flush();

		assertEquals("fast", getText(view));

		// Slow completes late
		slow.complete("slow");
		SignalDispatcher.flush();

		assertEquals("fast", getText(view), "Late response must not overwrite fresh response");
	}

	@Test
	void callbackExceptionDoesNotCrash() {
		Query<String> query = Query.of(() -> CompletableFuture.completedFuture("boom"));

		QueryView<String> view = QueryView.of(query).data(data -> {
			throw new RuntimeException("UI callback crash");
		});

		assertDoesNotThrow(() -> {
			view.element();
			SignalDispatcher.flush();
		});

		// View falls back to default error view containing error message
		assertTrue(getText(view).contains("UI callback crash"));
	}

	@Test
	void duplicateKeysShareCache() {
		QueryKey key = QueryKey.of("shared-key");
		AtomicInteger fetchCount = new AtomicInteger(0);

		Query<String> q1 = Query.of(key, () -> {
			fetchCount.incrementAndGet();
			return CompletableFuture.completedFuture("shared-data");
		});
		Query<String> q2 = Query.of(key, () -> {
			fetchCount.incrementAndGet();
			return CompletableFuture.completedFuture("shared-data");
		});

		QueryView<String> v1 = QueryView.of(q1).data(QueryViewTest::label);
		QueryView<String> v2 = QueryView.of(q2).data(QueryViewTest::label);

		v1.element();
		v2.element();
		SignalDispatcher.flush();

		assertEquals(1, fetchCount.get(), "Shared key should deduplicate fetch");
		assertEquals("shared-data", getText(v1));
		assertEquals("shared-data", getText(v2));
	}

	@Test
	void querySharedByMultipleComponents() {
		Query<String> sharedQuery = Query.of(() -> CompletableFuture.completedFuture("initial"));

		QueryView<String> v1 = QueryView.of(sharedQuery).data(QueryViewTest::label);
		QueryView<String> v2 = QueryView.of(sharedQuery).data(QueryViewTest::label);

		v1.element();
		v2.element();
		SignalDispatcher.flush();

		assertEquals("initial", getText(v1));
		assertEquals("initial", getText(v2));
	}

	@Test
	void queryOutlivingComponent() {
		QueryKey key = QueryKey.of("outlive");
		Query<String> query = Query.of(key, () -> CompletableFuture.completedFuture("alive"));

		QueryView<String> view = QueryView.of(query).data(QueryViewTest::label);
		view.element();
		SignalDispatcher.flush();
		assertEquals("alive", getText(view));

		// Dispose view (component unmounts)
		view.dispose();
		assertTrue(view.isDisposed());

		// Query is still usable and fresh in cache
		assertFalse(query.isDisposed());
		assertEquals("alive", query.get());
	}

	@Test
	void disposalCleansUpChildrenAndStopsUpdates() {
		Signal<String> dataSignal = Signal.of("v1");
		Query<String> query = Query.of(() -> CompletableFuture.completedFuture(dataSignal.get()));

		AtomicBoolean childDisposed = new AtomicBoolean(false);
		QueryView<String> view = QueryView.of(query).data(data -> new Component() {
			@Override
			public Element element() {
				return new Text(data).element();
			}

			@Override
			public void dispose() {
				childDisposed.set(true);
			}

			@Override
			public boolean isDisposed() {
				return childDisposed.get();
			}
		});

		view.element();
		SignalDispatcher.flush();

		view.dispose();
		assertTrue(childDisposed.get(), "Unmounting QueryView must dispose active child component");

		// Further query updates must not resurrect view
		dataSignal.set("v2");
		SignalDispatcher.flush();
		assertEquals(0, view.container().getChildren().size);
	}

	@Test
	void ensureFreshCalledOnMount() {
		QueryKey key = QueryKey.of("qv-mount-fresh");
		AtomicInteger fetchCount = new AtomicInteger(0);

		Query<String> query = Query.of(key, () -> {
			int count = fetchCount.incrementAndGet();
			return CompletableFuture.completedFuture("data v" + count);
		});

		assertEquals(1, fetchCount.get());

		// Simulate query becoming stale
		cache.getOrCreateEntry(key).clear();
		assertTrue(query.isStale());

		// When QueryView mounts (calls element()), it should invoke ensureFresh() and refetch!
		QueryView<String> view = QueryView.of(query).data(QueryViewTest::label);
		view.element();

		assertEquals(2, fetchCount.get());
		assertEquals("data v2", getText(view));
	}

	@Test
	void queryWithInitialDataRendersViaFluentDataMethod() {
		QueryKey key = QueryKey.of("qv-initial-data");
		Query<String> query = Query.of(key, () -> CompletableFuture.completedFuture("initial-data"));

		// Simulate UI.query(...) where element() is called BEFORE .data(...)
		QueryView<String> view = QueryView.of(query);
		view.element(); // build() runs while dataFactory is still null

		// Now fluent .data() is called
		view.data(QueryViewTest::label);
		SignalDispatcher.flush();

		assertEquals("initial-data", getText(view));
	}

	@Test
	void queryWithAsyncDelayRendersDataAfterWaiting() throws Exception {
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(() -> future);

		QueryView<String> view = QueryView.of(query);
		view.element();
		view.loading(() -> label("Async Loading..."));
		view.data(QueryViewTest::label);

		assertEquals("Async Loading...", getText(view));

		// Complete future on background thread and wait for completion
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {}
			future.complete("delayed-data");
		});
		t.start();
		t.join();

		SignalDispatcher.flush();
		assertEquals("delayed-data", getText(view));
	}

	@Test
	void queryWithCustomLoadingViaFluentMethod() {
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(() -> future);

		QueryView<String> view = QueryView.of(query);
		view.element(); // build() runs with default loading
		view.loading(() -> label("Custom Spinner"));
		view.data(QueryViewTest::label);

		assertEquals("Custom Spinner", getText(view));
	}

	@Test
	void queryWithCustomErrorViaFluentMethod() {
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(() -> future).retry(0);
		future.completeExceptionally(new RuntimeException("Crash"));

		QueryView<String> view = QueryView.of(query);
		view.element(); // build() runs with default error
		view.error(err -> label("Custom Error: " + err.getMessage()));
		view.data(QueryViewTest::label);

		assertEquals("Custom Error: Crash", getText(view));
	}

	@Test
	void queryEnabledToggledRendersData() {
		Signal<Boolean> enabled = Signal.of(false);
		Query<String> query = Query.of(enabled, () -> CompletableFuture.completedFuture("enabled-data"));

		QueryView<String> view = QueryView.of(query);
		view.element();
		view.data(QueryViewTest::label);

		// While disabled, should not display data
		assertNotEquals("enabled-data", getText(view));

		// Enable and flush
		enabled.set(true);
		SignalDispatcher.flush();

		assertEquals("enabled-data", getText(view));
	}

	@Test
	void fetchingChangesDoNotRebuildWhenUsingSimpleDataFactory() {
		// Regression: fetching.set(true/false) was triggering full teardown+rebuild
		// of the data component even when the data hadn't changed and the caller
		// never used isFetching (simple Function<T,Component> variant).
		CompletableFuture<String> future = new CompletableFuture<>();
		Query<String> query = Query.of(() -> future);

		QueryView<String> view = QueryView.of(query);
		view.element();

		AtomicInteger buildCount = new AtomicInteger(0);
		view.data(data -> {
			buildCount.incrementAndGet();
			return label(data);
		});
		SignalDispatcher.flush();

		// Deliver initial data
		future.complete("page1");
		SignalDispatcher.flush();

		assertEquals("page1", getText(view));
		int buildsAfterInitialLoad = buildCount.get();
		assertEquals(1, buildsAfterInitialLoad, "should build exactly once for initial data");

		// Simulate a refetch (fetching=true while data stays the same)
		query.refetch();
		SignalDispatcher.flush();

		// fetching flipped true — but data hasn't changed and factory doesn't use isFetching
		// Must NOT trigger a rebuild
		assertEquals(buildsAfterInitialLoad, buildCount.get(),
				"fetching=true must not rebuild when using simple dataFactory");
		assertEquals("page1", getText(view));
	}
}
