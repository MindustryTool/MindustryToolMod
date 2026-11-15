package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import mindustrytool.ui.dock.MindustryDockRegistry;
import solim.core.Component;
import solim.core.EventsUtil;
import solim.layout.Row;
import solim.overlay.Hud;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class TeamResourceHudView extends Component {

    private final TeamResourceFeature feature;
    private final TeamResourceState state;
    private @Nullable Hud hud;

    public TeamResourceHudView(TeamResourceFeature feature, TeamResourceState state) {
        this.feature = feature;
        this.state = state;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));
        Readable<Boolean> expanded = feature.expandedConfig.signal();

        Signal<Float> screenWidth = EventsUtil.createSignal(this, ResizeEvent.class,
                TeamResourceFeature::getSceneWidth);

        Readable<Float> hudWidth = new Computed<>(() -> {
            Float sw = screenWidth.get();
            float screenW = sw != null ? sw : TeamResourceFeature.getSceneWidth();
            Float s = scale.get();
            float scaleVal = s != null ? s : 1f;
            Float cfgW = feature.overlayWidthConfig.signal().get();
            float userWidth = screenW * (cfgW != null ? cfgW : 0.28f);
            float minWidth = (Vars.mobile ? 160f : 200f) * scaleVal;
            float maxWidth = screenW * 0.95f;
            float widthToUse = Mathf.clamp(userWidth, minWidth, maxWidth);
            return Boolean.TRUE.equals(expanded.get()) ? widthToUse
                    : Math.min(widthToUse, (Vars.mobile ? 180f : 240f) * scaleVal);
        });

        Readable<Integer> itemCols = new Computed<>(() -> {
            Float w = hudWidth.get();
            Float s = scale.get();
            float scaleVal = s != null ? s : 1f;
            float minCardW = 54f * scaleVal;
            return Math.max(3, (int) ((w != null ? w : 200f) / (minCardW > 0f ? minCardW : 54f)));
        });

        Readable<Color> bgColor = feature.hideBackgroundConfig.signal()
                .map(hide -> Boolean.TRUE.equals(hide) ? Color.clear : WebStyles.Colors.SECTION_BG);
        Readable<Color> borderColor = feature.hideBackgroundConfig.signal()
                .map(hide -> Boolean.TRUE.equals(hide) ? Color.clear : WebStyles.Colors.SECTION_BORDER);

        hud = hud();
        hud.children(() -> {
            column().props(maxWidth(hudWidth), padding(unit(1.5f)), left(), gap(unit(1.5f))).children(() -> {

                when(feature.hideHeaderConfig.signal())
                        .elseDo(() -> row().props(growX(), gap(unit(1))).children(() -> {

                            when(feature.hideDragHandleConfig.signal())
                                    .elseDo(() -> button()
                                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize),
                                                    dockable(MindustryDockRegistry.get().engine(), feature.dockPosition, "team-resources"))
                                            .children(() -> icon(Icon.move).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize))));

                            button()
                                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(expanded.map(exp -> Core.bundle.get(
                                            Boolean.TRUE.equals(exp) ? "team-resources.collapse"
                                                    : "team-resources.expand",
                                            "Toggle Expand"))))
                                    .onClick(() -> feature.expandedConfig
                                            .set(!Boolean.TRUE.equals(feature.expandedConfig.get())))
                                    .children(() -> icon(
                                            expanded.map(exp -> Boolean.TRUE.equals(exp) ? Icon.down : Icon.right))
                                            .props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));

                            dynamic(state.validTeamsSignal, teams -> row().props(gap(unit(1))).children(() -> {
                                if (teams != null) {
                                    int limit = Math.min(teams.size, 5);
                                    for (int i = 0; i < limit; i++) {
                                        Team team = teams.get(i);
                                        button()
                                                .props(style(WebStyles.filterChip()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), checked(state.selectedTeamSignal.map(sel -> sel == team)), tooltip(team.localized()))
                                                .onClick(() -> state.setSelectedTeam(team))
                                                .children(() -> image(Tex.whiteui).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(team.color)));
                                    }
                                    if (teams.size > 5) {
                                        button("...", () -> new TeamResourceAllTeamsDialog(state).show())
                                                .props(style(WebStyles.outlineText()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize));
                                    }
                                }
                            }));

                            spacer();

                            button()
                                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("team-resources.settings.title", "Settings")))
                                    .onClick(() -> feature.getSettingDialog().get().show())
                                    .children(() -> icon(Icon.settings).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize)));
                        }));

                Readable<Boolean> showContent = new Computed<>(
                        () -> Boolean.TRUE.equals(feature.hideHeaderConfig.signal().get())
                                || Boolean.TRUE.equals(expanded.get()));

                when(showContent)
                        .thenDo(() -> buildExpandedContent(scale, itemCols))
                        .elseDo(() -> row())
                        .props(growX());
            });
        });

        hud.dockable(null, MindustryDockRegistry.get().engine(), feature.dockPosition, feature.xSignal, feature.ySignal,
                "team-resources");
        hud.props(rounded(unit(2), bgColor), border(1.5f, borderColor), opacity(feature.opacityConfig.signal()), position(feature.xSignal, feature.ySignal));
        hud.toFrontOnTouch();

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            feature.hideHeaderConfig.signal().get();
            Core.app.post(() -> {
                if (hud != null) {
                    hud.keepInScreen();
                }
            });
        });

        Core.app.post(() -> {
            if (hud != null) {
                hud.keepInScreen();
                hud.root().toFront();
            }
        });

        hud.element().update(() -> {
            if (!hud.element().visible)
                return;
            state.update();
        });

        return hud.element();
    }

    private Component buildExpandedContent(Readable<Float> scale, Readable<Integer> itemCols) {
        Readable<Float> itemCardHeight = scale.map(s -> 22f * (s != null ? s : 1f));
        Readable<Float> unitCardHeight = scale.map(s -> 20f * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> 16f * (s != null ? s : 1f));

        return column().props(growX(), gap(unit(1))).children(() -> {
            when(feature.hideHeaderConfig.signal())
                    .elseDo(() -> divider());

            when(feature.showItemsConfig.signal())
                    .thenDo(() -> column().props(growX()).children(() -> {
                        when(state.usedItemsSignal.map(items -> items == null || items.isEmpty()))
                                .thenDo(() -> row().props(left()).children(
                                        () -> text(Core.bundle.get("team-resources.no-items", "No core items"))
                                                .props(color(Color.gray), style(Styles.outlineLabel))))
                                .props(growX());

                        reactiveGrid(state.usedItemsSignal).columns(itemCols)
                                .key(item -> item.name).props(growX(), gap(unit(0.5f)))
                                .items(item -> createItemCard(item, itemCardHeight, iconSize, scale));
                    }))
                    .elseDo(() -> row())
                    .props(growX());

            when(feature.showUnitsConfig.signal())
                    .thenDo(() -> column().props(growX()).children(() -> {
                        when(state.usedUnitsSignal.map(units -> units == null || units.isEmpty()))
                                .thenDo(() -> row().props(left()).children(
                                        () -> text(Core.bundle.get("team-resources.no-units", "No active units"))
                                                .props(color(Color.gray), style(Styles.outlineLabel))))
                                .props(growX());

                        reactiveGrid(state.usedUnitsSignal).columns(itemCols)
                                .key(unit -> unit.name).props(growX(), gap(unit(0.5f)))
                                .items(unit -> createUnitCard(unit, unitCardHeight, iconSize, scale));
                    }))
                    .elseDo(() -> row())
                    .props(growX());

            when(feature.showPowerConfig.signal())
                    .thenDo(() -> createPowerSection(scale))
                    .elseDo(() -> row())
                    .props(growX());
        });
    }

    private Component createItemCard(Item item, Readable<Float> cardHeight, Readable<Float> iconSize,
            Readable<Float> scale) {
        Row itemRow = row()
                .props(padding(unit(0.5f)), left(), height(cardHeight), prefHeight(cardHeight), center(), gap(unit(0.5f)))
                .children(() -> {
                    image(new TextureRegionDrawable(item.uiIcon))
                            .props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), scaling(Scaling.fit));
                    row().props(bottom(), left(), gap(unit(0.5f))).children(() -> {
                        text(state.tickSignal.map(t -> state.getFormattedAmount(item)))
                                .props(style(Styles.outlineLabel), fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f))));
                        text(state.tickSignal.map(t -> state.getFormattedRate(item)))
                                .props(color(state.tickSignal.map(t -> state.getRateColor(item))),
                                        style(Styles.outlineLabel),
                                        fontScale(scale.map(s -> 0.55f * (s != null ? s : 1f))));
                    });
                });

        itemRow.element().addListener(new Tooltip(table -> {
            table.background(Styles.black8).margin(8f);
            table.update(() -> {
                table.clearChildren();
                table.left();

                Table header = new Table();
                header.add(new Image(item.uiIcon)).size(16f).padRight(4f);
                header.add(item.localizedName + ": ").color(Color.lightGray);
                header.add(String.valueOf(state.getItemAmount(item))).color(Color.white);

                String fullRate = state.getFullFormattedRate(item);
                header.add(" (" + fullRate + ")").color(state.getRateColor(item)).padLeft(4f);
                table.add(header).left().row();

                HistoryBuffer history = state.getItemHistory(item);
                if (history != null && history.size() > 1) {
                    table.add(new SparklineWidget(history, Pal.accent)).padTop(4f).row();
                }
            });
        }));

        return itemRow;
    }

    private Component createUnitCard(UnitType type, Readable<Float> cardHeight, Readable<Float> iconSize,
            Readable<Float> scale) {
        Row unitRow = row()
                .props(padding(unit(0.5f)), left(), height(cardHeight), prefHeight(cardHeight), center(), gap(unit(0.5f)))
                .children(() -> {
                    image(new TextureRegionDrawable(type.uiIcon))
                            .props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), scaling(Scaling.fit));
                    text(state.tickSignal.map(t -> state.getUnitCountText(type)))
                            .props(style(Styles.outlineLabel), fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f))));
                });

        unitRow.element().addListener(new Tooltip(table -> {
            table.background(Styles.black8).margin(6f);
            table.update(() -> {
                table.clearChildren();
                table.left();
                table.add(new Image(type.uiIcon)).size(16f).padRight(4f);
                table.add(type.localizedName + ": ").color(Color.lightGray);
                table.add(state.getUnitCountText(type)).color(Color.white);
            });
        }));

        return unitRow;
    }

    private Component createPowerSection(Readable<Float> scale) {
        Readable<Float> barHeight = scale.map(s -> 18f * (s != null ? s : 1f));

        return row()
                .props(growX(), height(barHeight), prefHeight(barHeight), margin(unit(0.5f)))
                .children(() -> new SplitBar(state, () -> scale.peek() != null ? scale.peek() : 1f));
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}