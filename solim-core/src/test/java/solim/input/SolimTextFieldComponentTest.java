package solim.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class SolimTextFieldComponentTest extends SolimEnv {

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
void signalUpdatesFieldText() {
    Signal<String> input = Signal.of("");
    SolimTextField tf = new SolimTextField(input);

    assertEquals("", tf.field().getText());

    input.set("hello");
    SignalDispatcher.flush();
    assertEquals("hello", tf.field().getText());
    tf.dispose();
}

@Test
void fieldTextUpdatesSignal() {
    Signal<String> input = Signal.of("a");
    SolimTextField tf = new SolimTextField(input);

    tf.field().setText("b");
    tf.field().change();
    assertEquals("b", input.get());
    SignalDispatcher.flush();
    tf.dispose();
}

@Test
void enterKeyTriggersSubmission() {
    Signal<String> text = Signal.of("hello world");
    SolimTextField tf = new SolimTextField(text);
    String[] submitted = {null};
    tf.onEnter(submittedText -> submitted[0] = submittedText);

    simulateKey(tf, KeyCode.enter);
    assertEquals("hello world", submitted[0]);
    tf.dispose();
}

@Test
void disabledStateTogglesFieldDisabled() {
    Signal<String> text = Signal.of("blocked");
    SolimTextField tf = new SolimTextField(text);

    tf.disabled(true);
    assertTrue(tf.field().isDisabled());

    tf.disabled(false);
    assertFalse(tf.field().isDisabled());
    tf.dispose();
}

@Test
void reactiveDisabledUpdatesFieldDisabled() {
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
void validatorUpdatesIsValid() {
    Signal<String> text = Signal.of("abc");
    SolimTextField tf = new SolimTextField(text);
    tf.validator(s -> s != null && s.length() >= 3);

    assertTrue(tf.isValid());

    text.set("ab");
    SignalDispatcher.flush();
    assertFalse(tf.isValid());

    text.set("abcd");
    SignalDispatcher.flush();
    assertTrue(tf.isValid());
    tf.dispose();
}

@Test
void nameModifierUpdatesElementName() {
    SolimTextField tf = new SolimTextField(Signal.of("")).name("tf");
    assertEquals("tf", tf.element().name);
    tf.dispose();
}

@Test
void elementIsSameAsField() {
    SolimTextField tf = new SolimTextField(Signal.of(""));
    assertSame(tf.field(), tf.element());
    tf.dispose();
}

@Test
void typeMessageSendMessageClearMessageCycle() {
    Signal<String> messageSignal = Signal.of("");
    SolimTextField tf = new SolimTextField(messageSignal);
    String[] sent = {null};
    tf.onEnter(msg -> {
        sent[0] = msg;
        messageSignal.set("");
        SignalDispatcher.flush();
    });

    // 1. "type message"
    tf.field().setText("hello");
    tf.field().change();
    assertEquals("hello", tf.field().getText());
    assertEquals("hello", messageSignal.get(), "Signal must match typed message");

    // 2. "send message"
    simulateKey(tf, KeyCode.enter);
    assertEquals("hello", sent[0]);

    // 3. "clear message"
    assertEquals("", tf.field().getText(), "Field must be cleared");
    assertEquals("", messageSignal.get(), "Signal must be cleared");

    // 4. "type message again"
    tf.field().setText("world");
    tf.field().change();

    // 5. "check the value signal too"
    assertEquals("world", tf.field().getText());
    assertEquals("world", messageSignal.get(), "Signal must reflect second message");

    SignalDispatcher.flush();
    tf.dispose();
}
}
