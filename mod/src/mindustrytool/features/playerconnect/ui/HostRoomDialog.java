package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

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
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class HostRoomDialog extends SolimDialog {

    public HostRoomDialog(PlayerConnectFeature feature) {
        super(Core.bundle.get("feature.player-connect.host-room", "Host Room"));

        name("hostRoomDialog");
        addCloseButton();
        closeOnBack();
        cont().center();

        children(() -> new HostRoomView(feature, this));
    }

    private static class HostRoomView extends BaseComponent {
        private final PlayerConnectFeature feature;
        private final HostRoomDialog dialog;

        private final Signal<Integer> step = Signal.of(1);
        private final Signal<PlayerConnectProvider> selectedProvider = Signal.of(null);
        private final ObjectMap<String, Signal<String>> pings = new ObjectMap<>();

        public HostRoomView(PlayerConnectFeature feature, HostRoomDialog dialog) {
            this.feature = feature;
            this.dialog = dialog;
        }

        @Override
        protected Element build() {
            return column()
                    .growX()
                    .maxWidth(dvw(90f).map(w -> Math.min(w, 1100f)))
                    .maxHeight(dvh(85f).map(h -> Math.min(h, 1200f)))
                    .gap(unit(3))
                    .center()
                    .children(() -> {
                        dynamic(step, currentStep -> {
                            if (currentStep == 1) {
                                buildStep1();
                            } else {
                                buildStep2();
                            }
                        }).growX();
                    }).element();
        }

        private Component buildStep1() {
            return column()
                    .growX()
                    .gap(unit(3.5f))
                    .center()
                    .children(() -> {
                        text(Core.bundle.get("feature.player-connect.step1-title", "Room Settings"))
                                .color(Pal.accent);

                        // Room Name
                        column().growX().gap(unit(1.5f)).left().children(() -> {
                            text(Core.bundle.get("feature.player-connect.room-name", "Room Name:")).left();
                            row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2))
                                    .rounded(unit(2))
                                    .children(() -> {
                                        textField(feature.roomNameConfig.signal())
                                                .grow()
                                                .style(WebStyles.clearInput());
                                    });
                        });

                        // Password
                        column().growX().gap(unit(1.5f)).left().children(() -> {
                            text(Core.bundle.get("feature.player-connect.password", "Password:")).left();
                            row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2))
                                    .rounded(unit(2))
                                    .children(() -> {
                                        textField(feature.passwordConfig.signal())
                                                .grow()
                                                .style(WebStyles.clearInput());
                                    });
                        });

                        // Max Players
                        Signal<String> maxPlayersSig = Signal.of(String.valueOf(feature.maxPlayerConfig.get()));
                        maxPlayersSig.subscribe(str -> {
                            try {
                                if (str != null) {
                                    feature.maxPlayerConfig.set(Math.max(0, Integer.parseInt(str.trim())));
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        });
                        column().growX().gap(unit(1.5f)).left().children(() -> {
                            text(Core.bundle.get("feature.player-connect.max-players", "Max Players (0=unlimited):"))
                                    .left();
                            row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2))
                                    .rounded(unit(2))
                                    .children(() -> {
                                        textField(maxPlayersSig)
                                                .grow()
                                                .style(WebStyles.clearInput());
                                    });
                        });

                        // Auto Accept
                        column().growX().gap(unit(1.5f)).left().children(() -> {
                            text(Core.bundle.get("feature.player-connect.auto-accept", "Auto-accept players:"))
                                    .left();
                            button(feature.autoAcceptConfig.signal().map(v -> Boolean.TRUE.equals(v)
                                    ? Core.bundle.get("yes", "Yes")
                                    : Core.bundle.get("no", "No")), () -> {
                                        feature.autoAcceptConfig
                                                .set(!Boolean.TRUE.equals(feature.autoAcceptConfig.get()));
                                    })
                                            .style(WebStyles.outline())
                                            .height(unit(10))
                                            .paddingX(unit(4))
                                            .paddingY(unit(2))
                                            .minWidth(unit(24));
                        });

                        button(Core.bundle.get("next", "Next"), () -> {
                            step.set(2);
                            pingAllProviders();
                        })
                                .style(WebStyles.primary())
                                .height(unit(11))
                                .paddingX(unit(8))
                                .paddingY(unit(2.5f))
                                .minWidth(unit(36));
                    });
        }

        private Component buildStep2() {
            return column()
                    .growX()
                    .gap(unit(3))
                    .center()
                    .children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            text(Core.bundle.get("feature.player-connect.select-provider", "Select Relay Provider"))
                                    .ellipsis()
                                    .growX()
                                    .color(Pal.accent);
                            spacer();
                            button(Core.bundle.get("refresh", "Refresh"), () -> {
                                feature.refreshProviders();
                                pingAllProviders();
                            })
                                    .style(WebStyles.outline())
                                    .height(unit(9))
                                    .paddingX(unit(4))
                                    .paddingY(unit(2));

                            button("+ " + Core.bundle.get("custom", "Custom"), this::showAddCustomDialog)
                                    .style(WebStyles.outline())
                                    .height(unit(9))
                                    .paddingX(unit(4))
                                    .paddingY(unit(2));
                        });

                        divider();

                        scroll().height(unit(80)).growX().scrollX(false).children(() -> {
                            column().growX().gap(unit(2)).children(() -> {
                                for (PlayerConnectProvider p : feature.providersSignal().get()) {
                                    providerCard(p);
                                }
                            });
                        });

                        divider();

                        row().gap(unit(4)).center().children(() -> {
                            button(Core.bundle.get("back", "Back"), () -> step.set(1))
                                    .style(WebStyles.outline())
                                    .height(unit(11))
                                    .paddingX(unit(6))
                                    .paddingY(unit(2.5f))
                                    .minWidth(unit(28));

                            button(Core.bundle.get("feature.player-connect.start-hosting", "Start Hosting"),
                                    this::startHosting)
                                            .style(WebStyles.primary())
                                            .height(unit(11))
                                            .paddingX(unit(8))
                                            .paddingY(unit(2.5f))
                                            .minWidth(unit(36))
                                            .enabled(selectedProvider.map(p -> p != null));
                        });
                    });
        }

        private Component providerCard(PlayerConnectProvider provider) {
            Signal<String> pingSignal = getOrCreatePingSignal(provider.getAddress());
            Computed<Boolean> isSelected = selectedProvider
                    .map(sel -> sel != null && sel.getAddress().equals(provider.getAddress()));
            Computed<Boolean> showAddress = dvw(100f).map(w -> w != null && w >= 700f);

            return card()
                    .background(WebStyles.Colors.SECONDARY)
                    .border(1.5f, isSelected.map(s -> s ? Pal.accent : WebStyles.Colors.BORDER_INPUT))
                    .growX()
                    .minHeight(unit(12))
                    .padding(unit(2), unit(3), unit(2), unit(3))
                    .onClick(() -> selectedProvider.set(provider))
                    .children(() -> {
                        row().growX().gap(unit(2)).center().children(() -> {
                            text(provider.getName()).color(isSelected.map(s -> s ? Pal.accent : Color.white)).left();
                            spacer();
                            when(showAddress)
                                    .thenDo(() -> text(provider.getAddress()).color(Color.lightGray));
                            text(pingSignal)
                                    .color(pingSignal.map(this::getPingColor))
                                    .width(unit(22))
                                    .right();
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
