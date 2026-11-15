package mindustrytool.features.resourcemonitor;

import arc.func.Floatp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.scene.Element;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.core.UI;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.world.blocks.power.PowerGraph;

public class SplitBar extends Element {

    private static final float MIN_SECTION_SHARE = 0.03f;

    public enum Mode {
        SATISFACTION,
        STORED
    }

    private final Seq<PowerGraph> graphs;
    private final Mode mode;
    private final float fontScale;
    private final @Nullable Floatp scaleSupplier;
    private final GlyphLayout layout = new GlyphLayout();
    private final ObjectFloatMap<PowerGraph> smoothFractions = new ObjectFloatMap<>();
    private final ObjectFloatMap<PowerGraph> smoothWeights = new ObjectFloatMap<>();
    private final ObjectMap<PowerGraph, SectionTextEntry> sectionTextCache = new ObjectMap<>();
    private final Seq<PowerGraph> visibleGraphs = new Seq<>();

    private static final class SectionTextEntry {
        long val = Long.MIN_VALUE;
        boolean positive = false;
        String text = "";
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, float fontScale) {
        this(graphs, mode, () -> fontScale);
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, @Nullable Floatp scaleSupplier) {
        this.graphs = graphs;
        this.mode = mode;
        this.scaleSupplier = scaleSupplier;
        this.fontScale = scaleSupplier != null ? scaleSupplier.get() : 1f;
    }

    @Override
    public float getPrefHeight() {
        return 20f * scale();
    }

    @Override
    public float getPrefWidth() {
        return 140f;
    }

    private float scale() {
        return scaleSupplier != null ? scaleSupplier.get() : fontScale;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (smoothFractions.size > graphs.size * 2 && smoothFractions.size > 16) {
            smoothFractions.clear();
            smoothWeights.clear();
            sectionTextCache.clear();
        }
        float speed = Math.min(1f, delta * 10f);
        for (int i = 0; i < graphs.size; i++) {
            PowerGraph graph = graphs.get(i);
            float targetFrac = Mathf.clamp(getFraction(graph), 0f, 1f);
            float prevFrac = smoothFractions.get(graph, targetFrac);
            smoothFractions.put(graph, Mathf.lerp(prevFrac, targetFrac, speed));

            float targetWeight = Math.max(0f, getWeight(graph));
            float prevWeight = smoothWeights.get(graph, targetWeight);
            smoothWeights.put(graph, Mathf.lerp(prevWeight, targetWeight, speed));
        }
    }

    @Override
    public void draw() {
        if (Tex.whiteui == null) {
            return;
        }
        Draw.reset();

        float alpha = this.color.a * parentAlpha;
        Draw.color(0f, 0f, 0f, alpha);
        Tex.whiteui.draw(x, y, width, height);

        if (graphs.isEmpty()) {
            Draw.reset();
            return;
        }

        float rawTotalWeight = 0f;
        for (int i = 0; i < graphs.size; i++) {
            PowerGraph graph = graphs.get(i);
            float w = smoothWeights.get(graph, getWeight(graph));
            if (w > 0f) {
                rawTotalWeight += w;
            }
        }

        if (rawTotalWeight <= 0.0001f) {
            Draw.reset();
            return;
        }

        visibleGraphs.clear();
        float visibleWeight = 0f;
        for (int i = 0; i < graphs.size; i++) {
            PowerGraph graph = graphs.get(i);
            float w = smoothWeights.get(graph, getWeight(graph));
            if (w > 0f && (w / rawTotalWeight >= MIN_SECTION_SHARE || graphs.size == 1)) {
                visibleGraphs.add(graph);
                visibleWeight += w;
            }
        }

        if (visibleGraphs.isEmpty()) {
            for (int i = 0; i < graphs.size; i++) {
                PowerGraph graph = graphs.get(i);
                float w = smoothWeights.get(graph, getWeight(graph));
                if (w > 0f) {
                    visibleGraphs.add(graph);
                    visibleWeight += w;
                }
            }
        }

        if (visibleGraphs.isEmpty() || visibleWeight <= 0.0001f) {
            Draw.reset();
            return;
        }

        float currentX = x;
        float rightEdge = x + width;
        Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;

        float originalScaleX = font != null ? font.getScaleX() : 1f;
        float originalScaleY = font != null ? font.getScaleY() : 1f;

        try {
            if (font != null) {
                float currentScale = scale();
                font.getData().setScale(originalScaleX * currentScale * 0.8f, originalScaleY * currentScale * 0.8f);
            }

            for (int i = 0; i < visibleGraphs.size; i++) {
                PowerGraph graph = visibleGraphs.get(i);
                float weight = smoothWeights.get(graph, getWeight(graph));
                boolean isLast = (i == visibleGraphs.size - 1);

                float sectionWidth = isLast
                        ? Math.max(0f, rightEdge - currentX)
                        : width * (weight / visibleWeight);
                float fraction = Mathf.clamp(smoothFractions.get(graph, getFraction(graph)), 0f, 1f);

                if (fraction > 0.01f) {
                    Color barColor = (mode == Mode.SATISFACTION && graph.getPowerBalance() < 0f)
                            ? Color.scarlet
                            : Pal.powerBar;
                    Draw.color(barColor.r, barColor.g, barColor.b, alpha);

                    float fillWidth = sectionWidth * fraction;
                    if (fillWidth > 0.5f) {
                        Tex.whiteui.draw(currentX, y, fillWidth, height);
                    }
                }

                if (!isLast && sectionWidth >= 4f) {
                    Draw.color(0f, 0f, 0f, 0.65f * alpha);
                    Tex.whiteui.draw(currentX + sectionWidth - 1f, y, 2f, height);
                }

                if (font != null) {
                    String text = getSectionText(graph);
                    if (!text.isEmpty()) {
                        layout.setText(font, text);

                        if (layout.width < sectionWidth - 4f) {
                            font.setColor(1f, 1f, 1f, alpha);
                            font.draw(text, currentX + sectionWidth / 2f - layout.width / 2f,
                                    y + height / 2f + layout.height / 2f);
                        }
                    }
                }

                currentX += sectionWidth;
            }
        } finally {
            if (font != null) {
                font.getData().setScale(originalScaleX, originalScaleY);
            }
        }

        Draw.reset();
    }

    private float getWeight(PowerGraph graph) {
        return mode == Mode.SATISFACTION
                ? Math.max(graph.getLastPowerProduced(), graph.getLastPowerNeeded())
                : graph.getLastCapacity();
    }

    private float getFraction(PowerGraph graph) {
        return mode == Mode.SATISFACTION
                ? graph.getSatisfaction()
                : (graph.getLastCapacity() <= 0f ? 0f : graph.getLastPowerStored() / graph.getLastCapacity());
    }

    private String getSectionText(PowerGraph graph) {
        SectionTextEntry entry = sectionTextCache.get(graph);
        if (entry == null) {
            entry = new SectionTextEntry();
            sectionTextCache.put(graph, entry);
        }
        if (mode == Mode.SATISFACTION) {
            float balance = graph.getPowerBalance();
            long val = (long) (balance * 60f);
            boolean pos = balance >= 0f;
            if (entry.val != val || entry.positive != pos || entry.text.isEmpty()) {
                entry.val = val;
                entry.positive = pos;
                entry.text = (pos ? "+" : "") + UI.formatAmount(val);
            }
            return entry.text;
        }
        long val = (long) graph.getLastPowerStored();
        if (entry.val != val || entry.text.isEmpty()) {
            entry.val = val;
            entry.text = UI.formatAmount(val);
        }
        return entry.text;
    }
}
