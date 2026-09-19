package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.display.Badge;
import solim.display.Text;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Row;
import solim.runtime.ParentStack;
import solim.runtime.StructuralReconciler;
import solim.test.SolimTestHarness;
import solim.test.TestScheduler;

public class IntegrationAndLeakRegressionTest extends SolimTestHarness {

	// ==========================================
	// Phase 19: Async & Lifecycle Behavior
	// ==========================================

	@Test
	void asyncCompletionAfterDisposalSafelyIgnored() {
		Signal<String> data = Signal.of("initial");
		AtomicBoolean uiMutated = new AtomicBoolean(false);

		BaseComponent comp = new BaseComponent() {
			@Override
			protected Element build() {
				Effect.of(() -> {
					String val = data.get();
					if (!"initial".equals(val)) {
						uiMutated.set(true);
					}
				});
				return new Element();
			}
		};

		comp.element();
		assertFalse(uiMutated.get());

		// Simulate async fetch started
		CompletableFuture<String> asyncFetch = new CompletableFuture<>();

		// Component is disposed before async fetch completes
		comp.dispose();
		assertTrue(comp.isDisposed());

		// Async fetch finishes now
		asyncFetch.complete("async-result");
		data.set(asyncFetch.join());
		TestScheduler.flush();

		assertFalse(uiMutated.get(), "Disposed component's effect must not run when async task finishes");
	}

	// ==========================================
	// Phase 20: Multi-Subsystem Integration Tree
	// ==========================================

	static class ItemRow extends BaseComponent {
		final String id;
		final Signal<String> labelText;
		final Runnable onDelete;

		ItemRow(String id, String initialText, Runnable onDelete) {
			this.id = id;
			this.labelText = Signal.of(initialText);
			this.onDelete = onDelete;
		}

		@Override
		protected Element build() {
			Row row = new Row().gap(8f);
			Table table = row.table();
			ParentStack.push(table);
			Text.of(labelText);
			new Button(onDelete);
			ParentStack.pop();
			return table;
		}
	}

	@Test
	void fullComponentTreeLifecycleAndReactivity() {
		Signal<List<String>> itemKeys = Signal.of(new ArrayList<>(Arrays.asList("k1", "k2", "k3")));
		Signal<Integer> count = Signal.of(3);

		StructuralReconciler<String, ItemRow> reconciler = new StructuralReconciler<>();
		List<String> deleted = new ArrayList<>();

		// 1. Build app tree
		BaseComponent app = new BaseComponent() {
			@Override
			protected Element build() {
				Column col = new Column().gap(16f);
				Table appTable = col.table();
				ParentStack.push(appTable);

				// Header
				Row header = new Row().gap(8f);
				Table headerTable = header.table();
				ParentStack.push(headerTable);
				Text.of("App Header");
				Badge.ofCount(count);
				ParentStack.pop();

				// Content Card
				Card card = new Card().padding(12f);
				Table cardContainer = card.container();
				ParentStack.push(cardContainer);

				Table listTable = new Table();
				ParentStack.push(listTable);
				reconciler.reconcile(
						itemKeys.get(),
						k -> k,
						k -> new ItemRow(k, "Item " + k, () -> deleted.add(k))
				);
				ParentStack.pop();

				ParentStack.pop(); // Card
				ParentStack.pop(); // App

				return appTable;
			}
		};
		app.element();

		assertEquals(3, reconciler.activeComponents().size());
		ItemRow row1 = reconciler.activeComponents().get("k1");
		ItemRow row2 = reconciler.activeComponents().get("k2");
		ItemRow row3 = reconciler.activeComponents().get("k3");

		// 2. Reorder items: k3, k1, k2
		itemKeys.set(new ArrayList<>(Arrays.asList("k3", "k1", "k2")));
		reconciler.reconcile(
				itemKeys.get(),
				k -> k,
				k -> new ItemRow(k, "Item " + k, () -> deleted.add(k))
		);

		assertEquals(3, reconciler.activeComponents().size());
		assertSame(row3, reconciler.activeComponents().get("k3"));
		assertSame(row1, reconciler.activeComponents().get("k1"));
		assertSame(row2, reconciler.activeComponents().get("k2"));
		assertFalse(row1.isDisposed());

		// 3. Remove k2
		itemKeys.set(new ArrayList<>(Arrays.asList("k3", "k1")));
		count.set(2);
		TestScheduler.flush();
		reconciler.reconcile(
				itemKeys.get(),
				k -> k,
				k -> new ItemRow(k, "Item " + k, () -> deleted.add(k))
		);

		assertEquals(2, reconciler.activeComponents().size());
		assertTrue(row2.isDisposed(), "Removed row k2 must be disposed");
		assertFalse(row1.isDisposed());
		assertFalse(row3.isDisposed());

		// 4. Dispose entire tree
		reconciler.dispose();
		app.dispose();
		TestScheduler.flush();

		assertTrue(reconciler.isDisposed());
		assertTrue(row1.isDisposed());
		assertTrue(row3.isDisposed());
		assertEquals(0, count.observerCount());
	}

	// ==========================================
	// Phase 21: Leak & Stress Testing (100+ Cycles)
	// ==========================================

	@Test
	void oneHundredMountAndDisposeCyclesLeavesZeroResidualListeners() {
		Signal<Integer> sharedState = Signal.of(42);

		for (int i = 0; i < 100; i++) {
			final int index = i;
			BaseComponent comp = new BaseComponent() {
				@Override
				protected Element build() {
					Effect.of(() -> {
						sharedState.get();
					});
					Text text = Text.of(sharedState.map(v -> "State: " + v + " (cycle " + index + ")"));
					return text.element();
				}
			};

			comp.element();
			assertEquals(2, sharedState.observerCount(), "Each cycle registers an effect and a mapped computed observer");

			comp.dispose();
			TestScheduler.flush();

			assertEquals(0, sharedState.observerCount(), "Disposal must remove all observers registered in cycle " + i);
		}

		// Verify that sharedState continues to function cleanly with no residual leaks
		sharedState.set(999);
		TestScheduler.flush();
		assertEquals(0, sharedState.observerCount());
		assertEquals(0, sharedState.listenerCount());
	}
}
