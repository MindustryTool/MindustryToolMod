package mindustrytool.features.prettychat.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.prettychat.Prettier;
import mindustrytool.features.prettychat.PrettyChatFeature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern declarative Solim settings view for Pretty Chat.
 */
public class PrettyChatSettingsView extends BaseComponent {

    private static final String[] CATEGORIES = {"all", "color", "font", "effect", "case", "fun", "custom"};

    private final PrettyChatFeature feature;
    private final Signal<String> previewSignal;
    private final Computed<String> resultComputed;
    private final Signal<String> selectedCategory;
    private final Computed<Seq<Prettier>> filteredPrettiers;

    public PrettyChatSettingsView(PrettyChatFeature feature) {
        this.feature = feature;
        String initialSample = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-sample", "Hello World! This is a test message.")
                : "Hello World! This is a test message.";
        this.previewSignal = Signal.of(initialSample);
        this.selectedCategory = Signal.of("all");
        this.resultComputed = new Computed<>(() -> {
            String input = previewSignal.get();
            List<String> enabled = feature.config().enabledIdsSignal.get();
            return !enabled.isEmpty() && input != null && !input.isEmpty()
                    ? feature.transform(input)
                    : (input != null ? input : "");
        });
        this.filteredPrettiers = new Computed<>(() -> {
            String cat = selectedCategory.get();
            Seq<Prettier> all = feature.prettiers();
            Seq<Prettier> list = new Seq<>();
            for (int i = 0; i < all.size; i++) {
                Prettier p = all.get(i);
                if ("all".equals(cat) || cat.equalsIgnoreCase(p.category())) {
                    list.add(p);
                }
            }
            return list;
        });
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2f)).padding(unit(2)).children(() -> {
                    livePreviewCard();
                    pipelineCard();
                    availableTransformersPanel();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(8, WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component livePreviewCard() {
        String previewLabel = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-label", "Live Message Preview")
                : "Live Message Preview";
        String liveBadge = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.live-badge", "LIVE")
                : "LIVE";
        String previewDesc = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-desc",
                        "Type a message below to preview your active transformer pipeline.")
                : "Type a message below to preview your active transformer pipeline.";
        String previewPlaceholder = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.preview-placeholder", "Type a message to preview...")
                : "Type a message to preview...";
        String simSender = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.sim-sender", "[accent][You]:[]")
                : "[accent][You]:[]";
        String escapeHint = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.escape-hint",
                        "Tip: In game chat, type // before your message to send raw text without styling.")
                : "Tip: In game chat, type // before your message to send raw text without styling.";

        return sectionCard().children(() -> {
            // Header
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.chat).size(unit(5)).color(Pal.accent);
                text(previewLabel)
                        .color(Pal.accent)
                        .wrap()
                        .growX()
                        .left();
                badge(liveBadge).color(WebStyles.Colors.PRIMARY);
            });

            text(previewDesc)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();

            // Preview Input Row
            row().growX().height(unit(10)).border(1.5f, WebStyles.Colors.BORDER)
                    .paddingX(unit(2)).rounded(unit(2)).children(() -> {
                textField(previewSignal)
                        .placeholder(previewPlaceholder)
                        .grow()
                        .style(WebStyles.clearInput());
            });

            // Simulated Chat Box
            column().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1)).left().children(() -> {
                row().growX().center().children(() -> {
                    text(simSender).left().wrap();
                    spacer();
                    text(resultComputed.map(r -> r.length() + "/150"))
                            .color(resultComputed.map(r -> r.length() > 150 ? Color.scarlet
                                    : (r.length() > 130 ? Color.gold : Color.lightGray)))
                            .right();
                });

                text(resultComputed)
                        .wrap()
                        .growX()
                        .left();
            });

            // Escape prefix hint
            text(escapeHint)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component pipelineCard() {
        String pipelineTitle = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.pipeline-title", "Active Pipeline")
                : "Active Pipeline";
        String pipelineDesc = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.pipeline-desc",
                        "Enabled transformers run sequentially in the order shown below.")
                : "Enabled transformers run sequentially in the order shown below.";
        String clearText = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.clear-pipeline", "Clear All")
                : "Clear All";
        String resetText = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.reset-default", "Reset to Default")
                : "Reset to Default";

        return sectionCard().children(() -> {
            // Header
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.tree).size(unit(5)).color(Pal.accent);
                text(pipelineTitle)
                        .color(Pal.accent)
                        .wrap()
                        .growX()
                        .left();
                badge(feature.config().enabledIdsSignal.map(l -> Core.bundle != null
                        ? Core.bundle.format("feature.pretty-chat.settings.pipeline-steps", l.size())
                        : l.size() + " steps"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            text(pipelineDesc)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();

            // Dynamic Active Pipeline List
            dynamic(feature.config().enabledIdsSignal, this::buildActivePipelineList).growX();

            // Quick actions
            row().growX().gap(unit(2)).children(() -> {
                button(clearText, () -> feature.config().setEnabledIds(new ArrayList<>()))
                        .style(WebStyles.outlineText())
                        .growX();

                button(resetText, () -> {
                    List<String> def = new ArrayList<>();
                    def.add("rainbow");
                    feature.config().setEnabledIds(def);
                })
                        .style(WebStyles.outlineText())
                        .growX();
            });
        });
    }

    private void buildActivePipelineList(List<String> enabledIds) {
        if (enabledIds.isEmpty()) {
            String emptyMessage = Core.bundle != null
                    ? Core.bundle.get("feature.pretty-chat.settings.no-transformers",
                            "No active transformers. Enable any transformer from the list.")
                    : "No active transformers. Enable any transformer from the list.";

            column().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).children(() -> {
                text(emptyMessage)
                        .color(WebStyles.Colors.GHOST_FG)
                        .center()
                        .wrap()
                        .growX();
            });
            return;
        }

        column().growX().gap(unit(1.5f)).children(() -> {
            for (int i = 0; i < enabledIds.size(); i++) {
                int index = i;
                String id = enabledIds.get(i);
                Prettier p = feature.getPrettier(id);
                if (p == null) {
                    continue;
                }

                row().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1.5f)).center().children(() -> {
                    text("#" + (index + 1))
                            .color(Pal.accent);

                    text(p.name())
                            .color(Color.white)
                            .wrap()
                            .growX()
                            .left();

                    if (index > 0) {
                        button(() -> feature.config().move(id, -1))
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle != null
                                        ? Core.bundle.get("feature.pretty-chat.settings.move-up", "Move Up")
                                        : "Move Up")
                                .children(() -> icon(Icon.up).size(unit(6)));
                    } else {
                        row().size(unit(11));
                    }

                    if (index < enabledIds.size() - 1) {
                        button(() -> feature.config().move(id, 1))
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle != null
                                        ? Core.bundle.get("feature.pretty-chat.settings.move-down", "Move Down")
                                        : "Move Down")
                                .children(() -> icon(Icon.down).size(unit(6)));
                    } else {
                        row().size(unit(11));
                    }

                    button(() -> feature.config().toggle(id))
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle != null
                                    ? Core.bundle.get("feature.pretty-chat.settings.remove", "Remove")
                                    : "Remove")
                            .children(() -> icon(Icon.cancel).size(unit(6)).color(WebStyles.Colors.DANGER));
                });
            }
        });
    }

    private Component availableTransformersPanel() {
        String availableTitle = Core.bundle != null
                ? Core.bundle.get("feature.pretty-chat.settings.available-transformers", "Available Transformers")
                : "Available Transformers";

        return sectionCard().children(() -> {
            // Header
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(availableTitle)
                        .color(Pal.accent)
                        .wrap()
                        .growX()
                        .left();
                badge(selectedCategory.map(this::getCategoryName))
                        .color(WebStyles.Colors.PRIMARY);
            });

            // Category Filter Chips
            wrap().growX().gap(unit(1)).children(() -> {
                for (String cat : CATEGORIES) {
                    String label = getCategoryName(cat);
                    button(label, () -> selectedCategory.set(cat))
                            .style(WebStyles.filterChipText())
                            .checked(selectedCategory.map(curr -> curr.equals(cat)));
                }
            });

            divider();

            reactiveGrid(filteredPrettiers)
                    .columns(1)
                    .key(Prettier::id)
                    .gap(unit(2))
                    .children(this::prettierCard);
        });
    }

    private Component prettierCard(Prettier p) {
        Readable<Boolean> isEnabled = feature.config().enabledIdsSignal.map(ids -> ids != null && ids.contains(p.id()));
        Readable<Integer> enabledIndex = feature.config().enabledIdsSignal.map(ids -> ids != null ? ids.indexOf(p.id()) : -1);

        return row()
                .growX()
                .padding(unit(2.5f))
                .rounded(6)
                .border(1.5f, isEnabled.map(e -> Boolean.TRUE.equals(e) ? Pal.accent : WebStyles.Colors.BORDER))
                .background(WebStyles.Colors.SECONDARY)
                .tooltip(t -> t.background(Styles.black6).margin(6f).add(p.description()).color(Color.lightGray))
                .children(() -> {
            // Left Information Column: Name, Category tag, Description, Live preview
            column().growX().gap(unit(1)).left().children(() -> {
                wrap().growX().gap(unit(1.5f)).left().children(() -> {
                    text(enabledIndex.map(idx -> idx != null && idx >= 0 ? "#" + (idx + 1) : ""))
                            .visible(isEnabled)
                            .color(Pal.accent)
                            .left();

                    text(p.name())
                            .color(isEnabled.map(e -> Boolean.TRUE.equals(e) ? Pal.accent : Color.white))
                            .wrap()
                            .left();

                    text("[" + getCategoryName(p.category()) + "]")
                            .color(WebStyles.Colors.GHOST_FG)
                            .wrap()
                            .left();
                });

                text(p.description())
                        .color(Color.lightGray)
                        .wrap()
                        .growX()
                        .left();

                // Live sample preview
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

            // Right Action Controls
            row().gap(unit(1)).right().center().children(() -> {
                if (p.isEditable()) {
                    button(() -> {
                        new PrettyChatEditDialog(p, () -> {
                            feature.config().setEnabledIds(feature.config().getEnabledIds());
                        }).show();
                    })
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle != null
                                    ? Core.bundle.get("feature.pretty-chat.settings.edit-script", "Edit Script")
                                    : "Edit Script")
                            .children(() -> icon(Icon.edit).size(unit(6)));
                }

                button(() -> feature.config().toggle(p.id()))
                        .style(WebStyles.filterChip())
                        .checked(isEnabled)
                        .size(unit(11))
                        .tooltip(isEnabled.map(e -> Boolean.TRUE.equals(e)
                                ? (Core.bundle != null ? Core.bundle.get("feature.pretty-chat.settings.remove", "Remove") : "Remove")
                                : (Core.bundle != null ? Core.bundle.get("feature.pretty-chat.settings.enable", "Enable") : "Enable")))
                        .children(() -> icon(isEnabled.map(e -> Boolean.TRUE.equals(e) ? (Drawable) Icon.ok : (Drawable) Icon.add)).size(unit(6)));
            });
        });
    }

    private String getCategoryName(String cat) {
        String key = "pretty-chat.category." + cat.toLowerCase(Locale.ROOT);
        return Core.bundle != null && Core.bundle.has(key)
                ? Core.bundle.get(key)
                : cat.substring(0, 1).toUpperCase(Locale.ROOT) + cat.substring(1);
    }
}
