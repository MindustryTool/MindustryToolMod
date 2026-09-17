package solim.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.runtime.SignalDispatcher;

class TwoWayBindingTest {

	@Test
	void signalUpdatesWidget() {
		Signal<String> sig = Signal.of("initial");
		String[] widgetVal = new String[] {"initial"};
		AtomicInteger setterCalls = new AtomicInteger(0);

		TwoWayBinding<String> binding = new TwoWayBinding<>(
				sig,
				() -> widgetVal[0],
				val -> {
					widgetVal[0] = val;
					setterCalls.incrementAndGet();
				},
				onChange -> () -> {}
		);

		assertEquals(0, setterCalls.get(), "Initial equal value should not trigger setter");

		sig.set("updated");
		SignalDispatcher.flush();
		assertEquals("updated", widgetVal[0]);
		assertEquals(1, setterCalls.get());

		binding.dispose();
	}

	@Test
	void widgetUpdatesSignalWithoutFeedbackLoop() {
		Signal<Integer> sig = Signal.of(10);
		int[] widgetVal = new int[] {10};
		AtomicInteger setterCalls = new AtomicInteger(0);
		Runnable[] trigger = new Runnable[1];

		TwoWayBinding<Integer> binding = new TwoWayBinding<>(
				sig,
				() -> widgetVal[0],
				val -> {
					widgetVal[0] = val;
					setterCalls.incrementAndGet();
				},
				onChange -> {
					trigger[0] = onChange;
					return () -> {};
				}
		);

		// Simulate widget user interaction
		widgetVal[0] = 42;
		trigger[0].run();

		assertEquals(42, sig.get());
		// Widget setter should NOT have been called when widget drove the change
		assertEquals(0, setterCalls.get(), "Widget change should not loop back to widget setter");

		binding.dispose();
	}

	@Test
	void disposalCleansUp() {
		Signal<String> sig = Signal.of("A");
		String[] widgetVal = new String[] {"A"};
		AtomicBoolean listenerRemoved = new AtomicBoolean(false);

		TwoWayBinding<String> binding = new TwoWayBinding<>(
				sig,
				() -> widgetVal[0],
				val -> widgetVal[0] = val,
				onChange -> () -> listenerRemoved.set(true)
		);

		assertFalse(binding.isDisposed());
		binding.dispose();
		assertTrue(binding.isDisposed());
		assertTrue(listenerRemoved.get(), "Widget listener must be cleaned up on disposal");

		sig.set("B");
		SignalDispatcher.flush();
		assertEquals("A", widgetVal[0], "Disposed binding should not propagate signal changes");
	}

	@Test
	void typeMessageSendMessageClearMessageCycle() {
		Signal<String> messageSignal = Signal.of("");
		String[] widgetVal = {""};
		Runnable[] widgetChangeListener = new Runnable[1];

		TwoWayBinding<String> binding = new TwoWayBinding<>(
				messageSignal,
				() -> widgetVal[0],
				val -> widgetVal[0] = val,
				onChange -> {
					widgetChangeListener[0] = onChange;
					return () -> {};
				}
		);

		// Initial state
		assertEquals("", widgetVal[0]);
		assertEquals("", messageSignal.get());

		// 1. "type message": user inputs message into widget
		widgetVal[0] = "First message";
		widgetChangeListener[0].run();

		// Check the value signal too
		assertEquals("First message", widgetVal[0]);
		assertEquals("First message", messageSignal.get(), "Signal must track typed widget message");

		// 2. "send message": simulated send consumes message
		String sentMessage = messageSignal.get();
		assertEquals("First message", sentMessage);

		// 3. "clear message": signal is cleared on send
		messageSignal.set("");
		SignalDispatcher.flush();
		assertEquals("", widgetVal[0], "Widget must be cleared when signal is cleared");
		assertEquals("", messageSignal.get(), "Signal must be cleared");

		// 4. "type message again": user inputs second message
		widgetVal[0] = "Second message";
		widgetChangeListener[0].run();

		// 5. "check the value signal too": signal must update on second message
		assertEquals("Second message", widgetVal[0]);
		assertEquals("Second message", messageSignal.get(), "Signal must reflect second message");

		binding.dispose();
	}
}
