package mindustrytool.components;

import static solim.UI.*;

import arc.scene.Element;

public final class SettingScaffold {

    private SettingScaffold() {
    }

    public static Element build(Runnable content) {
        return column().grow().top().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().top().center().gap(unit(3)).padding(unit(2)).children(content);
            });
        }).element();
    }
}
