package mindustrytool.features.godmode;

import arc.scene.ui.layout.Table;

public interface GodModeProvider {
    void build(Table table);

    boolean isAvailable();
}
