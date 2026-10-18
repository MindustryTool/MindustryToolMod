package mindustrytool.components;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.features.FeatureKeybind;
import mindustrytool.features.FeatureKeybindManager;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class SettingKeybindRow extends Component {

    private final FeatureKeybind keybind;
    private final Signal<Boolean> listening = Signal.of(false);
    private boolean listeningActive = false;

    public static boolean isAnyListening() {
        return FeatureKeybindManager.isCapturing();
    }

    public SettingKeybindRow(FeatureKeybind keybind) {
        this.keybind = keybind;
    }

    @Override
    protected Element build() {
        Readable<String> keyText = Signal.computed(() -> {
            if (Boolean.TRUE.equals(listening.get())) {
                return Core.bundle.get("keybind.listening.prompt", "Press any key...");
            }
            KeyCode code = keybind.keySignal().get();
            if (code == null || code == KeyCode.unset) {
                return Core.bundle.get("keybind.label.unset", "None");
            }
            return code.value;
        });

        Readable<Color> keyTextColor = Signal.computed(() -> {
            if (Boolean.TRUE.equals(listening.get())) {
                return WebStyles.Colors.PRIMARY;
            }
            KeyCode code = keybind.keySignal().get();
            return (code == null || code == KeyCode.unset)
                    ? WebStyles.Colors.GHOST_FG
                    : WebStyles.Colors.SECONDARY_FG;
        });

        return row()
                .props(growX(), gap(unit(2)), center())
                .children(() -> {

                    column()
                            .props(growX(), gap(unit(0.5f)), left())
                            .children(() -> {
                                text(resolveBindName())
                                        .props(color(Color.white), left());

                                text(Core.bundle.get("keybind.listening.help",
                                        "Press Esc to cancel, Backspace to clear"))
                                        .props(color(WebStyles.Colors.GHOST_FG), left(), visible(listening));
                            });

                    spacer();

                    button()
                            .props(
                                    style(listening.map(l -> Boolean.TRUE.equals(l)
                                            ? WebStyles.keybindListening().style()
                                            : WebStyles.keybindChip().style())),
                                    height(unit(8)),
                                    prefHeight(unit(8)),
                                    paddingX(unit(2.5f)))
                            .onClick(this::toggleListening)
                            .children(() -> text(keyText).props(color(keyTextColor)));

                    button()
                            .props(
                                    style(WebStyles.outline()),
                                    size(unit(8)),
                                    prefWidth(unit(8)),
                                    prefHeight(unit(8)),
                                    enabled(keybind.keySignal().map(k -> !keybind.isDefault())),
                                    tooltip(Core.bundle.get("keybind.button.reset", "Reset to default")))
                            .onClick(() -> {
                                cancelListening();
                                Core.app.post(keybind::resetToDefault);
                            })
                            .children(() -> icon(Icon.refresh)
                                    .props(
                                            size(unit(5)),
                                            prefWidth(unit(5)),
                                            prefHeight(unit(5)),
                                            color(keybind.keySignal().map(k -> keybind.isDefault()
                                                    ? WebStyles.Colors.DISABLED_FG
                                                    : WebStyles.Colors.GHOST_FG))));
                })
                .element();
    }

    private void toggleListening() {
        if (Boolean.TRUE.equals(listening.peek())) {
            cancelListening();
        } else {
            startListening();
        }
    }

    public void startListening() {
        if (listeningActive) {
            return;
        }
        listeningActive = true;
        FeatureKeybindManager.beginCapture(this);
        listening.set(true);
    }

    public void cancelListening() {
        if (!listeningActive) {
            return;
        }
        listeningActive = false;
        FeatureKeybindManager.endCapture(this);
        listening.set(false);
    }

    public void handleInput(KeyCode code) {
        if (code == KeyCode.escape) {
            cancelListening();
            return;
        }
        if (code == KeyCode.backspace) {
            cancelListening();
            keybind.rebind(KeyCode.unset);
            return;
        }
        if (code == KeyCode.mouseLeft) {

            return;
        }
        cancelListening();
        keybind.rebind(code);
    }

    @Override
    public void dispose() {
        cancelListening();
        super.dispose();
    }

    private String resolveBindName() {
        String bindName = keybind.getBind().name;
        String key = "keybind." + bindName + ".name";
        return Core.bundle.has(key) ? Core.bundle.get(key) : bindName;
    }
}
