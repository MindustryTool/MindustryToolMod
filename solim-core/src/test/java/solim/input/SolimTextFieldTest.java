package solim.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class SolimTextFieldTest extends SolimEnv {

	@BeforeEach
	void setUp() {
		newScene();
	}

	private void simulateKey(SolimTextField tf, KeyCode key) {
		InputEvent event = new InputEvent();
		event.type = InputEvent.InputEventType.keyDown;
		event.keyCode = key;
		tf.field().getListeners().each(l -> l.handle(event));
	}

	@Test
	void enterKeySubmission() {
		Signal<String> text = Signal.of("hello world");
		SolimTextField tf = new SolimTextField(text);

		String[] submitted = {null};
		tf.onEnter(submittedText -> submitted[0] = submittedText);

		// Non-enter key should do nothing
		simulateKey(tf, KeyCode.space);
		assertNull(submitted[0]);

		// Enter key should submit text
		simulateKey(tf, KeyCode.enter);
		assertEquals("hello world", submitted[0]);

		// Update text and submit again
		text.set("updated text");
		SignalDispatcher.flush();
		simulateKey(tf, KeyCode.enter);
		assertEquals("updated text", submitted[0]);

		tf.dispose();
	}

	@Test
	void enterKeyDisabledIgnored() {
		Signal<String> text = Signal.of("blocked");
		SolimTextField tf = new SolimTextField(text);

		boolean[] submitted = {false};
		tf.onEnter(() -> submitted[0] = true);

		tf.disabled(true);
		assertTrue(tf.field().isDisabled());

		simulateKey(tf, KeyCode.enter);
		assertFalse(submitted[0], "Disabled text field must not submit on enter");

		tf.disabled(false);
		assertFalse(tf.field().isDisabled());

		simulateKey(tf, KeyCode.enter);
		assertTrue(submitted[0], "Enabled text field should submit on enter");

		tf.dispose();
	}

	@Test
	void validationFeedback() {
		Signal<String> text = Signal.of("abc");
		SolimTextField tf = new SolimTextField(text);

		// Validates length >= 3
		tf.validator(s -> s != null && s.length() >= 3);
		assertTrue(tf.isValid());
		assertTrue(tf.valid().get());

		// Signal update that fails validation
		text.set("ab");
		SignalDispatcher.flush();
		assertFalse(tf.isValid());
		assertFalse(tf.valid().get());

		// Signal update that passes validation
		text.set("abcd");
		SignalDispatcher.flush();
		assertTrue(tf.isValid());
		assertTrue(tf.valid().get());

		tf.dispose();
	}

	@Test
	void reactiveDisabledBinding() {
		Signal<Boolean> disabled = Signal.of(false);
		SolimTextField tf = new SolimTextField(Signal.of("test")).disabled(disabled);

		assertFalse(tf.field().isDisabled());

		disabled.set(true);
		SignalDispatcher.flush();
		assertTrue(tf.field().isDisabled());

		disabled.set(false);
		SignalDispatcher.flush();
		assertFalse(tf.field().isDisabled());

		tf.dispose();
	}

	@Test
	void typeMessageSendMessageClearMessageCycle() {
		Signal<String> messageSignal = Signal.of("");
		SolimTextField tf = new SolimTextField(messageSignal);

		String[] sentMessage = {null};
		tf.onEnter(msg -> {
			sentMessage[0] = msg;
			messageSignal.set(""); // Clear message upon sending (e.g. ChatInputView behavior)
			SignalDispatcher.flush();
		});

		// Initial state
		assertEquals("", tf.field().getText());
		assertEquals("", messageSignal.get());

		// 1. "type message": user inputs first message into the text field
		tf.field().setText("Hello world");
		tf.field().change();

		// Check the value signal too
		assertEquals("Hello world", tf.field().getText());
		assertEquals("Hello world", messageSignal.get(), "Signal must track typed message");

		// 2. "send message": enter key triggers onEnter callback
		simulateKey(tf, KeyCode.enter);
		assertEquals("Hello world", sentMessage[0], "Sent message must match typed message");

		// 3. "clear message": field text and signal should be cleared
		assertEquals("", tf.field().getText(), "Field text must be cleared after send");
		assertEquals("", messageSignal.get(), "Signal must be cleared after send");

		// 4. "type message again": user inputs second message
		tf.field().setText("How are you?");
		tf.field().change();

		// 5. "check the value signal too": verify signal updates on second message
		assertEquals("How are you?", tf.field().getText(), "Field text must reflect new input");
		assertEquals("How are you?", messageSignal.get(), "Signal must reflect second typed message");

		// Send second message and verify clearing
		simulateKey(tf, KeyCode.enter);
		assertEquals("How are you?", sentMessage[0]);
		assertEquals("", tf.field().getText());
		assertEquals("", messageSignal.get());

		tf.dispose();
	}
}
