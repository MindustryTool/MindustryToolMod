package mindustrytool.features.teamresource;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import mindustry.ui.Fonts;

public class SparklineWidget extends Element {

    private final HistoryBuffer buffer;
    private final Color lineColor;
    private final GlyphLayout layout = new GlyphLayout();

    public SparklineWidget(HistoryBuffer buffer, Color lineColor) {
        this.buffer = buffer;
        this.lineColor = lineColor;
    }

    @Override
    public float getPrefWidth() {
        return 160f;
    }

    @Override
    public float getPrefHeight() {
        return 50f;
    }

    @Override
    public void draw() {
        Draw.reset();
        int size = buffer.size();
        if (size < 2) {
            return;
        }

        float min = buffer.min();
        float max = buffer.max();
        float range = max - min;
        if (range <= 0.0001f) {
            range = 1f;
        }

        float paddingLeft = 32f;
        float paddingRight = 6f;
        float paddingTop = 8f;
        float paddingBottom = 8f;

        float chartX = x + paddingLeft;
        float chartY = y + paddingBottom;
        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        if (chartWidth <= 0f || chartHeight <= 0f) {
            return;
        }

        Draw.color(Color.darkGray, 0.4f);
        Lines.stroke(1f);
        Lines.line(chartX, chartY, chartX + chartWidth, chartY);
        Lines.line(chartX, chartY + chartHeight, chartX + chartWidth, chartY + chartHeight);

        Font font = Fonts.outline;
        float prevScaleX = font.getScaleX();
        float prevScaleY = font.getScaleY();
        try {
            font.getData().setScale(0.55f);
            font.setColor(Color.lightGray);

            String maxStr = TeamResourceState.formatCompactAmount((long) max);
            layout.setText(font, maxStr);
            font.draw(maxStr, x + paddingLeft - layout.width - 4f, chartY + chartHeight + layout.height / 2f);

            String minStr = TeamResourceState.formatCompactAmount((long) min);
            layout.setText(font, minStr);
            font.draw(minStr, x + paddingLeft - layout.width - 4f, chartY + layout.height / 2f);
        } finally {
            font.getData().setScale(prevScaleX, prevScaleY);
        }

        Draw.color(lineColor);
        Lines.stroke(1.5f);

        float stepX = chartWidth / (HistoryBuffer.CAPACITY - 1);
        float startOffset = (HistoryBuffer.CAPACITY - size) * stepX;

        float prevX = chartX + startOffset;
        float prevVal = buffer.get(0);
        float prevY = chartY + ((prevVal - min) / range) * chartHeight;

        for (int i = 1; i < size; i++) {
            float curVal = buffer.get(i);
            float curX = chartX + startOffset + i * stepX;
            float curY = chartY + ((curVal - min) / range) * chartHeight;
            Lines.line(prevX, prevY, curX, curY);
            prevX = curX;
            prevY = curY;
        }

        Draw.reset();
    }
}
