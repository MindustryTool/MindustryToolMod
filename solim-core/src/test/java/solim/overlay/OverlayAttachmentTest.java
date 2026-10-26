package solim.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockAudio;
import arc.scene.Element;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.layout.Row;
import solim.test.SolimEnv;

class OverlayAttachmentTest extends SolimEnv {

    private static Font testFont() {
        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        return new Font(fontData, new TextureRegion(), false);
    }

    @BeforeEach
    void setUp() {
        if (Core.audio == null) {
            Core.audio = new MockAudio();
        }
        newScene();
        Font font = testFont();
        try {
            Core.scene.getStyle(ButtonStyle.class);
        } catch (IllegalArgumentException missing) {
            Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());
        }
        try {
            Core.scene.getStyle(DialogStyle.class);
        } catch (IllegalArgumentException missing) {
            DialogStyle style = new DialogStyle();
            style.titleFont = font;
            Core.scene.addStyle(DialogStyle.class, style);
        }
        try {
            Core.scene.getStyle(TextButtonStyle.class);
        } catch (IllegalArgumentException missing) {
            TextButtonStyle style = new TextButtonStyle();
            style.font = font;
            Core.scene.addStyle(TextButtonStyle.class, style);
        }
        try {
            Core.scene.getStyle(LabelStyle.class);
        } catch (IllegalArgumentException missing) {
            LabelStyle style = new LabelStyle();
            style.font = font;
            Core.scene.addStyle(LabelStyle.class, style);
        }
        try {
            Core.scene.getStyle(TextFieldStyle.class);
        } catch (IllegalArgumentException missing) {
            TextFieldStyle style = new TextFieldStyle();
            style.font = font;
            Core.scene.addStyle(TextFieldStyle.class, style);
        }
    }

    @Test
    void popupCreatedInsideContainerDoesNotAttachToContainerTable() {
        Row row = new Row();
        AtomicReference<Popup<String>> popupRef = new AtomicReference<>();
        row.children(() -> {
            popupRef.set(new Popup<String>());
        });
        Element el = row.element();
        assertNull(popupRef.get().table().parent);
        assertEquals(0, ((Table) el).getChildren().size);
        assertFalse(popupRef.get().isDisposed());

        row.dispose();
        assertTrue(popupRef.get().isDisposed());
    }

    @Test
    void dialogCreatedInsideContainerDoesNotAttachToContainerTable() {
        Row row = new Row();
        AtomicReference<SolimDialog> dialogRef = new AtomicReference<>();
        row.children(() -> {
            dialogRef.set(new SolimDialog("Test"));
        });
        Element el = row.element();
        assertNull(dialogRef.get().dialog().parent);
        assertEquals(0, ((Table) el).getChildren().size);
        assertFalse(dialogRef.get().isDisposed());

        row.dispose();
        assertTrue(dialogRef.get().isDisposed());
    }
}
