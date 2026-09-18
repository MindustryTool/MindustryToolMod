package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Scl;
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
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;
import solim.overlay.Hud;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Fully reactive and declarative Team Resource HUD overlay.
 * Uses Solim HUD, reactive bindings for scale, opacity, dimensions, position, and core stats.
 */
public class TeamResourceHudView extends BaseComponent {

    private final TeamResourceFeature feature;
    private final TeamResourceState state;
    private @Nullable Hud hud;

    public TeamResourceHudView(TeamResourceFeature feature, TeamResourceState state) {
        this.feature = feature;
        this.state = state;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> 28f * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> 18f * (s != null ? s : 1f));
        Readable<Boolean> expanded = feature.expandedConfig.signal();

        Signal<Float> screenWidth = createSignal(ResizeEvent.class, TeamResourceHudView::getSceneWidth);

        Readable<Float> hudWidth = new Computed<>(() -> {
            Float sw = screenWidth.get();
            float screenW = sw != null ? sw : getSceneWidth();
            Float s = scale.get();
            float scaleVal = s != null ? s : 1f;
            Float cfgW = feature.overlayWidthConfig.signal().get();
            float userWidth = screenW * (cfgW != null ? cfgW : 0.28f);
            float minWidth = (Vars.mobile ? 180f : 220f) * scaleVal;
            float maxWidth = screenW * 0.98f;
            float widthToUse = Mathf.clamp(userWidth, minWidth, maxWidth);
            return Boolean.TRUE.equals(expanded.get()) ? widthToUse : Math.min(widthToUse, (Vars.mobile ? 180f : 240f) * scaleVal);
        });

        Readable<Integer> itemCols = new Computed<>(() -> {
            Float w = hudWidth.get();
            Float s = scale.get();
            float scaleVal = s != null ? s : 1f;
            float minCardW = 72f * scaleVal;
            return Math.max(2, (int) ((w != null ? w : 220f) / (minCardW > 0f ? minCardW : 72f)));
        });

        Readable<Drawable> bgDrawable = feature.hideBackgroundConfig.signal()
                .map(hide -> Boolean.TRUE.equals(hide) ? null : Styles.black6);

        hud = hud(() -> {
            column().width(hudWidth).left().gap(unit(1)).padding(unit(2)).children(() -> {
                // 1. Header Row
                row().growX().gap(unit(1)).children(() -> {
                    // Drag handle
                    button()
                            .style(Styles.clearNonei)
                            .size(buttonSize)
                            .children(() -> icon(Icon.move).scaling(Scaling.fit))
                            .draggable(hud, feature.xSignal, feature.ySignal);

                    // Expand / Collapse toggle button
                    button()
                            .style(Styles.clearNonei)
                            .size(buttonSize)
                            .onClick(() -> feature.expandedConfig.set(!Boolean.TRUE.equals(feature.expandedConfig.get())))
                            .tooltip(expanded.map(exp -> Core.bundle.get(Boolean.TRUE.equals(exp) ? "team-resources.collapse" : "team-resources.expand", "Toggle Expand")))
                            .children(() -> text(expanded.map(exp -> Boolean.TRUE.equals(exp) ? "▼" : "▶")));

                    // Team selector chips
                    dynamic(state.validTeamsSignal, teams -> row().gap(unit(1)).children(() -> {
                        if (teams != null) {
                            int limit = Math.min(teams.size, 5);
                            for (int i = 0; i < limit; i++) {
                                Team team = teams.get(i);
                                button()
                                        .style(Styles.clearTogglei)
                                        .size(buttonSize)
                                        .onClick(() -> state.setSelectedTeam(team))
                                        .checked(state.selectedTeamSignal.map(sel -> sel == team))
                                        .tooltip(team.localized())
                                        .children(() -> image(Tex.whiteui).size(iconSize).color(team.color));
                            }
                            if (teams.size > 5) {
                                button("...", () -> new TeamResourceAllTeamsDialog(state).show())
                                        .style(Styles.flatBordert)
                                        .size(buttonSize);
                            }
                        }
                    }));

                    spacer();

                    // Settings button
                    button()
                            .style(Styles.clearNonei)
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("team-resources.settings.title", "Settings"))
                            .onClick(() -> feature.getSettingDialog().get().show())
                            .children(() -> icon(Icon.settings).scaling(Scaling.fit));
                });

                // 2. Expanded Content Panel
                dynamic(expanded, isExp -> Boolean.TRUE.equals(isExp) ? buildExpandedContent(scale, itemCols) : row()).growX();
            });
        });

        hud.background(bgDrawable);
        hud.opacity(feature.opacityConfig.signal());
        hud.position(feature.xSignal, feature.ySignal);
        hud.toFrontOnTouch();

        // Screen resize clamping with automatic ownership cleanup
        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        // Initial layout stabilization
        Core.app.post(() -> {
            if (hud != null) {
                hud.root().invalidateHierarchy();
                hud.pack();
                hud.keepInScreen();
                hud.root().toFront();
            }
        });

        // Frame update to poll state
        hud.element().update(() -> {
            if (!hud.element().visible) return;
            state.update();
        });

        return hud.element();
    }

    private Component buildExpandedContent(Readable<Float> scale, Readable<Integer> itemCols) {
        Readable<Float> itemCardHeight = scale.map(s -> 34f * (s != null ? s : 1f));
        Readable<Float> unitCardHeight = scale.map(s -> 28f * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> 18f * (s != null ? s : 1f));

        return column().growX().gap(unit(1)).children(() -> {
            divider();

            // Core Items Section
            dynamic(feature.showItemsConfig.signal(), show -> Boolean.TRUE.equals(show) ? column(() -> {
                dynamic(state.usedItemsSignal, items -> {
                    if (items == null || items.isEmpty()) {
                        return row().left().children(() -> text(Core.bundle.get("team-resources.no-items", "No core items")).color(Color.gray).style(Styles.outlineLabel));
                    }
                    return grid(
                        itemCols,
                        state.usedItemsSignal,
                        item -> item.name,
                        item -> createItemCard(item, itemCardHeight, iconSize, scale)
                    ).growX().gap(unit(1));
                }).growX();
            }).growX() : row()).growX();

            // Units Section
            dynamic(feature.showUnitsConfig.signal(), show -> Boolean.TRUE.equals(show) ? column(() -> {
                dynamic(state.usedUnitsSignal, units -> {
                    if (units == null || units.isEmpty()) {
                        return row().left().children(() -> text(Core.bundle.get("team-resources.no-units", "No active units")).color(Color.gray).style(Styles.outlineLabel));
                    }
                    return grid(
                        itemCols,
                        state.usedUnitsSignal,
                        unit -> unit.name,
                        unit -> createUnitCard(unit, unitCardHeight, iconSize, scale)
                    ).growX().gap(unit(1));
                }).growX();
            }).growX() : row()).growX();

            // Power Section
            dynamic(feature.showPowerConfig.signal(), show -> Boolean.TRUE.equals(show) ? createPowerSection(scale) : row()).growX();
        });
    }

    private Component createItemCard(Item item, Readable<Float> cardHeight, Readable<Float> iconSize, Readable<Float> scale) {
        Card card = card(Styles.black3, () -> {
            row().growX().padding(unit(1)).gap(unit(1)).children(() -> {
                image(new TextureRegionDrawable(item.uiIcon)).size(iconSize).scaling(Scaling.fit);
                column().left().children(() -> {
                    text(state.tickSignal.map(t -> state.getFormattedAmount(item)))
                            .style(Styles.outlineLabel)
                            .fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f)));
                    text(state.tickSignal.map(t -> (Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get()) || state.isViewingStats()) ? state.getFormattedRate(item) : ""))
                            .color(state.tickSignal.map(t -> state.getRateColor(item)))
                            .style(Styles.outlineLabel)
                            .fontScale(scale.map(s -> 0.60f * (s != null ? s : 1f)));
                });
            });
        })
        .margin(scale.map(s -> 2f * (s != null ? s : 1f)))
        .growX()
        .height(cardHeight)
        .onClick(() -> state.setViewingStats(!state.isViewingStats()));

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

    private Component createUnitCard(UnitType type, Readable<Float> cardHeight, Readable<Float> iconSize, Readable<Float> scale) {
        return card(Styles.black3, () -> {
            row().growX().padding(unit(1)).gap(unit(1)).children(() -> {
                image(new TextureRegionDrawable(type.uiIcon)).size(iconSize).scaling(Scaling.fit);
                text(state.tickSignal.map(t -> state.getUnitCountText(type)))
                        .style(Styles.outlineLabel)
                        .fontScale(scale.map(s -> 0.72f * (s != null ? s : 1f)));
            });
        })
        .margin(scale.map(s -> 2f * (s != null ? s : 1f)))
        .growX()
        .height(cardHeight);
    }

    private Component createPowerSection(Readable<Float> scale) {
        return column(() -> {
            divider();

                row().left().growX()
                .marginTop(scale.map(s -> 6f * (s != null ? s : 1f)))
                .marginBottom(scale.map(s -> 3f * (s != null ? s : 1f)))
                .children(() -> {
                    text(state.tickSignal.map(t -> {
                        if (state.getTeamGraphs().isEmpty()) {
                            return Core.bundle.get("team-resources.no-power", "No power network");
                        }
                        return Core.bundle.get("team-resources.power-prefix", "Power: ") + state.getFormattedPowerBalance();
                    }))
                    .color(state.tickSignal.map(t -> state.getTeamGraphs().isEmpty() ? Color.gray : state.getPowerBalanceColor()))
                    .style(Styles.outlineLabel)
                    .fontScale(scale.map(s -> 0.82f * (s != null ? s : 1f)));
                });

            SplitBar satisfactionBar = new SplitBar(state.getTeamGraphs(), SplitBar.Mode.SATISFACTION, () -> scale.get() != null ? scale.get() : 1f);
            row().growX().height(scale.map(s -> 20f * (s != null ? s : 1f)))
                .marginBottom(scale.map(s -> 4f * (s != null ? s : 1f)))
                .children(() -> {
                    arc(satisfactionBar);
                });

            dynamic(feature.showStoredPowerConfig.signal(), show -> Boolean.TRUE.equals(show) ? column(() -> {
                row().left().growX()
                    .marginTop(scale.map(s -> 5f * (s != null ? s : 1f)))
                    .marginBottom(scale.map(s -> 3f * (s != null ? s : 1f)))
                    .children(() -> {
                        text(state.tickSignal.map(t -> Core.bundle.get("team-resources.stored-prefix", "Stored: ") + state.getFormattedStoredPower()))
                                .style(Styles.outlineLabel)
                                .fontScale(scale.map(s -> 0.80f * (s != null ? s : 1f)));
                    });

                SplitBar storedBar = new SplitBar(state.getTeamGraphs(), SplitBar.Mode.STORED, () -> scale.get() != null ? scale.get() : 1f);
                row().growX().height(scale.map(s -> 20f * (s != null ? s : 1f)))
                    .marginBottom(scale.map(s -> 2f * (s != null ? s : 1f)))
                    .children(() -> {
                        arc(storedBar);
                    });
            }).growX().gap(unit(1)) : row()).growX();
        }).growX().gap(unit(1));
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }

    private static float getSceneWidth() {
        if (Core.scene != null && Core.scene.getWidth() > 0f) {
            return Core.scene.getWidth();
        }
        float scl = Scl.scl();
        return (Core.graphics != null ? Core.graphics.getWidth() : 800f) / (scl > 0f ? scl : 1f);
    }
}
