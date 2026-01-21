package mindustrytool.features.display.teamresource;

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

    public enum Mode {
        SATISFACTION,
        STORED
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, float fontScale) {
        this.graphs = graphs;
        this.mode = mode;
        this.fontScale = fontScale;
    }

    @Override
    public void draw() {
        Draw.color(Color.black);
        Tex.barTop.draw(x, y, width, height);

        if (graphs.isEmpty()) {
            return;
        }

        float totalWeight = 0f;
        for (PowerGraph graph : graphs) {
            totalWeight += getWeight(graph);
        }

        if (totalWeight <= 0.0001f) {
            
            
            
        } else {
            float currentX = x;

            Font font = Fonts.outline;
            float originalScaleX = font.getScaleX();
            float originalScaleY = font.getScaleY();
            
            font.getData().setScale(originalScaleX * fontScale * 0.8f, originalScaleY * fontScale * 0.8f);

            for (int i = 0; i < graphs.size; i++) {
                PowerGraph graph = graphs.get(i);
                float weight = getWeight(graph);
                if (weight <= 0)
                    continue;

                float sectionWidth = width * (weight / totalWeight);
                float fraction = getFraction(graph);

                
                if (fraction > 0.01f) {
                    Draw.color(Pal.powerBar);
                    float fillWidth = sectionWidth * fraction;
                    if (fillWidth > 0.5f) {
                        Tex.bar.draw(currentX, y, fillWidth, height);
                    }
                }

                
                if (i < graphs.size - 1) {
                    Draw.color(Color.black);
                    Draw.alpha(0.5f);
                    Draw.rect("whiteui", currentX + sectionWidth, y + height / 2f, 2f, height);
                }

                
                String text = getSectionText(graph);
                if (!text.isEmpty()) {
                    GlyphLayout layout = new GlyphLayout();
                    layout.setText(font, text);

                    
                    if (layout.width < sectionWidth - 4f) {
                        font.setColor(Color.white);
                        font.draw(text, currentX + sectionWidth / 2f - layout.width / 2f,
                                y + height / 2f + layout.height / 2f);
                    }
                }

                currentX += sectionWidth;
            }

            font.getData().setScale(originalScaleX, originalScaleY);
            Draw.reset();
        }
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
            if (graph.getLastCapacity() <= 0)
                return 0f;
            return graph.getLastPowerStored() / graph.getLastCapacity();
        }
    }

    private String getSectionText(PowerGraph graph) {
        if (mode == Mode.SATISFACTION) {
            float balance = graph.getPowerBalance();
            return (balance >= 0 ? "+" : "") + UI.formatAmount((long) (balance * 60));
        } else {
            return UI.formatAmount((long) graph.getLastPowerStored());
        }
    }
}
