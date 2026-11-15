package mindustrytool.features.resourcemonitor;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.actions.TemporalAction;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustrytool.features.resourcemonitor.SplitBar.Mode;
import mindustrytool.features.settings.ModSettings;
import solim.graphics.BlurHelper;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signals;

public class ResourceMonitorHudView extends Table {

    private final ResourceMonitorFeature feature;
    private final ResourceMonitorState state;
    private final Readable<Float> effectiveScale;
    private final List<Effect> effects = new ArrayList<>();
    private final Cons<ResizeEvent> resizeListener;
    private final StatsHoverListener statsListener = new StatsHoverListener();
    private final ObjectSet<Item> animatedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> animatedUnits = new ObjectSet<>();

    private boolean initializing = true;
    private boolean disposed = false;
    private boolean dragging = false;
    private boolean collapseAnimating = false;
    private boolean lastGraphsEmpty = true;
    private @Nullable StatsCollapser statsCollapser;
    private @Nullable ImageButton collapseButton;

    public ResourceMonitorHudView(ResourceMonitorFeature feature, ResourceMonitorState state) {
        this.feature = feature;
        this.state = state;
        this.effectiveScale = ModSettings.effectiveScale(feature.scaleConfig.signal());

        this.name = "resource-monitor-hud";
        this.touchable = Touchable.enabled;
        top().left();

        setPosition(feature.x(), feature.y());

        resizeListener = e -> {
            if (!feature.isEnabled() || disposed) {
                return;
            }
            rebuild();
            keepInScreen();
            Core.app.post(this::keepInScreen);
        };
        Events.on(ResizeEvent.class, resizeListener);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                toFront();
                return false;
            }
        });

        registerEffects();
        initializing = false;
        rebuild();

        Core.app.post(() -> {
            if (!disposed && feature.isEnabled()) {
                keepInScreen();
                toFront();
            }
        });
    }

    private void registerEffects() {
        effects.add(Effect.of(Signals.isPortrait(), p -> {
            if (!initializing) {
                Core.app.post(this::keepInScreen);
            }
        }));
        effects.add(Effect.of(feature.xSignal, val -> {
            if (!dragging && val != null) {
                setPosition(val, this.y);
            }
        }));
        effects.add(Effect.of(feature.ySignal, val -> {
            if (!dragging && val != null) {
                setPosition(this.x, val);
            }
        }));
        effects.add(Effect.of(feature.opacityConfig.signal(), val -> {
            color.a = val != null ? val : 1f;
        }));
        effects.add(Effect.of(effectiveScale, s -> rebuild()));
        effects.add(Effect.of(feature.overlayWidthConfig.signal(), w -> rebuild()));
        effects.add(Effect.of(feature.overlayHeightConfig.signal(), h -> rebuild()));
        effects.add(Effect.of(feature.limitHeightConfig.signal(), l -> rebuild()));
        effects.add(Effect.of(feature.rowSpacingConfig.signal(), r -> rebuild()));
        effects.add(Effect.of(feature.autoWidthConfig.signal(), a -> rebuild()));
        effects.add(Effect.of(feature.columnsConfig.signal(), c -> rebuild()));
        effects.add(Effect.of(feature.unitColumnsConfig.signal(), uc -> rebuild()));
        effects.add(Effect.of(feature.showItemsConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.showUnitsConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.compactUnitsConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.showPowerConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.showStoredPowerConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.hideBackgroundConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.alwaysShowFlowRateConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.hideZeroUnitsConfig.signal(), v -> {
            if (!initializing) {
                state.refreshZeroFilters();
            }
            rebuild();
        }));
        effects.add(Effect.of(feature.hideZeroItemsConfig.signal(), v -> {
            if (!initializing) {
                state.refreshZeroFilters();
            }
            rebuild();
        }));
        effects.add(Effect.of(feature.expandedConfig.signal(), v -> {
            boolean expanded = Boolean.TRUE.equals(v);
            boolean hideHeader = Boolean.TRUE.equals(feature.hideHeaderConfig.get());
            if (!hideHeader && statsCollapser != null && statsCollapser.parent != null) {
                statsCollapser.setCollapsed(!expanded);
                animateCollapseButton(expanded);
            } else {
                rebuild();
            }
        }));
        effects.add(Effect.of(feature.hideDragHandleConfig.signal(), v -> rebuild()));
        effects.add(Effect.of(feature.hideHeaderConfig.signal(), v -> {
            rebuild();
            if (!initializing) {
                Core.app.post(this::keepInScreen);
            }
        }));
        effects.add(Effect.of(state.selectedTeamSignal, t -> {
            animatedItems.clear();
            animatedUnits.clear();
            rebuild();
        }));
        effects.add(Effect.of(state.validTeamsSignal, t -> rebuild()));
        effects.add(Effect.of(state.usedItemsSignal, t -> rebuild()));
        effects.add(Effect.of(state.usedUnitsSignal, t -> rebuild()));
        effects.add(Effect.of(state.teamGraphsSignal, g -> {
            if (state.getTeamGraphs().isEmpty() != lastGraphsEmpty) {
                rebuild();
            }
        }));
        effects.add(Effect.of(state.tickSignal, tick -> {
            if (state.getTeamGraphs().isEmpty() != lastGraphsEmpty) {
                rebuild();
            }
        }));
    }

    public Element element() {
        return this;
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Effect effect : effects) {
            effect.dispose();
        }
        effects.clear();
        Events.remove(ResizeEvent.class, resizeListener);
        clearChildren();
        remove();
    }

    @Override
    public void act(float delta) {
        float prevHeight = getHeight();
        float prevTopY = prevHeight > 0f ? (this.y + prevHeight) : -1f;

        super.act(delta);
        if (!visible || disposed) {
            return;
        }
        state.update();

        if (statsCollapser != null) {
            float prefH = getPrefHeight();
            if (Math.abs(prefH - prevHeight) > 0.1f) {
                setSize(getPrefWidth(), prefH);
                validate();
                if (!dragging && prevTopY > 0f) {
                    float sh = ResourceMonitorFeature.getSceneHeight();
                    float targetY = prevTopY - prefH;
                    if (sh >= 64f) {
                        targetY = Mathf.clamp(targetY, 0f, Math.max(0f, sh - prefH));
                    }
                    setPosition(this.x, targetY);
                    if (statsCollapser.isActionRunning()) {
                        collapseAnimating = true;
                    } else {
                        collapseAnimating = false;
                        feature.y(targetY);
                    }
                }
            } else if (collapseAnimating && !statsCollapser.isActionRunning()) {
                collapseAnimating = false;
                if (!dragging) {
                    feature.y(this.y);
                }
            }
        }
    }

    public float getEffectiveScale() {
        Float s = effectiveScale.peek();
        return s != null ? s : 1f;
    }

    private void animateCollapseButton(boolean expanded) {
        ImageButton btn = collapseButton;
        if (btn == null) {
            return;
        }
        btn.clearActions();
        btn.setOrigin(Align.center);
        btn.setTransform(true);
        btn.actions(Actions.rotateTo(expanded ? 0f : 90f, 0.25f, Interp.pow3Out));
    }

    @Override
    protected void drawBackground(float x, float y) {
        if (Boolean.TRUE.equals(feature.hideBackgroundConfig.get())) {
            return;
        }
        Float bgOpacityVal = feature.backgroundOpacityConfig.get();
        float bgAlpha = Mathf.clamp(bgOpacityVal != null ? bgOpacityVal : 0.65f, 0f, 1f);
        if (bgAlpha <= 0.001f) {
            return;
        }
        super.drawBackground(x, y);
        if (Tex.whiteui != null) {
            Draw.color(0f, 0f, 0f, bgAlpha * this.color.a * parentAlpha);
            Tex.whiteui.draw(x, y, width, height);
            Draw.reset();
        }
    }

    public void rebuild() {
        if (disposed || initializing) {
            return;
        }

        lastGraphsEmpty = state.getTeamGraphs().isEmpty();
        statsCollapser = null;
        collapseButton = null;
        clearChildren();

        boolean hideBg = Boolean.TRUE.equals(feature.hideBackgroundConfig.get());
        if (hideBg) {
            setBackground(null);
        } else {
            if (Tex.whiteui == null && getBackground() == null) {
                setBackground(Styles.black6);
            }
            BlurHelper.applyBackdropBlur(this, 10f);
        }

        Float opacityVal = feature.opacityConfig.get();
        color.a = opacityVal != null ? opacityVal : 1f;

        float scale = getEffectiveScale();
        float screenW = ResourceMonitorFeature.getSceneWidth();
        float minWidth = (Vars.mobile ? 120f : 160f) * scale;
        float maxWidth = screenW * 0.95f;

        float targetWidth;
        if (Boolean.TRUE.equals(feature.autoWidthConfig.get())) {
            Integer cfgCols = feature.columnsConfig.get();
            int targetCols = (cfgCols != null && cfgCols >= 2) ? Math.min(8, cfgCols) : 2;
            Integer cfgUnitCols = feature.unitColumnsConfig.get();
            int targetUnitCols = (cfgUnitCols != null && cfgUnitCols >= 2) ? Math.min(10, cfgUnitCols) : targetCols;
            boolean alwaysFlow = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get());
            boolean showItems = Boolean.TRUE.equals(feature.showItemsConfig.get());
            boolean showUnits = Boolean.TRUE.equals(feature.showUnitsConfig.get());
            boolean compactUnits = Boolean.TRUE.equals(feature.compactUnitsConfig.get());

            float itemTargetW = showItems
                    ? targetCols * (20f + (alwaysFlow ? 98f : 58f) + 12f) * scale
                    : 0f;
            float unitTargetW = showUnits
                    ? targetUnitCols * (compactUnits ? 34f : (20f + 38f + 10f)) * scale
                    : 0f;
            targetWidth = Math.max(itemTargetW, unitTargetW);
            if (targetWidth <= 0f) {
                targetWidth = 2f * (20f + 58f + 12f) * scale;
            }
        } else {
            Float cfgW = feature.overlayWidthConfig.get();
            targetWidth = screenW * (cfgW != null ? cfgW : 0.28f);
        }
        final float finalWidth = Mathf.clamp(targetWidth, minWidth, Math.max(minWidth, maxWidth));

        boolean expanded = Boolean.TRUE.equals(feature.expandedConfig.get());
        boolean hideHeader = Boolean.TRUE.equals(feature.hideHeaderConfig.get());

        margin(6f * scale);

        table(content -> {
            content.top().left();

            if (!hideHeader) {
                buildHeader(content, scale);
            }

            if (hideHeader) {
                buildStatsSection(content, finalWidth, scale, false);
            } else {
                StatsCollapser collapser = new StatsCollapser(!expanded, statsWrap -> {
                    statsWrap.top().left();
                    buildStatsSection(statsWrap, finalWidth, scale, true);
                });
                statsCollapser = collapser;
                content.add(collapser).width(finalWidth).top().left().row();
            }
        }).width(finalWidth).top().left();

        float prevHeight = getHeight();
        float prevTopY = prevHeight > 0f ? (feature.y() + prevHeight) : -1f;

        pack();

        float targetY = feature.y();
        if (prevTopY > 0f && Math.abs(getHeight() - prevHeight) > 0.01f) {
            float sw = ResourceMonitorFeature.getSceneWidth();
            float sh = ResourceMonitorFeature.getSceneHeight();
            targetY = prevTopY - getHeight();
            if (sh >= 64f) {
                targetY = Mathf.clamp(targetY, 0f, Math.max(0f, sh - getHeight()));
            }
            if (sw >= 64f) {
                feature.x(Mathf.clamp(feature.x(), 0f, Math.max(0f, sw - getWidth())));
            }
            feature.y(targetY);
        }
        setPosition(feature.x(), targetY);
    }

    private void buildStatsSection(Table container, float finalWidth, float scale, boolean hasHeaderAbove) {
        if (hasHeaderAbove) {
            addSectionDivider(container, finalWidth, scale, 2f * scale);
        }
        if (!Boolean.TRUE.equals(feature.limitHeightConfig.get())) {
            buildStatsContent(container, finalWidth, scale);
            return;
        }
        float screenH = ResourceMonitorFeature.getSceneHeight();
        Float cfgH = feature.overlayHeightConfig.get();
        float heightFrac = Mathf.clamp(cfgH != null ? cfgH : 0.60f, 0.20f, 0.95f);
        float maxStatsHeight = Math.max(80f * scale, screenH * heightFrac - 44f * scale);

        Table inner = new Table();
        inner.top().left();
        buildStatsContent(inner, finalWidth, scale);
        inner.pack();

        if (inner.getPrefHeight() <= maxStatsHeight) {
            container.add(inner).width(finalWidth).top().left().row();
            return;
        }

        ScrollPane pane = Styles.noBarPane != null
                ? new ScrollPane(inner, Styles.noBarPane)
                : new ScrollPane(inner);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        pane.setFadeScrollBars(true);
        container.add(pane).width(finalWidth).maxHeight(maxStatsHeight).top().left().row();
    }

    private static void addSectionDivider(Table parent, float width, float scale, float padV) {
        if (Tex.whiteui == null) {
            return;
        }
        Image div = new Image(Tex.whiteui);
        div.setColor(1f, 1f, 1f, 0.14f);
        parent.add(div).width(width).height(Math.max(1f, 1f * scale)).padTop(padV).padBottom(padV).left().row();
    }

    private void buildHeader(Table content, float scale) {
        float btnSize = 30f * scale;
        float iconSize = 18f * scale;

        content.table(header -> {
            header.left().defaults().padRight(4f * scale);

            if (!Boolean.TRUE.equals(feature.hideDragHandleConfig.get())) {
                ImageButton moveBtn = header.button(Icon.move, Styles.clearNonei, iconSize, () -> {
                }).size(btnSize).get();

                moveBtn.addListener(new InputListener() {
                    float dragStartX;
                    float dragStartY;
                    float elemStartX;
                    float elemStartY;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        dragging = true;
                        dragStartX = event.stageX;
                        dragStartY = event.stageY;
                        elemStartX = ResourceMonitorHudView.this.x;
                        elemStartY = ResourceMonitorHudView.this.y;
                        toFront();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        if (!dragging) {
                            return;
                        }
                        float nextX = elemStartX + (event.stageX - dragStartX);
                        float nextY = elemStartY + (event.stageY - dragStartY);
                        setPosition(nextX, nextY);
                        keepInScreen();
                        feature.x(ResourceMonitorHudView.this.x);
                        feature.y(ResourceMonitorHudView.this.y);
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        if (dragging) {
                            dragging = false;
                            keepInScreen();
                            feature.x(ResourceMonitorHudView.this.x);
                            feature.y(ResourceMonitorHudView.this.y);
                        }
                    }
                });
            }

            boolean expanded = Boolean.TRUE.equals(feature.expandedConfig.get());
            ImageButton collapseBtn = new ImageButton(Icon.downOpen, Styles.clearNonei) {
                @Override
                public void layout() {
                    super.layout();
                    setOrigin(Align.center);
                }
            };
            collapseBtn.resizeImage(iconSize);
            collapseBtn.setOrigin(Align.center);
            collapseBtn.setTransform(true);
            collapseBtn.rotation = expanded ? 0f : 90f;
            collapseBtn.clicked(() -> {
                feature.expandedConfig.set(!Boolean.TRUE.equals(feature.expandedConfig.get()));
            });
            header.add(collapseBtn).size(btnSize);
            collapseButton = collapseBtn;
            addTooltip(collapseBtn, Core.bundle.get(
                    expanded ? "team-resources.collapse" : "team-resources.expand"));

            Seq<Team> validTeams = state.validTeamsSignal.peek();
            if (validTeams != null && !validTeams.isEmpty()) {
                header.table(teamsRow -> {
                    teamsRow.left().defaults().pad(2f * scale);
                    int limit = Math.min(validTeams.size, 5);
                    for (int i = 0; i < limit; i++) {
                        Team team = validTeams.get(i);
                        teamsRow.button(b -> {
                            b.image(Tex.whiteui).size(16f * scale).color(team.color);
                        }, Styles.clearTogglei, () -> {
                            state.setSelectedTeam(team);
                        }).checked(sel -> state.getSelectedTeam() == team).size(26f * scale);
                    }
                    if (validTeams.size > 5) {
                        teamsRow.button("...", Styles.flatBordert,
                                () -> new ResourceMonitorAllTeamsDialog(state).show())
                                .size(26f * scale);
                    }
                }).left();
            }

            header.add().growX();

            ImageButton settingsBtn = header.button(Icon.settings, Styles.clearNonei, iconSize,
                    feature::showSettingDialog).size(btnSize).padRight(0f).get();
            addTooltip(settingsBtn, Core.bundle.get("team-resources.settings.title"));
        }).growX().padBottom(2f * scale).top().left().row();
    }

    private void buildStatsContent(Table content, float finalWidth, float scale) {
        Integer cfgCols = feature.columnsConfig.get();
        int fixedCols = (cfgCols != null && cfgCols >= 2) ? Math.min(8, cfgCols) : 0;
        Float rawRowSpacing = feature.rowSpacingConfig.get();
        float rowSpacing = Mathf.clamp(rawRowSpacing != null ? rawRowSpacing : 3f, 0f, 12f) * scale;

        boolean showItems = Boolean.TRUE.equals(feature.showItemsConfig.get());
        boolean showUnits = Boolean.TRUE.equals(feature.showUnitsConfig.get());
        boolean showPower = Boolean.TRUE.equals(feature.showPowerConfig.get());

        content.table(stats -> {
            stats.top().left();

            if (showItems) {
                Seq<Item> items = state.usedItemsSignal.peek();
                stats.table(itemsTable -> {
                    itemsTable.left();
                    itemsTable.touchable = Touchable.enabled;
                    itemsTable.addListener(statsListener);

                    if (items == null || items.isEmpty()) {
                        Label emptyLbl = new Label(
                                Core.bundle.get("team-resources.no-items"),
                                Styles.outlineLabel);
                        emptyLbl.setColor(Color.gray);
                        emptyLbl.setFontScale(0.8f * scale);
                        itemsTable.add(emptyLbl).left();
                        return;
                    }

                    boolean alwaysFlow = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get());
                    float iconSize = 20f * scale;
                    float labelWidth = (alwaysFlow ? 98f : 58f) * scale;
                    float padding = 12f * scale;
                    float minCellWidth = iconSize + labelWidth + padding;
                    int cols = fixedCols > 0
                            ? fixedCols
                            : Math.max(2, (int) (finalWidth / Math.max(1f, minCellWidth)));
                    float colWidth = finalWidth / cols;
                    float fixedLabelWidth = Math.max(16f * scale, colWidth - iconSize - 12f * scale);

                    int i = 0;
                    for (Item item : items) {
                        Table itemCell = itemsTable.table(cell -> {
                            cell.left();
                            cell.image(item.uiIcon).size(iconSize).scaling(Scaling.fit).padRight(4f * scale);
                            Label itemLbl = new Label(() -> state.formatItem(item));
                            itemLbl.setStyle(Styles.outlineLabel);
                            itemLbl.setFontScale(0.76f * scale);
                            itemLbl.setEllipsis(true);
                            cell.add(itemLbl).width(fixedLabelWidth).padRight(8f * scale).left();
                        }).width(colWidth).padBottom(rowSpacing).left().get();

                        if (animatedItems.add(item)) {
                            itemCell.setTransform(true);
                            itemCell.setOrigin(Align.left);
                            itemCell.setScale(0.75f);
                            itemCell.color.a = 0f;
                            itemCell.actions(Actions.parallel(
                                    Actions.fadeIn(0.22f, Interp.pow2Out),
                                    Actions.scaleTo(1f, 1f, 0.22f, Interp.swingOut)));
                        }

                        if (++i % cols == 0) {
                            itemsTable.row();
                        }
                    }
                    if (i % cols != 0) {
                        for (int rem = i % cols; rem < cols; rem++) {
                            itemsTable.add().width(colWidth);
                        }
                    }
                }).width(finalWidth).padTop(2f * scale).padBottom(2f * scale).row();
            }

            if (showUnits) {
                if (showItems) {
                    addSectionDivider(stats, finalWidth, scale, Math.max(2f * scale, rowSpacing));
                }
                Seq<UnitType> units = state.usedUnitsSignal.peek();
                stats.table(unitsTable -> {
                    unitsTable.left();

                    if (units == null || units.isEmpty()) {
                        Label emptyLbl = new Label(
                                Core.bundle.get("team-resources.no-units"),
                                Styles.outlineLabel);
                        emptyLbl.setColor(Color.gray);
                        emptyLbl.setFontScale(0.8f * scale);
                        unitsTable.add(emptyLbl).left();
                        return;
                    }

                    Integer cfgUnitCols = feature.unitColumnsConfig.get();
                    int fixedUnitCols = (cfgUnitCols != null && cfgUnitCols >= 2) ? Math.min(10, cfgUnitCols) : 0;
                    boolean compactUnits = Boolean.TRUE.equals(feature.compactUnitsConfig.get());

                    if (compactUnits) {
                        float defaultUnitSize = 26f * scale;
                        float unitPad = 6f * scale;
                        float cellWidth = defaultUnitSize + unitPad;
                        int cols = fixedUnitCols > 0
                                ? fixedUnitCols
                                : Math.max(2, (int) (finalWidth / Math.max(1f, cellWidth)));
                        float colWidth = fixedUnitCols > 0
                                ? (finalWidth / cols)
                                : Math.min(cellWidth + 4f * scale, finalWidth / cols);
                        float unitSize = Math.min(defaultUnitSize, Math.max(16f * scale, colWidth - unitPad));

                        int i = 0;
                        for (UnitType type : units) {
                            Table unitCell = unitsTable.table(cell -> {
                                Image unitImg = new Image(type.uiIcon);
                                unitImg.setScaling(Scaling.fit);

                                Table countOverlay = new Table();
                                countOverlay.right().bottom();
                                countOverlay.label(() -> state.getUnitCountText(type))
                                        .fontScale(0.70f * scale)
                                        .style(Styles.outlineLabel);

                                cell.stack(unitImg, countOverlay).size(unitSize);
                            }).width(colWidth).padBottom(rowSpacing).left().get();

                            if (animatedUnits.add(type)) {
                                unitCell.setTransform(true);
                                unitCell.setOrigin(Align.center);
                                unitCell.setScale(0.6f);
                                unitCell.color.a = 0f;
                                unitCell.actions(Actions.parallel(
                                        Actions.fadeIn(0.22f, Interp.pow2Out),
                                        Actions.scaleTo(1f, 1f, 0.22f, Interp.swingOut)));
                            }

                            if (++i % cols == 0) {
                                unitsTable.row();
                            }
                        }
                    } else {
                        float iconSize = 20f * scale;
                        float labelWidth = 38f * scale;
                        float padding = 10f * scale;
                        float minCellWidth = iconSize + labelWidth + padding;
                        int cols = fixedUnitCols > 0
                                ? fixedUnitCols
                                : Math.max(2, (int) (finalWidth / Math.max(1f, minCellWidth)));
                        float colWidth = fixedUnitCols > 0
                                ? (finalWidth / cols)
                                : Math.min(92f * scale, finalWidth / cols);
                        float fixedLabelWidth = Math.max(16f * scale, colWidth - iconSize - 10f * scale);

                        int i = 0;
                        for (UnitType type : units) {
                            Table unitCell = unitsTable.table(cell -> {
                                cell.left();
                                cell.image(type.uiIcon).size(iconSize).scaling(Scaling.fit).padRight(4f * scale);
                                Label unitLbl = new Label(() -> state.getUnitCountText(type));
                                unitLbl.setStyle(Styles.outlineLabel);
                                unitLbl.setFontScale(0.76f * scale);
                                unitLbl.setEllipsis(true);
                                cell.add(unitLbl).width(fixedLabelWidth).padRight(6f * scale).left();
                            }).width(colWidth).padBottom(rowSpacing).left().get();

                            if (animatedUnits.add(type)) {
                                unitCell.setTransform(true);
                                unitCell.setOrigin(Align.left);
                                unitCell.setScale(0.75f);
                                unitCell.color.a = 0f;
                                unitCell.actions(Actions.parallel(
                                        Actions.fadeIn(0.22f, Interp.pow2Out),
                                        Actions.scaleTo(1f, 1f, 0.22f, Interp.swingOut)));
                            }

                            if (++i % cols == 0) {
                                unitsTable.row();
                            }
                        }
                    }
                }).width(finalWidth).left().padTop(2f * scale).padBottom(2f * scale).row();
            }

            if (showPower) {
                if (showItems || showUnits) {
                    addSectionDivider(stats, finalWidth, scale, Math.max(2f * scale, rowSpacing));
                }
                stats.table(power -> {
                    power.left().defaults().growX();
                    float barHeight = 18f * scale;

                    if (state.getTeamGraphs().isEmpty()) {
                        Label noPowerLabel = new Label(
                                Core.bundle.get("team-resources.no-power"),
                                Styles.outlineLabel);
                        noPowerLabel.setFontScale(0.8f * scale);
                        noPowerLabel.setColor(Color.gray);
                        power.add(noPowerLabel).width(finalWidth).left().row();
                    } else {
                        String rawPowerPrefix = Core.bundle.get("team-resources.power-prefix");
                        final String powerPrefix = rawPowerPrefix.endsWith(" ") ? rawPowerPrefix : rawPowerPrefix + " ";

                        Label powerLabel = new Label("", Styles.outlineLabel);
                        powerLabel.setFontScale(0.78f * scale);
                        powerLabel.setEllipsis(true);
                        final String[] lastBalanceStr = { null };
                        powerLabel.update(() -> {
                            String balanceStr = state.getFormattedPowerBalance();
                            if (!balanceStr.equals(lastBalanceStr[0])) {
                                lastBalanceStr[0] = balanceStr;
                                powerLabel.setText(powerPrefix + balanceStr);
                            }
                            powerLabel.setColor(state.getPowerBalanceColor());
                        });
                        power.add(powerLabel).width(finalWidth).left().padBottom(Math.max(2f * scale, rowSpacing)).row();

                        power.add(new SplitBar(
                                state.getTeamGraphs(),
                                Mode.SATISFACTION,
                                this::getEffectiveScale))
                                .width(finalWidth)
                                .height(barHeight)
                                .padBottom(rowSpacing)
                                .row();

                        if (Boolean.TRUE.equals(feature.showStoredPowerConfig.get())) {
                            String rawStoredPrefix = Core.bundle.get("team-resources.stored-prefix");
                            final String storedPrefix = rawStoredPrefix.endsWith(" ") ? rawStoredPrefix
                                    : rawStoredPrefix + " ";

                            Label storedLabel = new Label("", Styles.outlineLabel);
                            storedLabel.setFontScale(0.78f * scale);
                            storedLabel.setEllipsis(true);
                            final String[] lastStoredStr = { null };
                            storedLabel.update(() -> {
                                String storedStr = state.getFormattedStoredPower();
                                if (!storedStr.equals(lastStoredStr[0])) {
                                    lastStoredStr[0] = storedStr;
                                    storedLabel.setText(storedPrefix + storedStr);
                                }
                            });
                            power.add(storedLabel).width(finalWidth).left()
                                    .padTop(Math.max(3f * scale, rowSpacing * 1.5f))
                                    .padBottom(Math.max(2f * scale, rowSpacing)).row();

                            power.add(new SplitBar(
                                    state.getTeamGraphs(),
                                    Mode.STORED,
                                    this::getEffectiveScale))
                                    .width(finalWidth)
                                    .height(barHeight)
                                    .row();
                        }
                    }
                }).width(finalWidth).padTop(2f * scale).row();
            }
        }).width(finalWidth).top().left();
    }

    private static void addTooltip(Element element, String text) {
        if (text == null || text.isEmpty() || Styles.black8 == null) {
            return;
        }
        try {
            element.addListener(new Tooltip(t -> t.background(Styles.black8).margin(4f).add(text)));
        } catch (Throwable ignored) {
        }
    }

    public void keepInScreen() {
        if (disposed || !feature.isEnabled() || Core.scene == null) {
            return;
        }
        float sw = ResourceMonitorFeature.getSceneWidth();
        float sh = ResourceMonitorFeature.getSceneHeight();
        if (sw < 64f || sh < 64f) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        float clampedX = Mathf.clamp(x, 0f, Math.max(0f, sw - w));
        float clampedY = Mathf.clamp(y, 0f, Math.max(0f, sh - h));
        if (Math.abs(clampedX - x) > 0.01f || Math.abs(clampedY - y) > 0.01f) {
            setPosition(clampedX, clampedY);
            if (!dragging) {
                feature.x(clampedX);
                feature.y(clampedY);
            }
        }
    }

    private final class StatsHoverListener extends ClickListener {
        private boolean isHovered = false;

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            state.setHoldingForStats(true);
            state.setViewingStats(true);
            return super.touchDown(event, x, y, pointer, button);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            state.setHoldingForStats(false);
            if (Vars.mobile || !isHovered) {
                state.setViewingStats(false);
            }
            super.touchUp(event, x, y, pointer, button);
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, @Nullable Element fromActor) {
            if (pointer == -1) {
                if (fromActor != null && event.listenerActor != null && fromActor.isDescendantOf(event.listenerActor)) {
                    super.enter(event, x, y, pointer, fromActor);
                    return;
                }
                isHovered = true;
                if (!state.isHoldingForStats()) {
                    state.setViewingStats(true);
                }
            }
            super.enter(event, x, y, pointer, fromActor);
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, @Nullable Element toActor) {
            if (pointer == -1) {
                if (toActor != null && event.listenerActor != null && toActor.isDescendantOf(event.listenerActor)) {
                    super.exit(event, x, y, pointer, toActor);
                    return;
                }
                isHovered = false;
                if (!state.isHoldingForStats()) {
                    state.setViewingStats(false);
                }
            }
            super.exit(event, x, y, pointer, toActor);
        }
    }

    private static final class StatsCollapser extends WidgetGroup {
        private final Table collTable = new Table();
        private boolean collapse;
        private final CollapseAction collapseAction = new CollapseAction();
        private boolean actionRunning = false;
        private float currentHeight = 0f;

        StatsCollapser(boolean collapsed, Cons<Table> cons) {
            this.collapse = collapsed;
            collapseAction.setDuration(0.25f);
            collapseAction.setInterpolation(Interp.pow3Out);
            setTransform(true);
            updateTouchable();
            addChild(collTable);
            cons.get(collTable);
        }

        void setCollapsed(boolean nextCollapse) {
            if (this.collapse == nextCollapse) {
                return;
            }
            this.collapse = nextCollapse;
            updateTouchable();
            actionRunning = true;
            removeAction(collapseAction);
            addAction(collapseAction);
            collapseAction.restart();
        }

        boolean isActionRunning() {
            return actionRunning;
        }

        private void updateTouchable() {
            this.touchable = collapse ? Touchable.disabled : Touchable.enabled;
        }

        @Override
        public void draw() {
            if (!actionRunning) {
                if (!collapse) {
                    super.draw();
                }
                return;
            }
            if (getWidth() > 1f && currentHeight > 1f) {
                Draw.flush();
                if (clipBegin(x, y, getWidth(), currentHeight)) {
                    super.draw();
                    Draw.flush();
                    clipEnd();
                }
            }
        }

        @Override
        protected void drawChildren() {
            if (collapse && !actionRunning) {
                return;
            }
            super.drawChildren();
        }

        @Override
        public void layout() {
            float w = getWidth();
            float h = collTable.getPrefHeight();
            collTable.setBounds(0f, 0f, w, h);
            if (!actionRunning) {
                currentHeight = collapse ? 0f : h;
            }
        }

        @Override
        public float getPrefWidth() {
            return collTable.getPrefWidth();
        }

        @Override
        public float getPrefHeight() {
            return !actionRunning ? (collapse ? 0f : collTable.getPrefHeight()) : currentHeight;
        }

        @Override
        public float getMinWidth() {
            return 0f;
        }

        @Override
        public float getMinHeight() {
            return 0f;
        }

        private final class CollapseAction extends TemporalAction {
            @Override
            public boolean act(float delta) {
                return super.act(delta) || !actionRunning;
            }

            @Override
            protected void begin() {
                actionRunning = true;
            }

            @Override
            protected void update(float percent) {
                currentHeight = (collapse ? (1f - percent) : percent) * collTable.getPrefHeight();
                invalidateHierarchy();
                if (parent != null) {
                    parent.invalidateHierarchy();
                }
            }

            @Override
            protected void end() {
                currentHeight = collapse ? 0f : collTable.getPrefHeight();
                actionRunning = false;
                invalidateHierarchy();
                if (parent != null) {
                    parent.invalidateHierarchy();
                }
            }
        }
    }
}
