package mindustrytool.features.resourcemonitor;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.core.UI;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles.Colors;
import solim.core.Component;
import solim.reactive.Effect;

public class ResourceMonitorPreviewView extends Component {

    private static final float MAX_PREVIEW_WIDTH = 420f;
    private static final float MIN_PREVIEW_WIDTH = 140f;
    private static final float MAX_PREVIEW_SCALE = 1.35f;
    private static final float MIN_PREVIEW_SCALE = 0.5f;

    private static final int[] SAMPLE_ITEM_AMOUNTS = { 4200, 3850, 2100, 1450, 920, 640, 0, 0 };
    private static final int[] SAMPLE_ITEM_RATES = { 45, 28, -12, 16, 8, -4, 0, 0 };
    private static final int[] SAMPLE_UNIT_COUNTS = { 24, 12, 8, 4, 0, 0 };
    private static final Color[] SAMPLE_ITEM_COLORS = {
            Color.valueOf("d99d73"),
            Color.valueOf("8c7fa9"),
            Color.valueOf("ebeef5"),
            Color.valueOf("b2c6d2"),
            Color.valueOf("53565c"),
            Color.valueOf("8da1e3"),
            Color.valueOf("f9a3c7"),
            Color.valueOf("f3e979")
    };

    private final ResourceMonitorFeature feature;
    private boolean initializing = true;
    private @Nullable Table previewStage;

    public ResourceMonitorPreviewView(ResourceMonitorFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Table stage = new Table() {
            @Override
            protected void drawBackground(float x, float y) {
                super.drawBackground(x, y);
                drawViewportGrid(x, y, width, height);
            }
        };
        stage.name = "resource-monitor-preview-stage";
        stage.touchable = Touchable.disabled;
        stage.background(Styles.black5 != null ? Styles.black5 : Styles.black6);
        stage.margin(10f);
        stage.center();
        this.previewStage = stage;

        initializing = true;
        registerEffects();
        initializing = false;
        rebuildPreview();

        return stage;
    }

    private void registerEffects() {
        Effect.of(feature.opacityConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.backgroundOpacityConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.scaleConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.overlayWidthConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.overlayHeightConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.limitHeightConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.rowSpacingConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.autoWidthConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.columnsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.unitColumnsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.showItemsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.showUnitsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.compactUnitsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.showPowerConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.showStoredPowerConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.hideBackgroundConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.alwaysShowFlowRateConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.hideZeroUnitsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.hideZeroItemsConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.hideDragHandleConfig.signal(), v -> rebuildPreview());
        Effect.of(feature.hideHeaderConfig.signal(), v -> rebuildPreview());
    }

    @Override
    protected void onDispose() {
        previewStage = null;
    }

    private void drawViewportGrid(float x, float y, float w, float h) {
        if (Tex.whiteui == null) {
            return;
        }
        Draw.color(Colors.SECTION_BORDER, 0.45f);
        float step = 20f;
        Lines.stroke(1f);
        for (float gx = x + step; gx < x + w - 1f; gx += step) {
            Lines.line(gx, y, gx, y + h);
        }
        for (float gy = y + step; gy < y + h - 1f; gy += step) {
            Lines.line(x, gy, x + w, gy);
        }
        Lines.rect(x, y, w, h);
        Draw.reset();
    }

    public void rebuildPreview() {
        if (initializing) {
            return;
        }
        Table stage = previewStage;
        if (stage == null) {
            return;
        }
        stage.clearChildren();

        Float rawScale = feature.scaleConfig.get();
        float scale = Mathf.clamp(rawScale != null ? rawScale : 1f, MIN_PREVIEW_SCALE, MAX_PREVIEW_SCALE);

        float minW = Mathf.clamp(MIN_PREVIEW_WIDTH * scale, 120f, MAX_PREVIEW_WIDTH);
        final float finalWidth;
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
                    ? targetCols * (18f + (alwaysFlow ? 82f : 48f) + 10f) * scale
                    : 0f;
            float unitTargetW = showUnits
                    ? targetUnitCols * (compactUnits ? 28f : (18f + 36f + 8f)) * scale
                    : 0f;
            float computedW = Math.max(itemTargetW, unitTargetW);
            if (computedW <= 0f) {
                computedW = 2f * (18f + 48f + 10f) * scale;
            }
            finalWidth = Mathf.clamp(computedW, minW, MAX_PREVIEW_WIDTH);
        } else {
            Float rawWidthFrac = feature.overlayWidthConfig.get();
            float widthFrac = rawWidthFrac != null ? rawWidthFrac : 0.28f;
            float normWidth = Mathf.clamp((widthFrac - 0.15f) / (1.0f - 0.15f), 0f, 1f);
            finalWidth = Mathf.lerp(minW, MAX_PREVIEW_WIDTH, normWidth);
        }

        boolean hideBg = Boolean.TRUE.equals(feature.hideBackgroundConfig.get());
        boolean hideHeader = Boolean.TRUE.equals(feature.hideHeaderConfig.get());
        Float opacityVal = feature.opacityConfig.get();
        float opacity = opacityVal != null ? opacityVal : 1f;
        Float bgOpacityVal = feature.backgroundOpacityConfig.get();
        final float bgOpacity = Mathf.clamp(bgOpacityVal != null ? bgOpacityVal : 0.65f, 0f, 1f);

        Table hudMock = new Table() {
            @Override
            protected void drawBackground(float x, float y) {
                if (hideBg || bgOpacity <= 0.001f) {
                    return;
                }
                if (Tex.whiteui != null) {
                    Draw.color(0f, 0f, 0f, bgOpacity * this.color.a * parentAlpha);
                    Tex.whiteui.draw(x, y, width, height);
                    Draw.reset();
                } else {
                    super.drawBackground(x, y);
                }
            }
        };
        hudMock.name = "resource-monitor-preview-hud";
        hudMock.top().left();
        Drawable bg = (hideBg || Tex.whiteui != null) ? null : Styles.black6;
        hudMock.setBackground(bg);
        hudMock.color.a = opacity;
        hudMock.margin(6f * scale);

        hudMock.table(content -> {
            content.top().left();
            if (!hideHeader) {
                buildPreviewHeader(content, scale);
                addSectionDivider(content, finalWidth, scale, 2f * scale);
            }
            buildPreviewStats(content, finalWidth, scale);
        }).width(finalWidth).top().left();

        stage.add(hudMock).width(finalWidth + 12f * scale).center();
    }

    private static void addSectionDivider(Table parent, float width, float scale, float padV) {
        if (Tex.whiteui == null) {
            return;
        }
        Image div = new Image(Tex.whiteui);
        div.setColor(1f, 1f, 1f, 0.14f);
        parent.add(div).width(width).height(Math.max(1f, 1f * scale)).padTop(padV).padBottom(padV).left().row();
    }

    private void buildPreviewHeader(Table content, float scale) {
        float btnSize = 26f * scale;
        float iconSize = 15f * scale;

        content.table(header -> {
            header.left().defaults().padRight(4f * scale);

            if (!Boolean.TRUE.equals(feature.hideDragHandleConfig.get()) && Icon.move != null) {
                header.image(Icon.move).size(iconSize).pad(Math.max(2f, (btnSize - iconSize) * 0.5f))
                        .color(Color.lightGray);
            }

            if (Icon.downOpen != null) {
                header.image(Icon.downOpen).size(iconSize).pad(Math.max(2f, (btnSize - iconSize) * 0.5f))
                        .color(Color.white);
            }

            if (Tex.whiteui != null) {
                header.table(teamsRow -> {
                    teamsRow.left().defaults().pad(2f * scale);
                    Color[] teamColors = {
                            Team.sharded != null ? Team.sharded.color : Pal.accent,
                            Team.crux != null ? Team.crux.color : Pal.remove,
                            Team.malis != null ? Team.malis.color : Color.purple
                    };
                    for (int i = 0; i < teamColors.length; i++) {
                        Color tc = teamColors[i];
                        boolean selected = (i == 0);
                        teamsRow.table(chip -> {
                            if (selected && Styles.black5 != null) {
                                chip.background(Styles.black5);
                            }
                            chip.image(Tex.whiteui).size(14f * scale).color(tc);
                        }).size(22f * scale);
                    }
                }).left();
            }

            header.add().growX();

            if (Icon.settings != null) {
                header.image(Icon.settings).size(iconSize).pad(Math.max(2f, (btnSize - iconSize) * 0.5f))
                        .padRight(0f)
                        .color(Color.lightGray);
            }
        }).growX().padBottom(2f * scale).top().left().row();
    }

    private void buildPreviewStats(Table content, float finalWidth, float scale) {
        Integer cfgCols = feature.columnsConfig.get();
        int fixedCols = (cfgCols != null && cfgCols >= 2) ? Math.min(8, cfgCols) : 0;
        Float rawRowSpacing = feature.rowSpacingConfig.get();
        float rowSpacing = Mathf.clamp(rawRowSpacing != null ? rawRowSpacing : 3f, 0f, 12f) * scale;

        boolean showItems = Boolean.TRUE.equals(feature.showItemsConfig.get());
        boolean showUnits = Boolean.TRUE.equals(feature.showUnitsConfig.get());
        boolean showPower = Boolean.TRUE.equals(feature.showPowerConfig.get());

        content.table(stats -> {
            stats.top().left();

            if (!showItems && !showUnits && !showPower) {
                Label empty = new Label(
                        Core.bundle.get("team-resources.no-items"),
                        Styles.outlineLabel != null ? Styles.outlineLabel : new LabelStyle(Fonts.def, Color.gray));
                empty.setColor(Color.gray);
                empty.setFontScale(0.75f * scale);
                stats.add(empty).left();
                return;
            }

            if (showItems) {
                buildSampleItems(stats, finalWidth, scale, fixedCols, rowSpacing);
            }

            if (showUnits) {
                if (showItems) {
                    addSectionDivider(stats, finalWidth, scale, Math.max(2f * scale, rowSpacing));
                }
                buildSampleUnits(stats, finalWidth, scale, rowSpacing);
            }

            if (showPower) {
                if (showItems || showUnits) {
                    addSectionDivider(stats, finalWidth, scale, Math.max(2f * scale, rowSpacing));
                }
                buildSamplePower(stats, finalWidth, scale, rowSpacing);
            }
        }).width(finalWidth).top().left();
    }

    private void buildSampleItems(Table stats, float finalWidth, float scale, int fixedCols, float rowSpacing) {
        boolean hideZeroItems = Boolean.TRUE.equals(feature.hideZeroItemsConfig.get());
        boolean alwaysFlow = Boolean.TRUE.equals(feature.alwaysShowFlowRateConfig.get());

        Seq<Item> sampleItems = resolveSampleItems();
        int countToRender = hideZeroItems ? 6 : 8;

        stats.table(itemsTable -> {
            itemsTable.left();

            float iconSize = 18f * scale;
            float labelWidth = (alwaysFlow ? 82f : 48f) * scale;
            float padding = 10f * scale;
            float minCellWidth = iconSize + labelWidth + padding;
            int cols = fixedCols > 0
                    ? fixedCols
                    : Math.max(2, (int) (finalWidth / Math.max(1f, minCellWidth)));
            float colWidth = finalWidth / cols;
            float fixedLabelWidth = Math.max(14f * scale, colWidth - iconSize - 10f * scale);

            for (int i = 0; i < countToRender; i++) {
                int idx = i;
                Item item = idx < sampleItems.size ? sampleItems.get(idx) : null;
                int amount = SAMPLE_ITEM_AMOUNTS[idx % SAMPLE_ITEM_AMOUNTS.length];
                int rate = SAMPLE_ITEM_RATES[idx % SAMPLE_ITEM_RATES.length];

                itemsTable.table(cell -> {
                    cell.left();
                    TextureRegion region = (item != null && item.uiIcon != null) ? item.uiIcon : null;
                    if (region != null) {
                        cell.image(region).size(iconSize).scaling(Scaling.fit).padRight(3f * scale);
                    } else if (Tex.whiteui != null) {
                        cell.image(Tex.whiteui).size(iconSize).color(sampleItemColor(idx)).padRight(3f * scale);
                    }

                    Label itemLbl = new Label(formatSampleItem(amount, rate, alwaysFlow),
                            Styles.outlineLabel != null ? Styles.outlineLabel
                                    : new LabelStyle(Fonts.def, Color.white));
                    itemLbl.setFontScale(0.72f * scale);
                    itemLbl.setEllipsis(true);
                    cell.add(itemLbl).width(fixedLabelWidth).padRight(6f * scale).left();
                }).width(colWidth).padBottom(rowSpacing).left();

                if ((i + 1) % cols == 0) {
                    itemsTable.row();
                }
            }
            if (countToRender % cols != 0) {
                for (int rem = countToRender % cols; rem < cols; rem++) {
                    itemsTable.add().width(colWidth);
                }
            }
        }).width(finalWidth).padTop(2f * scale).padBottom(2f * scale).row();
    }

    private void buildSampleUnits(Table stats, float finalWidth, float scale, float rowSpacing) {
        boolean hideZeroUnits = Boolean.TRUE.equals(feature.hideZeroUnitsConfig.get());
        Seq<UnitType> sampleUnits = resolveSampleUnits();
        int countToRender = hideZeroUnits ? 4 : 6;
        Integer cfgUnitCols = feature.unitColumnsConfig.get();
        int fixedUnitCols = (cfgUnitCols != null && cfgUnitCols >= 2) ? Math.min(10, cfgUnitCols) : 0;
        boolean compactUnits = Boolean.TRUE.equals(feature.compactUnitsConfig.get());

        stats.table(unitsTable -> {
            unitsTable.left();

            if (compactUnits) {
                float defaultUnitSize = 24f * scale;
                float unitPad = 4f * scale;
                float cellWidth = defaultUnitSize + unitPad;
                int cols = fixedUnitCols > 0
                        ? fixedUnitCols
                        : Math.max(2, (int) (finalWidth / Math.max(1f, cellWidth)));
                float colWidth = fixedUnitCols > 0
                        ? (finalWidth / cols)
                        : Math.min(cellWidth + 4f * scale, finalWidth / cols);
                float unitSize = Math.min(defaultUnitSize, Math.max(14f * scale, colWidth - unitPad));

                for (int i = 0; i < countToRender; i++) {
                    int idx = i;
                    UnitType type = idx < sampleUnits.size ? sampleUnits.get(idx) : null;
                    int unitCount = SAMPLE_UNIT_COUNTS[idx % SAMPLE_UNIT_COUNTS.length];

                    unitsTable.table(cell -> {
                        TextureRegion region = (type != null && type.uiIcon != null) ? type.uiIcon : null;
                        Image unitImg = region != null ? new Image(region) : new Image(Tex.whiteui);
                        if (region == null) {
                            unitImg.setColor(unitCount > 0 ? Pal.accent : Color.gray);
                        } else if (unitCount == 0) {
                            unitImg.setColor(1f, 1f, 1f, 0.45f);
                        }
                        unitImg.setScaling(Scaling.fit);

                        Table countOverlay = new Table();
                        countOverlay.right().bottom();
                        String countText = unitCount > 0 ? UI.formatAmount(unitCount) : "0";
                        Label countLbl = new Label(countText,
                                Styles.outlineLabel != null ? Styles.outlineLabel
                                        : new LabelStyle(Fonts.def, Color.white));
                        countLbl.setFontScale(0.66f * scale);
                        if (unitCount == 0) {
                            countLbl.setColor(Color.gray);
                        }
                        countOverlay.add(countLbl);

                        cell.stack(unitImg, countOverlay).size(unitSize);
                    }).width(colWidth).padBottom(rowSpacing).left();

                    if ((i + 1) % cols == 0) {
                        unitsTable.row();
                    }
                }
            } else {
                float iconSize = 18f * scale;
                float labelWidth = 34f * scale;
                float padding = 8f * scale;
                float minCellWidth = iconSize + labelWidth + padding;
                int cols = fixedUnitCols > 0
                        ? fixedUnitCols
                        : Math.max(2, (int) (finalWidth / Math.max(1f, minCellWidth)));
                float colWidth = fixedUnitCols > 0
                        ? (finalWidth / cols)
                        : Math.min(78f * scale, finalWidth / cols);
                float fixedLabelWidth = Math.max(14f * scale, colWidth - iconSize - 8f * scale);

                for (int i = 0; i < countToRender; i++) {
                    int idx = i;
                    UnitType type = idx < sampleUnits.size ? sampleUnits.get(idx) : null;
                    int unitCount = SAMPLE_UNIT_COUNTS[idx % SAMPLE_UNIT_COUNTS.length];

                    unitsTable.table(cell -> {
                        cell.left();
                        TextureRegion region = (type != null && type.uiIcon != null) ? type.uiIcon : null;
                        Image unitImg = region != null ? new Image(region) : new Image(Tex.whiteui);
                        if (region == null) {
                            unitImg.setColor(unitCount > 0 ? Pal.accent : Color.gray);
                        } else if (unitCount == 0) {
                            unitImg.setColor(1f, 1f, 1f, 0.45f);
                        }
                        unitImg.setScaling(Scaling.fit);
                        cell.add(unitImg).size(iconSize).padRight(3f * scale);

                        String countText = unitCount > 0 ? UI.formatAmount(unitCount) : "0";
                        Label countLbl = new Label(countText,
                                Styles.outlineLabel != null ? Styles.outlineLabel
                                        : new LabelStyle(Fonts.def, Color.white));
                        countLbl.setFontScale(0.72f * scale);
                        countLbl.setEllipsis(true);
                        if (unitCount == 0) {
                            countLbl.setColor(Color.gray);
                        }
                        cell.add(countLbl).width(fixedLabelWidth).padRight(5f * scale).left();
                    }).width(colWidth).padBottom(rowSpacing).left();

                    if ((i + 1) % cols == 0) {
                        unitsTable.row();
                    }
                }
            }
        }).width(finalWidth).left().padTop(2f * scale).padBottom(2f * scale).row();
    }

    private void buildSamplePower(Table stats, float finalWidth, float scale, float rowSpacing) {
        stats.table(power -> {
            power.left().defaults().growX();
            float barHeight = 16f * scale;

            String rawPowerPrefix = Core.bundle.get("team-resources.power-prefix");
            String powerPrefix = rawPowerPrefix.endsWith(" ") ? rawPowerPrefix : rawPowerPrefix + " ";

            Label powerLabel = new Label(
                    powerPrefix + "+1.4k",
                    Styles.outlineLabel != null ? Styles.outlineLabel : new LabelStyle(Fonts.def, Color.white));
            powerLabel.setFontScale(0.75f * scale);
            powerLabel.setColor(Pal.heal);
            powerLabel.setEllipsis(true);
            power.add(powerLabel).width(finalWidth).left().padBottom(Math.max(2f * scale, rowSpacing)).row();

            power.add(new MockBarElement(0.82f, "+1.4k", Pal.powerBar, scale))
                    .width(finalWidth)
                    .height(barHeight)
                    .padBottom(rowSpacing)
                    .row();

            if (Boolean.TRUE.equals(feature.showStoredPowerConfig.get())) {
                String rawStoredPrefix = Core.bundle.get("team-resources.stored-prefix");
                String storedPrefix = rawStoredPrefix.endsWith(" ") ? rawStoredPrefix : rawStoredPrefix + " ";

                Label storedLabel = new Label(
                        storedPrefix + "45k / 60k",
                        Styles.outlineLabel != null ? Styles.outlineLabel
                                : new LabelStyle(Fonts.def, Color.white));
                storedLabel.setFontScale(0.75f * scale);
                storedLabel.setEllipsis(true);
                power.add(storedLabel).width(finalWidth).left().padTop(Math.max(3f * scale, rowSpacing * 1.5f))
                        .padBottom(Math.max(2f * scale, rowSpacing)).row();

                power.add(new MockBarElement(0.75f, "45k", Pal.powerBar, scale))
                        .width(finalWidth)
                        .height(barHeight)
                        .row();
            }
        }).width(finalWidth).padTop(2f * scale).row();
    }

    private static String formatSampleItem(int amount, int rate, boolean alwaysFlow) {
        String color = rate < 0 ? "[scarlet]" : (rate > 0 ? "[lime]" : "[white]");
        String amtText = UI.formatAmount(amount);
        if (!alwaysFlow) {
            return color + amtText;
        }
        String rateStr = rate == 0 ? "0/s" : (rate > 0 ? "[lime]+" + rate + "[gray]/s[]" : "[scarlet]" + rate + "[gray]/s[]");
        return color + amtText + " [gray](" + rateStr + "[gray])[]";
    }

    private static Seq<Item> resolveSampleItems() {
        Seq<Item> result = new Seq<>();
        Item[] candidates = {
                Items.copper, Items.lead, Items.metaglass, Items.graphite,
                Items.silicon, Items.titanium, Items.thorium, Items.surgeAlloy
        };
        for (Item item : candidates) {
            if (item != null) {
                result.add(item);
            }
        }
        if (result.isEmpty() && Vars.content != null && Vars.content.items() != null) {
            for (Item item : Vars.content.items()) {
                if (item != null && result.size < 8) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static Seq<UnitType> resolveSampleUnits() {
        Seq<UnitType> result = new Seq<>();
        UnitType[] candidates = {
                UnitTypes.poly, UnitTypes.mega, UnitTypes.flare,
                UnitTypes.mono, UnitTypes.dagger, UnitTypes.mace
        };
        for (UnitType type : candidates) {
            if (type != null) {
                result.add(type);
            }
        }
        if (result.isEmpty() && Vars.content != null && Vars.content.units() != null) {
            for (UnitType type : Vars.content.units()) {
                if (type != null && result.size < 6) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    private static Color sampleItemColor(int idx) {
        return SAMPLE_ITEM_COLORS[idx % SAMPLE_ITEM_COLORS.length];
    }

    private static final class MockBarElement extends Element {
        private final float fraction;
        private final String label;
        private final Color fillColor;
        private final float scale;
        private final GlyphLayout glyphLayout = new GlyphLayout();

        MockBarElement(float fraction, String label, Color fillColor, float scale) {
            this.fraction = Mathf.clamp(fraction, 0f, 1f);
            this.label = label;
            this.fillColor = fillColor != null ? fillColor : Color.gold;
            this.scale = scale;
        }

        @Override
        public void draw() {
            if (Tex.whiteui == null) {
                return;
            }
            Draw.reset();
            Draw.color(Color.black, parentAlpha);
            Tex.whiteui.draw(x, y, width, height);

            float fillW = width * fraction;
            if (fillW > 0.5f) {
                Draw.color(fillColor.r, fillColor.g, fillColor.b, parentAlpha);
                Tex.whiteui.draw(x, y, fillW, height);
            }

            Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
            if (font != null && label != null && !label.isEmpty()) {
                float origX = font.getScaleX();
                float origY = font.getScaleY();
                try {
                    font.getData().setScale(origX * scale * 0.72f, origY * scale * 0.72f);
                    glyphLayout.setText(font, label);
                    if (glyphLayout.width < width - 4f) {
                        font.setColor(1f, 1f, 1f, parentAlpha);
                        font.draw(label, x + width * 0.5f - glyphLayout.width * 0.5f,
                                y + height * 0.5f + glyphLayout.height * 0.5f);
                    }
                } finally {
                    font.getData().setScale(origX, origY);
                }
            }
            Draw.reset();
        }
    }
}
