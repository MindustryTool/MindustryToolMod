package mindustrytool.features.teamresource;

import arc.func.Floatp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.core.UI;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.world.blocks.power.PowerGraph;

public class SplitBar extends Element {
    private final Seq<PowerGraph> graphs;
    private final Mode mode;
    private final float fontScale;
    private final Floatp scaleSupplier;
    private final GlyphLayout layout = new GlyphLayout();

    public enum Mode {
        SATISFACTION,
        STORED
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, float fontScale) {
        this(graphs, mode, () -> fontScale);
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, Floatp scaleSupplier) {
        this.graphs = graphs;
        this.mode = mode;
        this.scaleSupplier = scaleSupplier;
        this.fontScale = scaleSupplier != null ? scaleSupplier.get() : 1f;
        this.userObject = "expanding";
    }

    @Override
    public float getPrefHeight() {
        return 20f * (scaleSupplier != null ? scaleSupplier.get() : fontScale);
    }

    @Override
    public float getPrefWidth() {
        return 140f;
    }

    @Override
    public void draw() {
        Draw.reset();

        Draw.color(Color.black);
        Tex.whiteui.draw(x, y, width, height);

        if (graphs.isEmpty()) {
            return;
        }

        float totalWeight = 0f;
        for (PowerGraph graph : graphs) {
            totalWeight += getWeight(graph);
        }

        if (totalWeight <= 0.0001f) {
            return;
        }

        float currentX = x;
        Font font = Fonts.outline;

        float originalScaleX = font.getScaleX();
        float originalScaleY = font.getScaleY();

        try {
            float currentScale = scaleSupplier != null ? scaleSupplier.get() : fontScale;
            font.getData().setScale(originalScaleX * currentScale * 0.8f, originalScaleY * currentScale * 0.8f);

            for (int i = 0; i < graphs.size; i++) {
                PowerGraph graph = graphs.get(i);
                float weight = getWeight(graph);
                if (weight <= 0f) {
                    continue;
                }

                float sectionWidth = width * (weight / totalWeight);
                float fraction = getFraction(graph);

                if (fraction > 0.01f) {
                    if (mode == Mode.SATISFACTION && graph.getPowerBalance() < 0f) {
                        Draw.color(Color.scarlet);
                    } else {
                        Draw.color(Pal.powerBar);
                    }

                    float fillWidth = sectionWidth * fraction;
                    if (fillWidth > 0.5f) {
                        Tex.whiteui.draw(currentX, y, fillWidth, height);
                    }
                }

                if (i < graphs.size - 1) {
                    Draw.color(Color.black);
                    Draw.alpha(0.5f);
                    Tex.whiteui.draw(currentX + sectionWidth - 1f, y, 2f, height);
                }

                String text = getSectionText(graph);
                if (!text.isEmpty()) {
                    layout.setText(font, text);

                    if (layout.width < sectionWidth - 4f) {
                        font.setColor(Color.white);
                        font.draw(text, currentX + sectionWidth / 2f - layout.width / 2f,
                                y + height / 2f + layout.height / 2f);
                    }
                }

                currentX += sectionWidth;
            }
        } finally {
            font.getData().setScale(originalScaleX, originalScaleY);
        }

        Draw.reset();
    }

    private float getWeight(PowerGraph graph) {
        if (mode == Mode.SATISFACTION) {
            return Math.max(graph.getLastPowerProduced(), graph.getLastPowerNeeded());
        } else {
            return graph.getLastCapacity();
        }
    }

    private float getFraction(PowerGraph graph) {
        if (mode == Mode.SATISFACTION) {
            return graph.getSatisfaction();
        } else {
            return graph.getLastCapacity() <= 0f ? 0f : graph.getLastPowerStored() / graph.getLastCapacity();
        }
    }

    private String getSectionText(PowerGraph graph) {
        if (mode == Mode.SATISFACTION) {
            float balance = graph.getPowerBalance();
            return (balance >= 0f ? "+" : "") + UI.formatAmount((long) (balance * 60f));
        } else {
            return UI.formatAmount((long) graph.getLastPowerStored());
        }
    }
}
