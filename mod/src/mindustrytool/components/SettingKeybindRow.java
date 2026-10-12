package mindustrytool.components;

import static solim.UI.*;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustrytool.features.FeatureKeybind;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Modern declarative Solim row component for an individual feature keybind.
 * Displays keybind name, interactive key chip for inline rebinding, and a reset button.
 */
public class SettingKeybindRow extends BaseComponent {
    private static volatile boolean anyListening = false;

    private final FeatureKeybind keybind;
    private final Signal<Boolean> listening = Signal.of(false);
    private boolean listeningActive = false;

    public static boolean isAnyListening() {
        return anyListening;
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
                .growX()
                .gap(unit(2))
                .center()
                .children(() -> {
                    // Left: Keybind name & help
                    column()
                            .growX()
                            .gap(unit(0.5f))
                            .left()
                            .children(() -> {
                                text(resolveBindName())
                                        .color(Color.white)
                                        .left();

                                text(Core.bundle.get("keybind.listening.help", "Press Esc to cancel, Backspace to clear"))
                                        .color(WebStyles.Colors.GHOST_FG)
                                        .left()
                                        .visible(listening);
                            });

                    spacer();

                    // Keybind chip button
                    button()
                            .style(listening.map(l -> Boolean.TRUE.equals(l)
                                    ? WebStyles.keybindListening().style()
                                    : WebStyles.keybindChip().style()))
                            .height(unit(8))
                            .paddingX(unit(2.5f))
                            .onClick(this::toggleListening)
                            .children(() -> text(keyText).color(keyTextColor));

                    // Reset button
                    button()
                            .style(WebStyles.ghost())
                            .size(unit(8))
                            .enabled(keybind.keySignal().map(k -> !keybind.isDefault()))
                            .tooltip(Core.bundle.get("keybind.button.reset", "Reset to default"))
                            .onClick(() -> {
                                cancelListening();
                                keybind.resetToDefault();
                            })
                            .children(() -> icon(Icon.refresh)
                                    .size(unit(5))
                                    .color(keybind.keySignal().map(k -> keybind.isDefault()
                                            ? WebStyles.Colors.DISABLED_FG
                                            : WebStyles.Colors.GHOST_FG)));
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
        anyListening = true;
        listening.set(true);
        Events.run(Trigger.update, this::onUpdate);
    }

    public void cancelListening() {
        if (!listeningActive) {
            return;
        }
        listeningActive = false;
        anyListening = false;
        listening.set(false);
    }

    public void handleInput(KeyCode code) {
        if (code == KeyCode.escape) {
            cancelListening();
            return;
        }
        if (code == KeyCode.backspace) {
            keybind.rebind(KeyCode.unset);
            cancelListening();
            return;
        }
        keybind.rebind(code);
        cancelListening();
    }

    private void onUpdate() {
        if (!listeningActive) {
            return;
        }
        for (KeyCode key : KeyCode.all) {
            if (Core.input.keyTap(key)) {
                handleInput(key);
                break;
            }
        }
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
