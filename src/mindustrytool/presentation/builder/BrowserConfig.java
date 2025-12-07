package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.layout.Scl;
import mindustrytool.domain.service.ContentType;

public class BrowserConfig {
    private static final float IMG_SIZE = 210, INFO_H = 60;

    public static int calculateItemsPerPage(ContentType type) {
        float itemSize = type == ContentType.MAP ? IMG_SIZE : Scl.scl(IMG_SIZE);
        int cols = (int) (Core.graphics.getWidth() / Scl.scl(itemSize));
        int rows = (int) (Core.graphics.getHeight() / Scl.scl(IMG_SIZE + INFO_H * 2));
        return Math.max(cols * rows, 20);
    }
}
