package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.core.EventsUtil;
import solim.layout.Card;
import solim.overlay.Hud;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Fully reactive and declarative Team Resource HUD overlay. Uses Solim HUD,
 * reactive bindings for scale, opacity, dimensions, position, and core stats.
 */
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
            boolean alwaysFlow = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.signal().get());
            float minCardW = (alwaysFlow ? 58f : 50f) * scaleVal;
            return Math.max(3, (int) ((w != null ? w : 200f) / (minCardW > 0f ? minCardW : 50f)));
        });

        Readable<Color> bgColor = feature.hideBackgroundConfig.signal()
                .map(hide -> Boolean.TRUE.equals(hide) ? Color.clear : WebStyles.Colors.SECTION_BG);
        Readable<Color> borderColor = feature.hideBackgroundConfig.signal()
                .map(hide -> Boolean.TRUE.equals(hide) ? Color.clear : WebStyles.Colors.SECTION_BORDER);

        hud = hud();
        hud.children(() -> {
            column().maxWidth(hudWidth).left().gap(unit(1.5f)).padding(unit(1.5f)).children(() -> {

                when(feature.hideHeaderConfig.signal())
                        .elseDo(() -> row().growX().gap(unit(1)).children(() -> {

                            when(feature.hideDragHandleConfig.signal())
                                    .elseDo(() -> button()
                                            .style(WebStyles.ghost())
                                            .size(buttonSize)
                                            .children(() -> icon(Icon.move).size(iconSize))
                                            .draggable(hud, feature.xSignal, feature.ySignal));

                            button()
                                    .style(WebStyles.ghost())
                                    .size(buttonSize)
                                    .onClick(() -> feature.expandedConfig
                                            .set(!Boolean.TRUE.equals(feature.expandedConfig.get())))
                                    .tooltip(expanded.map(exp -> Core.bundle.get(
                                            Boolean.TRUE.equals(exp) ? "team-resources.collapse"
                                                    : "team-resources.expand",
                                            "Toggle Expand")))
                                    .children(() -> icon(
                                            expanded.map(exp -> Boolean.TRUE.equals(exp) ? Icon.down : Icon.right))
                                            .size(iconSize));

                            dynamic(state.validTeamsSignal, teams -> row().gap(unit(1)).children(() -> {
                                if (teams != null) {
                                    int limit = Math.min(teams.size, 5);
                                    for (int i = 0; i < limit; i++) {
                                        Team team = teams.get(i);
                                        button()
                                                .style(WebStyles.filterChip())
                                                .size(buttonSize)
                                                .onClick(() -> state.setSelectedTeam(team))
                                                .checked(state.selectedTeamSignal.map(sel -> sel == team))
                                                .tooltip(team.localized())
                                                .children(() -> image(Tex.whiteui).size(iconSize).color(team.color));
                                    }
                                    if (teams.size > 5) {
                                        button("...", () -> new TeamResourceAllTeamsDialog(state).show())
                                                .style(WebStyles.outlineText())
                                                .size(buttonSize);
                                    }
                                }
                            }));

                            spacer();

                            button()
                                    .style(WebStyles.ghost())
                                    .size(buttonSize)
                                    .tooltip(Core.bundle.get("team-resources.settings.title", "Settings"))
                                    .onClick(() -> feature.getSettingDialog().get().show())
                                    .children(() -> icon(Icon.settings).size(iconSize));
                        }));

                Readable<Boolean> showContent = new Computed<>(
                        () -> Boolean.TRUE.equals(feature.hideHeaderConfig.signal().get())
                                || Boolean.TRUE.equals(expanded.get()));

                when(showContent)
                        .thenDo(() -> buildExpandedContent(scale, itemCols))
                        .elseDo(() -> row())
                        .growX();
            });
        });

        hud.rounded(unit(2), bgColor);
        hud.border(1.5f, borderColor);
        hud.opacity(feature.opacityConfig.signal());
        hud.position(feature.xSignal, feature.ySignal);
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
        Readable<Float> itemCardHeight = new Computed<>(() -> {
            boolean flow = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.signal().get());
            Float s = scale.get();
            return (flow ? 32f : 24f) * (s != null ? s : 1f);
        });
        Readable<Float> unitCardHeight = scale.map(s -> 22f * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> 16f * (s != null ? s : 1f));

        return column().growX().gap(unit(1)).children(() -> {
            when(feature.hideHeaderConfig.signal())
                    .elseDo(() -> divider());

            when(feature.showItemsConfig.signal())
                    .thenDo(() -> column().growX().children(() -> {
                        when(state.usedItemsSignal.map(items -> items == null || items.isEmpty()))
                                .thenDo(() -> row().left().children(
                                        () -> text(Core.bundle.get("team-resources.no-items", "No core items"))
                                                .color(Color.gray).style(Styles.outlineLabel)))
                                .growX();

                        reactiveGrid(state.usedItemsSignal).columns(itemCols)
                                .key(item -> item.name).growX().gap(unit(1))
                                .children(item -> createItemCard(item, itemCardHeight, iconSize, scale));
                    }))
                    .elseDo(() -> row())
                    .growX();

            when(feature.showUnitsConfig.signal())
                    .thenDo(() -> column().growX().children(() -> {
                        when(state.usedUnitsSignal.map(units -> units == null || units.isEmpty()))
                                .thenDo(() -> row().left().children(
                                        () -> text(Core.bundle.get("team-resources.no-units", "No active units"))
                                                .color(Color.gray).style(Styles.outlineLabel)))
                                .growX();

                        reactiveGrid(state.usedUnitsSignal).columns(itemCols)
                                .key(unit -> unit.name).growX().gap(unit(1))
                                .children(unit -> createUnitCard(unit, unitCardHeight, iconSize, scale));
                    }))
                    .elseDo(() -> row())
                    .growX();

            when(feature.showPowerConfig.signal())
                    .thenDo(() -> createPowerSection(scale))
                    .elseDo(() -> row())
                    .growX();
        });
    }

    private Component createItemCard(Item item, Readable<Float> cardHeight, Readable<Float> iconSize,
            Readable<Float> scale) {
        Card card = card()
                .padding(unit(1f))
                .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                .border(1f, WebStyles.Colors.BORDER)
                .growX()
                .height(cardHeight)
                .margin(scale.map(s -> 2f * (s != null ? s : 1f)))
                .onClick(() -> state.setViewingStats(!state.isViewingStats()))
                .children(() -> {
                    row().growX().gap(unit(1)).children(() -> {
                        image(new TextureRegionDrawable(item.uiIcon)).size(iconSize).scaling(Scaling.fit);
                        column().left().children(() -> {
                            text(state.tickSignal.map(t -> state.getFormattedAmount(item)))
                                    .style(Styles.outlineLabel)
                                    .fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f)));
                            Readable<String> rateText = new Computed<>(() -> {
                                state.tickSignal.get();
                                boolean show = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.signal().get())
                                        || state.isViewingStats();
                                return show ? state.getFormattedRate(item) : "";
                            });
                            text(rateText)
                                    .color(state.tickSignal.map(t -> state.getRateColor(item)))
                                    .style(Styles.outlineLabel)
                                    .fontScale(scale.map(s -> 0.60f * (s != null ? s : 1f)));
                        });
                    });
                });

        card.element().addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, @Nullable Element fromActor) {
                if (pointer == -1) {
                    state.setViewingStats(true);
                    state.clearSnapshot();
                }
                super.enter(event, x, y, pointer, fromActor);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, @Nullable Element toActor) {
                if (pointer == -1) {
                    state.setViewingStats(false);
                    state.clearSnapshot();
                }
                super.exit(event, x, y, pointer, toActor);
            }
        });

        return card;
    }

    private Component createUnitCard(UnitType type, Readable<Float> cardHeight, Readable<Float> iconSize,
            Readable<Float> scale) {
        return card()
                .padding(unit(1f))
                .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                .border(1f, WebStyles.Colors.BORDER)
                .growX()
                .height(cardHeight)
                .margin(scale.map(s -> 2f * (s != null ? s : 1f)))
                .children(() -> {
                    row().growX().gap(unit(1)).children(() -> {
                        image(new TextureRegionDrawable(type.uiIcon)).size(iconSize).scaling(Scaling.fit);
                        text(state.tickSignal.map(t -> state.getUnitCountText(type)))
                                .style(Styles.outlineLabel)
                                .fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f)));
                    });
                });
    }

    private Component createPowerSection(Readable<Float> scale) {
        Readable<Float> iconSize = scale.map(s -> 14f * (s != null ? s : 1f));

        return card()
                .padding(unit(1.5f))
                .gap(unit(1))
                .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                .border(1f, WebStyles.Colors.BORDER)
                .growX()
                .children(() -> {
                    row().left().growX().gap(unit(1)).center().children(() -> {
                        icon(Icon.power).size(iconSize).color(state.tickSignal
                                .map(t -> state.getTeamGraphs().isEmpty() ? Color.gray : state.getPowerBalanceColor()));
                        text(state.tickSignal.map(t -> {
                            if (state.getTeamGraphs().isEmpty()) {
                                return Core.bundle.get("team-resources.no-power", "No power network");
                            }
                            return Core.bundle.get("team-resources.power-prefix", "Power: ")
                                    + state.getFormattedPowerBalance();
                        }))
                                .color(state.tickSignal.map(t -> state.getTeamGraphs().isEmpty() ? Color.gray
                                        : state.getPowerBalanceColor()))
                                .style(Styles.outlineLabel)
                                .fontScale(scale.map(s -> 0.82f * (s != null ? s : 1f)));
                    });

                    SplitBar satisfactionBar = new SplitBar(state.getTeamGraphs(), SplitBar.Mode.SATISFACTION,
                            () -> scale.peek() != null ? scale.peek() : 1f);
                    row().growX().height(scale.map(s -> 20f * (s != null ? s : 1f)))
                            .children(() -> arc(satisfactionBar));

                    when(feature.showStoredPowerConfig.signal())
                            .thenDo(() -> column(() -> {
                                row().left().growX().gap(unit(1)).center().children(() -> {
                                    icon(Icon.powerSmall).size(iconSize);
                                    text(state.tickSignal
                                            .map(t -> Core.bundle.get("team-resources.stored-prefix", "Stored: ")
                                                    + state.getFormattedStoredPower()))
                                            .style(Styles.outlineLabel)
                                            .fontScale(scale.map(s -> 0.80f * (s != null ? s : 1f)));
                                });

                                SplitBar storedBar = new SplitBar(state.getTeamGraphs(), SplitBar.Mode.STORED,
                                        () -> scale.peek() != null ? scale.peek() : 1f);
                                row().growX().height(scale.map(s -> 20f * (s != null ? s : 1f)))
                                        .children(() -> arc(storedBar));
                            }).growX().gap(unit(1)))
                            .elseDo(() -> row())
                            .growX();
                });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
