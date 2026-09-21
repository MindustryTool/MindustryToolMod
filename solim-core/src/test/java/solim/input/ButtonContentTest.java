package solim.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import org.junit.jupiter.api.Test;

import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ButtonContentTest extends SolimEnv {

    private static Label findLabel(Button btn) {
        for (Element child : btn.button().getChildren()) {
            if (child instanceof Label) {
                return (Label) child;
            }
        }
        return null;
    }

    private static Image findImage(Button btn) {
        for (Element child : btn.button().getChildren()) {
            if (child instanceof Image) {
                return (Image) child;
            }
        }
        return null;
    }

    @Test
    void textAppendsLabel() {
        Button btn = new Button().text("Save");
        Label label = findLabel(btn);
        assertNotNull(label);
        assertEquals("Save", label.getText().toString());
        btn.dispose();
    }

    @Test
    void reactiveTextUpdatesLabel() {
        Signal<String> text = Signal.of("A");
        Button btn = new Button().text(text);
        Label label = findLabel(btn);
        assertNotNull(label);
        assertEquals("A", label.getText().toString());

        text.set("B");
        SignalDispatcher.flush();
        assertEquals("B", label.getText().toString());
        btn.dispose();
    }

    @Test
    void iconAppendsImage() {
        Drawable drawable = new TextureRegionDrawable(new TextureRegion());
        Button btn = new Button().icon(drawable);
        assertNotNull(findImage(btn));
        btn.dispose();
    }

    @Test
    void reactiveIconUpdatesDrawable() {
        Drawable first = new TextureRegionDrawable(new TextureRegion());
        Drawable second = new TextureRegionDrawable(new TextureRegion());
        Signal<Drawable> icon = Signal.of(first);
        Button btn = new Button().icon(icon);
        Image image = findImage(btn);
        assertNotNull(image);
        assertNotNull(image.getDrawable());

        icon.set(second);
        SignalDispatcher.flush();
        assertNotNull(image.getDrawable());
        btn.dispose();
    }

    @Test
    void disposedButtonDropsReactiveTextBinding() {
        Signal<String> text = Signal.of("A");
        Button btn = new Button().text(text);
        Label label = findLabel(btn);
        assertNotNull(label);

        btn.dispose();
        assertTrue(btn.isDisposed());
        text.set("B");
        SignalDispatcher.flush();
        assertEquals("A", label.getText().toString());
    }
}
