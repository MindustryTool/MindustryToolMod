package mindustrytool.components;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.maxWidth;
import static solim.modifier.Values.padding;

import arc.scene.Element;

public final class SettingScaffold {

    private SettingScaffold() {
    }

    public static Element build(Runnable content) {
        return column().props(grow(), maxWidth(500f), top(), center()).children(() -> {
            scroll().props(grow()).scrollX(false).children(() -> {
                column().props(growX(), padding(unit(2)), top(), center(), gap(unit(3))).children(content);
            });
        }).element();
    }
}
