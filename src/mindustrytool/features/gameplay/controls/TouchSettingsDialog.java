package mindustrytool.features.gameplay.controls;

import arc.Core;
import arc.scene.ui.Slider;
import arc.util.Strings;
import mindustry.ui.Styles;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Stack;

public class TouchSettingsDialog extends BaseDialog {

    public TouchSettingsDialog() {
        super("Touch Settings");
        addCloseButton();
        this.buttons.button("Reset to defaults", mindustry.gen.Icon.refresh, this::resetToDefaults).size(250f, 64f);
        setup();
    }

    private void setup() {
        cont.clear();

        Table table = new Table();
        table.margin(10f);

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // === Visuals Section ===
        table.add("@mdt.settings.touch.visuals").color(Pal.accent).left().padBottom(6f).row();
        table.image().color(Pal.accent).fillX().height(3f).padBottom(10f).row();

        // Style Selection
        table.table(t -> {
            t.left();
            t.add("Style: ").padRight(10f);
            String currentStyle = Core.settings.getString("touch-style", "ROUND");

            t.button("Joystick", Styles.flatToggleMenut, () -> {
                Core.settings.put("touch-style", "ROUND");
                setup();
                refreshHandler();
            }).checked(currentStyle.equals("ROUND")).size(120f, 40f).padRight(5f);

            t.button("D-Pad", Styles.flatToggleMenut, () -> {
                Core.settings.put("touch-style", "DIRECTIONAL");
                setup();
                refreshHandler();
            }).checked(currentStyle.equals("DIRECTIONAL")).size(120f, 40f);
        }).left().padBottom(10f).row();

        // Sliders
        addSlider(table, "Size", "touch-size-scale", 1.0f, 0.5f, 2.0f, true, width);
        addSlider(table, "Opacity", "touch-opacity", 0.5f, 0.1f, 1.0f, true, width);
        addSlider(table, "Sensitivity", "touch-sensitivity", 1.5f, 0.5f, 5.0f, false, width);

        table.add().height(20f).row();

        // === Features Section ===
        table.add("@mdt.settings.touch.features").color(Pal.accent).left().padBottom(6f).row();
        table.image().color(Pal.accent).fillX().height(3f).padBottom(10f).row();

        // Double Tap Toggle
        table.check("Double Tap to Follow", Core.settings.getBool("touch-enable-double-tap", true), val -> {
            Core.settings.put("touch-enable-double-tap", val);
            refreshHandler();
        }).left().padBottom(5f).row();

        // Unit Lock Toggle
        table.check("Hold to Lock Unit (5s)", Core.settings.getBool("touch-enable-unit-lock", true), val -> {
            Core.settings.put("touch-enable-unit-lock", val);
            refreshHandler();
        }).left().padBottom(5f).row();

        // Lock Position Toggle
        table.check("Lock Joystick Position", Core.settings.getBool("touch-locked", true), val -> {
            Core.settings.put("touch-locked", val);
            refreshHandler();
        }).left().padBottom(5f).row();

        // Wrap in ScrollPane
        cont.pane(table).grow();
    }

    private void addSlider(Table table, String name, String key, float def, float min, float max, boolean percent,
            float width) {
        float val = Core.settings.getFloat(key, def);
        Slider slider = new Slider(min, max, 0.05f, false);
        slider.setValue(val);

        arc.scene.ui.Label label = new arc.scene.ui.Label(
                percent ? Strings.fixed(val * 100, 0) + "%" : "x" + Strings.fixed(val, 1), Styles.outlineLabel);
        label.setColor(arc.graphics.Color.lightGray);

        Table content = new Table();
        content.touchable = arc.scene.event.Touchable.disabled;
        content.margin(3f, 33f, 3f, 33f);
        content.add(name, Styles.outlineLabel).left().growX();
        content.add(label).padLeft(10f).right();

        slider.changed(() -> {
            float v = slider.getValue();
            Core.settings.put(key, v);
            label.setText(percent ? Strings.fixed(v * 100, 0) + "%" : "x" + Strings.fixed(v, 1));
            refreshHandler();
        });

        Stack stack = new Stack();
        stack.add(slider);
        stack.add(content);

        table.add(stack).width(width).padBottom(4f).left().row();
    }

    private void refreshHandler() {
        if (TouchFeature.touchComponent.isLoaded()) {
            TouchFeature.touchComponent.get().rebuild();
        }
    }

    private void resetToDefaults() {
        Core.settings.put("touch-style", "ROUND");
        Core.settings.put("touch-size-scale", 1.0f);
        Core.settings.put("touch-opacity", 0.5f);
        Core.settings.put("touch-sensitivity", 1.5f);
        Core.settings.put("touch-enable-double-tap", true);
        Core.settings.put("touch-enable-unit-lock", true);
        Core.settings.put("touch-locked", true);

        setup();
        refreshHandler();
    }
}
