package mindustrytool.features.prettychat.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Tooltip;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.prettychat.Prettier;
import mindustrytool.features.prettychat.PrettyChatFeature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Row;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

import java.util.List;

/**
 * Declarative Solim settings view for Pretty Chat.
 * Features an interactive real-time preview and a reorderable pipeline list.
 */
public class PrettyChatSettingsView extends BaseComponent {

    private final PrettyChatFeature feature;
    private final Signal<String> previewSignal;
    private final Computed<String> resultComputed;

    public PrettyChatSettingsView(PrettyChatFeature feature) {
        this.feature = feature;
        this.previewSignal = Signal.of("Hello World! This is a test message.");
        this.resultComputed = new Computed<>(() -> {
            String input = previewSignal.get();
            List<String> enabled = feature.config().enabledIdsSignal.get();
            return !enabled.isEmpty() && input != null && !input.isEmpty()
                    ? feature.transform(input)
                    : (input != null ? input : "");
        });
    }

    @Override
    protected Element build() {
        String previewLabel = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-label", "Preview Message")
                : "Preview Message";
        String previewPlaceholder = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-placeholder", "Type a message to preview...")
                : "Type a message to preview...";
        String resultLabel = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.result-label", "Result:")
                : "Result:";
        String pipelineTitle = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.pipeline-title", "Transformer Pipeline")
                : "Transformer Pipeline";
        String pipelineDesc = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.pipeline-desc",
                        "Enabled transformers run sequentially in the order shown below.")
                : "Enabled transformers run sequentially in the order shown below.";

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(2)).children(() -> {
                    // Preview Input Section
                    column().growX().gap(unit(1.5f)).left().children(() -> {
                        text(previewLabel).left();
                        row().growX().height(unit(10)).border(1.5f, WebStyles.Colors.BORDER)
                                .paddingX(unit(2)).rounded(unit(2)).children(() -> {
                                    textField(previewSignal)
                                            .placeholder(previewPlaceholder)
                                            .grow()
                                            .style(WebStyles.clearInput());
                                });
                    });

                    // Real-time Result Section
                    row().growX().gap(unit(2)).children(() -> {
                        text(resultLabel).color(Color.lightGray).left();
                        text(resultComputed).color(Pal.accent).wrap().left().growX();
                        text(resultComputed.map(r -> r.length() + "/150"))
                                .color(resultComputed.map(r -> r.length() > 150 ? Color.scarlet
                                        : (r.length() > 130 ? Color.gold : Color.lightGray)))
                                .right();
                    });

                    divider();

                    // Pipeline Section Header
                    column().growX().gap(unit(0.5f)).left().children(() -> {
                        text(pipelineTitle).color(Pal.accent).left();
                        text(pipelineDesc).color(Color.lightGray).left();
                    });

                    // Dynamic Prettier Cards List
                    dynamic(feature.config().enabledIdsSignal, this::buildCardList).growX();
                });
            });
        }).element();
    }

    private Component buildCardList(List<String> enabledIds) {
        Seq<Prettier> displayList = new Seq<>(feature.prettiers());
        displayList.sort((p1, p2) -> {
            int idx1 = enabledIds.indexOf(p1.id());
            int idx2 = enabledIds.indexOf(p2.id());
            if (idx1 != -1 && idx2 != -1) {
                return Integer.compare(idx1, idx2);
            }
            return idx1 != -1 ? -1 : (idx2 != -1 ? 1 : 0);
        });

        return column().growX().gap(unit(2)).children(() -> {
            for (int i = 0; i < displayList.size; i++) {
                Prettier p = displayList.get(i);
                boolean isEnabled = enabledIds.contains(p.id());
                int enabledIndex = isEnabled ? enabledIds.indexOf(p.id()) : -1;
                int enabledCount = enabledIds.size();

                Row cardRow = row()
                        .growX()
                        .padding(unit(2.5f))
                        .rounded(unit(2))
                        .border(1.5f, isEnabled ? Pal.accent : WebStyles.Colors.BORDER);

                // Show description on hover via tooltip
                cardRow.table().addListener(new Tooltip(t -> {
                    t.background(Styles.black6).margin(6f).add(p.description()).color(Color.lightGray);
                }));

                cardRow.children(() -> {
                    // Left Information Column: Name & Direct Preview
                    column().growX().gap(unit(1)).left().children(() -> {
                        row().growX().gap(unit(1.5f)).left().children(() -> {
                            if (isEnabled) {
                                text("#" + (enabledIndex + 1))
                                        .color(Pal.accent)
                                        .left();
                            }
                            text(p.name())
                                    .color(isEnabled ? Pal.accent : Color.white)
                                    .left();
                        });

                        // Direct transformed preview reacting immediately to preview input
                        Readable<String> previewTransformed = previewSignal.map(input -> {
                            String str = input != null ? input : "";
                            return p.transform(str);
                        });
                        text(previewTransformed)
                                .color(Color.white)
                                .left()
                                .wrap()
                                .growX();
                    });

                    spacer();

                    // Right Action Buttons
                    row().gap(unit(1)).right().children(() -> {
                        if (p.isEditable()) {
                            button(Icon.edit, () -> {
                                new PrettyChatEditDialog(p, () -> {
                                    feature.config().setEnabledIds(feature.config().getEnabledIds());
                                }).show();
                            })
                                    .style(Styles.emptyi)
                                    .size(unit(8))
                                    .tooltip(Core.bundle != null
                                            ? Core.bundle.get("feature.pretty-chat.settings.edit-script", "Edit Script")
                                            : "Edit Script");
                        }

                        if (isEnabled) {
                            if (enabledIndex > 0) {
                                button(Icon.up, () -> feature.config().move(p.id(), -1))
                                        .style(Styles.emptyi)
                                        .size(unit(8))
                                        .tooltip(Core.bundle != null
                                                ? Core.bundle.get("feature.pretty-chat.settings.move-up", "Move Up")
                                                : "Move Up");
                            } else {
                                row().size(unit(8));
                            }

                            if (enabledIndex < enabledCount - 1) {
                                button(Icon.down, () -> feature.config().move(p.id(), 1))
                                        .style(Styles.emptyi)
                                        .size(unit(8))
                                        .tooltip(Core.bundle != null
                                                ? Core.bundle.get("feature.pretty-chat.settings.move-down", "Move Down")
                                                : "Move Down");
                            } else {
                                row().size(unit(8));
                            }
                        }

                        checkbox("", isEnabled, checked -> feature.config().toggle(p.id()))
                                .size(unit(8));
                    });
                });
            }
        });
    }
}
