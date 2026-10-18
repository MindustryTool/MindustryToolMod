package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.ellipsis;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.minHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.onClick;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.color;
import static solim.modifier.Values.paddingY;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.scrollX;
import static solim.modifier.Values.style;
import static solim.modifier.Values.enabled;
import static solim.modifier.Values.width;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.models.response.PlayerConnectProvider;
import solim.core.Component;
import solim.core.Units;
import solim.modifier.Values;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class HostRoomDialog extends SolimDialog {

    public HostRoomDialog(PlayerConnectFeature feature) {
        super(Core.bundle.get("feature.player-connect.host-room", "Host Room"));

        name("hostRoomDialog");
        closeOnBack();
        cont().center();

        children(() -> new HostRoomView(feature, this));
    }

    private static class HostRoomView extends Component {
        private static enum Step {
            CONFIG, PROVIDER
        }

        private final PlayerConnectFeature feature;
        private final HostRoomDialog dialog;

        private final Signal<Step> step = Signal.of(Step.CONFIG);
        private final Signal<PlayerConnectProvider> selectedProvider = Signal.of(null);
        private final ObjectMap<String, Signal<String>> pings = new ObjectMap<>();

        public HostRoomView(PlayerConnectFeature feature, HostRoomDialog dialog) {
            this.feature = feature;
            this.dialog = dialog;

            dialog.shown(() -> {
                Core.app.post(() -> {
                    step.set(Step.CONFIG);
                });
            });
        }

        @Override
        protected Element build() {
            return column()
                    .props(growX(), Values.maxWidth(Units.dvw.map(w -> Math.min(w * 0.9f, 1100f))), maxHeight(Units.dvh.map(h -> Math.min(h * 0.85f, 1200f))), gap(unit(3)), center())
                    .children(() -> {
                        dynamic(step, currentStep -> {
                            if (currentStep == Step.CONFIG) {
                                buildConfig();
                            } else {
                                buildProvider();
                            }
                        }).props(growX());
                    }).element();
        }

        private Component buildConfig() {
            return column()
                    .props(growX(), gap(unit(3.5f)), center())
                    .children(() -> {
                        text(Core.bundle.get("feature.player-connect.step1-title", "Room Settings"))
                                .props(color(Pal.accent));

                        column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.room-name", "Room Name:")).props(left());
                            row().props(growX(), height(unit(11)), prefHeight(unit(11)), Values.border(1.5f, Color.darkGray), paddingX(unit(2)), Values.rounded(unit(2)))
                                    .children(() -> {
                                        textField(feature.roomNameConfig.signal())
                                                .props(style(WebStyles.clearInput()), grow());
                                    });
                        });

                        column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.password", "Password:")).props(left());
                            row().props(growX(), height(unit(11)), prefHeight(unit(11)), Values.border(1.5f, Color.darkGray), paddingX(unit(2)), Values.rounded(unit(2)))
                                    .children(() -> {
                                        textField(feature.passwordConfig.signal())
                                                .props(style(WebStyles.clearInput()), grow());
                                    });
                        });

                        Signal<String> maxPlayersSig = Signal.of(String.valueOf(feature.maxPlayerConfig.get()));
                        Effect.of(maxPlayersSig, str -> {
                            try {
                                if (str != null) {
                                    feature.maxPlayerConfig.set(Math.max(0, Integer.parseInt(str.trim())));
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        });
                        column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.max-players", "Max Players (0=unlimited):")).props(left());
                            row().props(growX(), height(unit(11)), prefHeight(unit(11)), Values.border(1.5f, Color.darkGray), paddingX(unit(2)), Values.rounded(unit(2)))
                                    .children(() -> {
                                        textField(maxPlayersSig)
                                                .props(style(WebStyles.clearInput()), grow());
                                    });
                        });

                        column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.auto-accept", "Auto-accept players:")).props(left());
                            button(feature.autoAcceptConfig.signal().map(v -> Boolean.TRUE.equals(v)
                                    ? Core.bundle.get("yes", "Yes")
                                    : Core.bundle.get("no", "No")), () -> {
                                        feature.autoAcceptConfig
                                                .set(!Boolean.TRUE.equals(feature.autoAcceptConfig.get()));
                                    })
                                    .props(style(WebStyles.outline()), height(unit(10)), prefHeight(unit(10)), paddingX(unit(4)), paddingY(unit(2)), minWidth(unit(24)));
                        });

                        row().props(gap(unit(2))).children(() -> {
                            button(Core.bundle.get("cancel", "Cancel"), dialog::hide)
                                    .props(style(WebStyles.outline()), height(unit(11)), prefHeight(unit(11)), paddingX(unit(6)), paddingY(unit(2.5f)), minWidth(unit(28)));

                            button(Core.bundle.get("next", "Next"), () -> {
                                step.set(Step.PROVIDER);
                                pingAllProviders();
                            })
                                    .props(style(WebStyles.primary()), height(unit(11)), prefHeight(unit(11)), paddingX(unit(8)), paddingY(unit(2.5f)), minWidth(unit(36)));
                        });
                    });
        }

        private Component buildProvider() {
            return column()
                    .props(growX(), gap(unit(3)), center())
                    .children(() -> {
                        row().props(growX(), gap(unit(2)), center()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.select-provider", "Select Relay Provider"))
                                    .props(color(Pal.accent), ellipsis(true), growX());
                            spacer();
                            button(Core.bundle.get("refresh", "Refresh"), () -> {
                                feature.refreshProviders();
                                pingAllProviders();
                            })
                                    .props(style(WebStyles.outline()), height(unit(9)), prefHeight(unit(9)), paddingX(unit(4)), paddingY(unit(2)));

                            button("+ " + Core.bundle.get("custom", "Custom"), this::showAddCustomDialog)
                                    .props(style(WebStyles.outline()), height(unit(9)), prefHeight(unit(9)), paddingX(unit(4)), paddingY(unit(2)));
                        });

                        divider();

                        scroll().props(height(unit(80)), prefHeight(unit(80)), growX(), scrollX(false)).children(() -> {
                            column().props(growX(), gap(unit(2))).children(() -> {
                                for (PlayerConnectProvider p : feature.providersSignal().get()) {
                                    providerCard(p);
                                }
                            });
                        });

                        divider();

                        row().props(gap(unit(4)), center()).children(() -> {
                            button(Core.bundle.get("back", "Back"), () -> step.set(Step.CONFIG))
                                    .props(style(WebStyles.outline()), height(unit(11)), prefHeight(unit(11)), paddingX(unit(6)), paddingY(unit(2.5f)), minWidth(unit(28)));

                            button(Core.bundle.get("feature.player-connect.start-hosting", "Start Hosting"),
                                    this::startHosting)
                                    .props(style(WebStyles.primary()), enabled(selectedProvider.map(p -> p != null)), height(unit(11)), prefHeight(unit(11)), paddingX(unit(8)), paddingY(unit(2.5f)), minWidth(unit(36)));
                        });
                    });
        }

        private Component providerCard(PlayerConnectProvider provider) {
            Signal<String> pingSignal = getOrCreatePingSignal(provider.getAddress());
            Computed<Boolean> isSelected = selectedProvider
                    .map(sel -> sel != null && sel.getAddress().equals(provider.getAddress()));
            Computed<Boolean> showAddress = Units.dvw.map(w -> w >= 700f);

            return card()
                    .props(background(WebStyles.Colors.SECONDARY), Values.border(1.5f, isSelected.map(s -> s ? Pal.accent : WebStyles.Colors.BORDER_INPUT)), growX(), minHeight(unit(12)), padding(unit(2), unit(3), unit(2), unit(3)), onClick(() -> selectedProvider.set(provider)))
                    .children(() -> {
                        row().props(growX(), gap(unit(2)), center()).children(() -> {
                            text(provider.getName()).props(color(isSelected.map(s -> s ? Pal.accent : Color.white)), left());
                            spacer();
                            when(showAddress)
                                    .thenDo(() -> text(provider.getAddress()).props(color(Color.lightGray)));
                            text(pingSignal)
                                    .props(color(pingSignal.map(this::getPingColor)), width(unit(22)), prefWidth(unit(22)), solim.modifier.Values.right());
                        });
                    });
        }

        private Color getPingColor(@Nullable String ping) {
            if (ping == null || "...".equals(ping)) {
                return Color.lightGray;
            }
            if (ping.endsWith("ms")) {
                try {
                    int ms = Integer.parseInt(ping.substring(0, ping.length() - 2).trim());
                    return ms < 200 ? Color.green : (ms < 500 ? Color.yellow : Color.scarlet);
                } catch (NumberFormatException ignored) {
                }
            }
            return Color.scarlet;
        }

        private Signal<String> getOrCreatePingSignal(String address) {
            Signal<String> sig = pings.get(address);
            if (sig == null) {
                sig = Signal.of("...");
                pings.put(address, sig);
            }
            return sig;
        }

        private void pingAllProviders() {
            for (PlayerConnectProvider p : feature.providersSignal().get()) {
                String addr = p.getAddress();
                Signal<String> sig = getOrCreatePingSignal(addr);
                sig.set("...");

                String[] parts = addr.split(":");
                if (parts.length == 2) {
                    try {
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        PlayerConnectClient.pingHost(host, port, ms -> sig.set(ms + "ms"), err -> sig.set("timeout"));
                    } catch (Exception e) {
                        sig.set("err");
                    }
                }
            }
        }

        private void startHosting() {
            PlayerConnectProvider provider = selectedProvider.peek();
            if (provider == null) {
                return;
            }

            String addr = provider.getAddress();
            String[] parts = addr.split(":");
            if (parts.length != 2) {
                Vars.ui.showErrorMessage("Invalid provider address: " + addr);
                return;
            }

            try {
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                feature.createRoom(host, port, link -> {
                    dialog.hide();
                    Vars.ui.showInfoFade("@feature.player-connect.create-success");
                }, err -> {
                    Vars.ui.showException(err);
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage("Invalid port number in address: " + addr);
            }
        }

        private void showAddCustomDialog() {
            Vars.ui.showTextInput(
                    Core.bundle.get("feature.player-connect.add-provider", "Add Relay Provider"),
                    Core.bundle.get("feature.player-connect.provider-address-hint", "host:port"),
                    "",
                    input -> {
                        if (input != null && input.contains(":")) {
                            feature.addCustomProvider("Custom (" + input + ")", input.trim());
                            pingAllProviders();
                        }
                    });
        }
    }
}