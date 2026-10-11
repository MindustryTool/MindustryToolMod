package mindustrytool.components;

import static solim.UI.*;

import arc.scene.Element;

/**
 * Standardized settings scaffold providing the unified viewport structure:
 * - Top-aligned on Y-axis (top()) and centered on X-axis (center())
 * - Width-constrained to at most 500 units (maxWidth(500f))
 * - Inter-section gap of 3 units (gap(unit(3)))
 */
public final class SettingScaffold {

    private SettingScaffold() {
    }

    /**
     * Builds the standard setting view root element enclosing the given section cards.
     */
    public static Element build(Runnable content) {
        return column().grow().top().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().top().center().gap(unit(3)).padding(unit(2)).children(content);
            });
        }).element();
    }
}
