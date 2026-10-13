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
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import mindustrytool.features.prettychat.Prettier;
import mindustrytool.features.prettychat.PrettyChatFeature;
import solim.core.Component;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern declarative Solim settings view for Pretty Chat.
 */
public class PrettyChatSettingsView extends Component {

    private static final String[] CATEGORIES = { "all", "color", "font", "effect", "case", "fun", "custom" };

    private final PrettyChatFeature feature;
    private final Signal<String> previewSignal;
    private final Signal<String> selectedCategory;

    public PrettyChatSettingsView(PrettyChatFeature feature) {
        this.feature = feature;
        String initialSample = Core.bundle.get("feature.pretty-chat.settings.preview-sample");
        this.previewSignal = Signal.of(initialSample);
        this.selectedCategory = Signal.of("all");
    }

    @Override
    protected Element build() {

        Computed<String> resultComputed = new Computed<>(() -> {
            String input = previewSignal.get();
            List<String> enabled = feature.config().enabledIdsSignal.get();
            return !enabled.isEmpty() && input != null && !input.isEmpty()
                    ? feature.transform(input)
                    : (input != null ? input : "");
        });
        Computed<Seq<Prettier>> filteredPrettiers = new Computed<>(() -> {
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

        return SettingScaffold.build(() -> {
            headerCard();
            livePreviewCard(resultComputed);
            pipelineCard();
            availableTransformersPanel(filteredPrettiers);
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "COMMUNICATION");
    }

    private Component livePreviewCard(Computed<String> resultComputed) {
        String previewLabel = Core.bundle.get("feature.pretty-chat.settings.preview-label");
        String previewDesc = Core.bundle.get("feature.pretty-chat.settings.preview-desc");
        String previewPlaceholder = Core.bundle.get("feature.pretty-chat.settings.preview-placeholder");
        String simSender = Core.bundle.get("feature.pretty-chat.settings.sim-sender");
        String escapeHint = Core.bundle.get("feature.pretty-chat.settings.escape-hint");

        return SettingCard.section(Icon.chat, previewLabel, () -> {
            text(previewDesc)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();

            row().growX().height(unit(10)).border(1.5f, WebStyles.Colors.BORDER)
                    .paddingX(unit(2)).rounded(unit(2)).children(() -> {
                        textField(previewSignal)
                                .placeholder(previewPlaceholder)
                                .grow()
                                .style(WebStyles.clearInput());
                    });

            column().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1)).left()
                    .children(() -> {
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

            text(escapeHint)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component pipelineCard() {
        String pipelineTitle = Core.bundle.get("feature.pretty-chat.settings.pipeline-title");
        String pipelineDesc = Core.bundle.get("feature.pretty-chat.settings.pipeline-desc");
        String clearText = Core.bundle.get("feature.pretty-chat.settings.clear-pipeline");
        String resetText = Core.bundle.get("feature.pretty-chat.settings.reset-default");

        return SettingCard.section(Icon.tree, pipelineTitle, () -> {
            text(pipelineDesc)
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();

            dynamic(feature.config().enabledIdsSignal, this::buildActivePipelineList).growX();

            row().growX().gap(unit(2)).children(() -> {
                button(clearText, () -> feature.config().setEnabledIds(new ArrayList<>()))
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();

                button(resetText, () -> {
                    List<String> def = new ArrayList<>();
                    def.add("rainbow");
                    feature.config().setEnabledIds(def);
                })
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();
            });
        });
    }

    private void buildActivePipelineList(List<String> enabledIds) {
        if (enabledIds.isEmpty()) {
            String emptyMessage = Core.bundle.get("feature.pretty-chat.settings.no-transformers");

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

                row().growX().padding(unit(2)).rounded(6, WebStyles.Colors.SECONDARY).gap(unit(1.5f)).center()
                        .children(() -> {
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
                                        .tooltip(Core.bundle.get("feature.pretty-chat.settings.move-up"))
                                        .children(() -> icon(Icon.up).size(unit(6)));
                            } else {
                                row().size(unit(11));
                            }

                            if (index < enabledIds.size() - 1) {
                                button(() -> feature.config().move(id, 1))
                                        .style(WebStyles.ghost())
                                        .size(unit(11))
                                        .tooltip(Core.bundle.get("feature.pretty-chat.settings.move-down"))
                                        .children(() -> icon(Icon.down).size(unit(6)));
                            } else {
                                row().size(unit(11));
                            }

                            button(() -> feature.config().toggle(id))
                                    .style(WebStyles.ghost())
                                    .size(unit(11))
                                    .tooltip(Core.bundle.get("feature.pretty-chat.settings.remove"))
                                    .children(() -> icon(Icon.cancel).size(unit(6)).color(WebStyles.Colors.DANGER));
                        });
            }
        });
    }

    private Component availableTransformersPanel(Computed<Seq<Prettier>> filteredPrettiers) {
        String availableTitle = Core.bundle.get("feature.pretty-chat.settings.available-transformers");

        return SettingCard.section(Icon.settings, availableTitle, () -> {

            wrap().growX().gap(unit(1)).children(() -> {
                for (String cat : CATEGORIES) {
                    String label = getCategoryName(cat);
                    button(label, () -> selectedCategory.set(cat))
                            .style(WebStyles.filterChipText())
                            .checked(selectedCategory.map(curr -> curr.equals(cat)));
                }
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            reactiveGrid(filteredPrettiers)
                    .columns(1)
                    .key(Prettier::id)
                    .gap(unit(2))
                    .children(this::prettierCard);
        });
    }

    private Component prettierCard(Prettier p) {
        Readable<Boolean> isEnabled = feature.config().enabledIdsSignal.map(ids -> ids != null && ids.contains(p.id()));
        Readable<Integer> enabledIndex = feature.config().enabledIdsSignal
                .map(ids -> ids != null ? ids.indexOf(p.id()) : -1);

        return row()
                .growX()
                .padding(unit(2.5f))
                .rounded(6)
                .border(1.5f, isEnabled.map(e -> Boolean.TRUE.equals(e) ? Pal.accent : WebStyles.Colors.BORDER))
                .background(WebStyles.Colors.SECONDARY)
                .tooltip(t -> t.background(Styles.black6).margin(6f).add(p.description()).color(Color.lightGray))
                .children(() -> {

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

                    row().gap(unit(1)).right().center().children(() -> {
                        if (p.isEditable()) {
                            button(() -> {
                                new PrettyChatEditDialog(p, () -> {
                                    feature.config().setEnabledIds(feature.config().getEnabledIds());
                                }).show();
                            })
                                    .style(WebStyles.ghost())
                                    .size(unit(11))
                                    .tooltip(Core.bundle.get("feature.pretty-chat.settings.edit-script"))
                                    .children(() -> icon(Icon.edit).size(unit(6)));
                        }

                        button(() -> feature.config().toggle(p.id()))
                                .style(WebStyles.filterChip())
                                .checked(isEnabled)
                                .size(unit(11))
                                .tooltip(isEnabled.map(e -> Boolean.TRUE.equals(e)
                                        ? (Core.bundle.get("feature.pretty-chat.settings.remove"))
                                        : (Core.bundle.get("feature.pretty-chat.settings.enable"))))
                                .children(() -> icon(isEnabled
                                        .map(e -> Boolean.TRUE.equals(e) ? (Drawable) Icon.ok : (Drawable) Icon.add))
                                        .size(unit(6)));
                    });
                });
    }

    private String getCategoryName(String cat) {
        String key = "pretty-chat.category." + cat.toLowerCase(Locale.ROOT);
        return Core.bundle.has(key)
                ? Core.bundle.get(key)
                : cat.substring(0, 1).toUpperCase(Locale.ROOT) + cat.substring(1);
    }
}
