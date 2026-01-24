package mindustrytool.features.chat.translation;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.gen.SendMessageCallPacket;
import mindustry.gen.SendMessageCallPacket2;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.LanguageDialog;
import mindustrytool.Main;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import arc.struct.Seq;

import java.util.Optional;

public class ChatTranslationFeature implements Feature {
    private final TranslationProvider NOOP_PROVIDER = new NoopTranslationProvider();
    private final Seq<TranslationProvider> providers = new Seq<>();
    private TranslationProvider currentProvider = NOOP_PROVIDER;
    private String lastError = null;
    private boolean enabled = false;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("chat-translation")
                .description("chat-translation.description")
                .icon(mindustry.gen.Icon.chat)
                .build();
    }

    public class SendTranslatedMessageCallPacket extends SendMessageCallPacket {

        @Override
        public void handleClient() {
            handleMessage(this.message, translated -> {
                NetClient.sendMessage(translated);
            });
        }
    }

    public class SendTranslatedMessageCallPacket2 extends SendMessageCallPacket2 {
        @Override
        public void handleClient() {
            if (Vars.player != this.playersender) {
                handleMessage(this.message, translated -> {
                    NetClient.sendMessage(translated, this.unformatted,
                            this.playersender);
                });
            } else {
                NetClient.sendMessage(this.message, this.unformatted,
                        this.playersender);
            }
        }
    }

    @Override
    public void init() {
        Main.registerPacketPlacement(SendMessageCallPacket.class, SendTranslatedMessageCallPacket::new);
        Main.registerPacketPlacement(SendMessageCallPacket2.class, SendTranslatedMessageCallPacket2::new);

        providers.add(NOOP_PROVIDER);
        providers.add(new GeminiTranslationProvider());
        providers.add(new DeepLTranslationProvider());

        providers.each(TranslationProvider::init);

        loadProvider();

    }

    public void handleMessage(String message, Cons<String> cons) {
        if (!enabled) {
            cons.get(message);
            return;
        }

        currentProvider.translate(Strings.stripColors(message))
                .thenApply(translated -> {
                    if (ChatTranslationConfig.isShowOriginal()) {
                        String locale = LanguageDialog.getDisplayName(Core.bundle.getLocale());
                        String formated = Strings.format("@\n\n[]@[]@\n\n", message, locale, translated);

                        return formated;
                    }

                    return translated;
                })
                .thenAccept(formated -> cons.get(formated))
                .exceptionally(e -> {
                    lastError = e.getMessage();

                    String formated = Strings.format("@\n\n@\n\n", message,
                            Core.bundle.get("chat-translation.error.prefix") + e.getMessage());

                    cons.get(formated);

                    Log.err("Translation failed", e);
                    return null;
                });

    }

    private void loadProvider() {
        String id = ChatTranslationConfig.getProviderId();
        currentProvider = providers.find(p -> p.getId().equals(id));

        if (currentProvider == null) {
            currentProvider = NOOP_PROVIDER;
        }
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @Override
    public Optional<Dialog> setting() {
        BaseDialog dialog = new BaseDialog(Core.bundle.get("chat-translation.settings.title"));
        dialog.addCloseButton();

        Table root = new Table();
        root.top().left().defaults().top().left().padBottom(5);

        root.check("@chat-translation.settings.show-original", ChatTranslationConfig.isShowOriginal(), val -> {
            ChatTranslationConfig.setShowOriginal(val);
        }).row();

        root.image().height(4).color(Color.gray).fillX().pad(10).row();

        root.add("@chat-translation.settings.providers").style(Styles.outlineLabel).padBottom(5).row();

        Table providerList = new Table();
        ButtonGroup<Button> group = new ButtonGroup<>();
        group.setMinCheckCount(1);

        for (TranslationProvider prov : providers) {
            Button card = new Button(Styles.togglet);
            card.top().left().margin(12);

            card.table(h -> {
                h.left();
                h.label(() -> prov.getName()).style(Styles.defaultLabel).growX().left();
            }).growX().row();

            card.collapser(s -> {
                s.add(prov.settings()).growX().padTop(10);
            }, false, card::isChecked).growX().row();

            card.clicked(() -> {
                if (currentProvider != prov) {
                    currentProvider = prov;
                    var isNoop = prov.getId().equals(NOOP_PROVIDER.getId());
                    ChatTranslationConfig.setProviderId(prov.getId());
                    FeatureManager.getInstance().setEnabled(this, !isNoop);
                }
            });

            if (currentProvider == prov) {
                card.setChecked(true);
            }

            group.add(card);
            providerList.add(card).growX().padBottom(8).row();
        }

        root.add(providerList).growX().row();

        root.image().height(4).color(Color.gray).fillX().pad(10).row();

        Label resultLabel = new Label("");
        resultLabel.setWrap(true);
        resultLabel.setAlignment(Align.center);

        TextButton testButton = new TextButton(Core.bundle.get("chat-translation.settings.test-button"),
                Styles.defaultt);
        testButton.clicked(() -> {
            if (currentProvider == NOOP_PROVIDER)
                return;

            testButton.setDisabled(true);
            testButton.setText(Core.bundle.get("chat-translation.settings.testing"));
            resultLabel.setText(Core.bundle.get("chat-translation.settings.testing-connection"));

            currentProvider.translate("Hello").thenAccept(result -> {
                Core.app.post(() -> {
                    testButton.setDisabled(false);
                    testButton.setText(Core.bundle.get("chat-translation.settings.test-button"));
                    resultLabel.setText(Core.bundle.get("chat-translation.settings.success") + result);
                });
            }).exceptionally(e -> {
                Core.app.post(() -> {
                    testButton.setDisabled(false);
                    testButton.setText(Core.bundle.get("chat-translation.settings.test-button"));
                    resultLabel.setText(Core.bundle.get("chat-translation.settings.failed") + e.getMessage());
                });
                return null;
            });
        });

        root.add(testButton).size(250, 50).pad(10)
                .disabled(b -> currentProvider == NOOP_PROVIDER
                        || testButton.getText().toString().equals(Core.bundle.get("chat-translation.settings.testing")))
                .row();
        root.add(resultLabel).growX().pad(10).row();

        root.label(() -> lastError == null ? "" : Core.bundle.get("chat-translation.settings.error-prefix") + lastError)
                .color(Color.red)
                .visible(() -> lastError != null)
                .row();

        dialog.cont.pane(root).growX().maxWidth(700f).margin(20);
        dialog.cont.pack();

        return Optional.of(dialog);
    }
}
