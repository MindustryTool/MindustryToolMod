package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.func.Floatp;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;
import solim.core.SolimElement;

public class SplitBar extends SolimElement {

    private final Bar bar;

    public enum Mode {
        SATISFACTION,
        STORED,
        UNIFIED
    }

    public SplitBar(TeamResourceState state, @Nullable Floatp scaleSupplier) {
        bar = new Bar(state, scaleSupplier);
        props(growX());
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, float fontScale) {
        this(graphs, mode, () -> fontScale);
    }

    public SplitBar(Seq<PowerGraph> graphs, Mode mode, @Nullable Floatp scaleSupplier) {
        bar = new Bar(graphs, mode, scaleSupplier);
        props(growX());
    }

    @Override
    protected Element createElement() {
        return bar;
    }

    private static final class Bar extends Element {

        private final @Nullable TeamResourceState state;
        private final Seq<PowerGraph> graphs;
        private final Mode mode;
        private final float fontScale;
        private final @Nullable Floatp scaleSupplier;
        private final GlyphLayout layout = new GlyphLayout();

        Bar(TeamResourceState state, @Nullable Floatp scaleSupplier) {
            this.state = state;
            this.graphs = state.getTeamGraphs();
            this.mode = Mode.UNIFIED;
            this.scaleSupplier = scaleSupplier;
            this.fontScale = scaleSupplier != null ? scaleSupplier.get() : 1f;
            attachTooltip();
        }

        Bar(Seq<PowerGraph> graphs, Mode mode, @Nullable Floatp scaleSupplier) {
            this.state = null;
            this.graphs = graphs;
            this.mode = mode;
            this.scaleSupplier = scaleSupplier;
            this.fontScale = scaleSupplier != null ? scaleSupplier.get() : 1f;
        }

        private void attachTooltip() {
            if (state == null) {
                return;
            }
            addListener(new Tooltip(table -> {
                table.background(Styles.black8).margin(8f);
                table.update(() -> {
                    table.clearChildren();
                    table.left();

                    float stored = state.getTotalStoredPower();
                    float cap = state.getTotalPowerCapacity();
                    float balance = state.getTotalPowerBalance() * 60f;

                    String storedText = TeamResourceState.formatCompactAmount((long) stored)
                            + " / " + TeamResourceState.formatCompactAmount((long) cap);
                    String balanceText = (balance >= 0f ? "+" : "")
                            + TeamResourceState.formatCompactAmount((long) balance) + "/s";

                    Table header = new Table();
                    header.add("Power: ").color(Color.lightGray);
                    header.add(storedText).color(Color.white);
                    header.add(" (" + balanceText + ")").color(balance >= 0f ? Pal.heal : Pal.remove);
                    table.add(header).left().row();

                    HistoryBuffer powerHistory = state.getTotalPowerHistory();
                    if (powerHistory.size() > 1) {
                        Color chartColor = balance >= 0f ? Pal.powerBar : Pal.remove;
                        table.add(new SparklineWidget(powerHistory, chartColor)).padTop(4f).row();
                    }
                });
            }));
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
        public void draw() {
            Draw.reset();

            Draw.color(Color.black);
            Tex.whiteui.draw(x, y, width, height);

            float totalWeight = 0f;
            for (int i = 0; i < graphs.size; i++) {
                totalWeight += getWeight(graphs.get(i));
            }

            if (graphs.isEmpty() || totalWeight <= 0.0001f) {
                Font font = Fonts.outline;
                float originalScaleX = font.getScaleX();
                float originalScaleY = font.getScaleY();
                try {
                    float currentScale = scale();
                    font.getData().setScale(originalScaleX * currentScale * 0.70f, originalScaleY * currentScale * 0.70f);
                    font.setColor(Color.gray);
                    String msg = Core.bundle.get("team-resources.no-power", "No power network");
                    layout.setText(font, msg);
                    if (layout.width < width - 4f) {
                        font.draw(msg, x + width / 2f - layout.width / 2f, y + height / 2f + layout.height / 2f);
                    }
                } finally {
                    font.getData().setScale(originalScaleX, originalScaleY);
                }
                Draw.reset();
                return;
            }

            float currentX = x;
            Font font = Fonts.outline;

            float originalScaleX = font.getScaleX();
            float originalScaleY = font.getScaleY();

            try {
                float currentScale = scale();
                font.getData().setScale(originalScaleX * currentScale * 0.75f, originalScaleY * currentScale * 0.75f);

                for (int i = 0; i < graphs.size; i++) {
                    PowerGraph graph = graphs.get(i);
                    float weight = getWeight(graph);
                    if (weight <= 0f) {
                        continue;
                    }

                    float sectionWidth = width * (weight / totalWeight);
                    float fraction = getFraction(graph);

                    if (fraction > 0.01f) {
                        if (graph.getPowerBalance() < 0f) {
                            Draw.color(Pal.remove);
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
                        Draw.alpha(0.6f);
                        Tex.whiteui.draw(currentX + sectionWidth - 1f, y, 2f, height);
                    }

                    String text = getSectionText(graph);
                    if (!text.isEmpty()) {
                        layout.setText(font, text);

                        if (layout.width < sectionWidth - 6f) {
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
            }
            float cap = graph.getLastCapacity();
            if (cap > 0f) {
                return cap;
            }
            return Math.max(1f, Math.max(graph.getLastPowerProduced(), graph.getLastPowerNeeded()));
        }

        private float getFraction(PowerGraph graph) {
            if (mode == Mode.SATISFACTION) {
                return Mathf.clamp(graph.getSatisfaction());
            }
            float cap = graph.getLastCapacity();
            if (cap > 0f) {
                return Mathf.clamp(graph.getLastPowerStored() / cap);
            }
            return Mathf.clamp(graph.getSatisfaction());
        }

        private String getSectionText(PowerGraph graph) {
            if (mode == Mode.SATISFACTION) {
                float balance = graph.getPowerBalance();
                return (balance >= 0f ? "+" : "") + TeamResourceState.formatCompactAmount((long) (balance * 60f));
            }
            float balance = graph.getPowerBalance() * 60f;
            String stored = TeamResourceState.formatCompactAmount((long) graph.getLastPowerStored());
            String net = (balance >= 0f ? "+" : "") + TeamResourceState.formatCompactAmount((long) balance);
            return stored + " " + net;
        }
    }
}
