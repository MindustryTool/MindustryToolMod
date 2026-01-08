package mindustrytool.features.display.teamresource;

import arc.func.Floatp;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;

public class ScaledBar extends Bar {
    private final float fontScale;

    public ScaledBar(Prov<CharSequence> name, Prov<Color> color,
            Floatp fraction, float fontScale) {
        super(name, color, fraction);
        this.fontScale = fontScale;
    }

    @Override
    public void draw() {
        Font font = Fonts.outline;
        float originalScaleX = font.getScaleX();
        float originalScaleY = font.getScaleY();
        Color originalColor = new Color(font.getColor());

        font.getData().setScale(originalScaleX * fontScale, originalScaleY * fontScale);

        super.draw();

        font.getData().setScale(originalScaleX, originalScaleY);
        font.setColor(originalColor);
    }
}
