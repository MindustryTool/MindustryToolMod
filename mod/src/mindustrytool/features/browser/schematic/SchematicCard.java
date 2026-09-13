package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.SchematicData;
import solim.core.BaseComponent;

/**
 * Hero image-first card showing a large schematic preview with the title
 * overlaid on a translucent bottom bar and a single row of compact
 * interactive action buttons with stat counts.
 */
public class SchematicCard extends BaseComponent {

    private final SchematicData schematic;
    private final Runnable onClick;
    private final Runnable onCopy;
    private final Runnable onSave;
    private final Runnable onDetails;

    public SchematicCard(
            SchematicData schematic,
            Runnable onClick,
            Runnable onCopy,
            Runnable onSave,
            Runnable onDetails) {
        this.schematic = schematic;
        this.onClick = onClick;
        this.onCopy = onCopy;
        this.onSave = onSave;
        this.onDetails = onDetails;
    }

    @Override
    protected Element build() {
        String imageUrl = BrowserImages.schematicPreviewUrl(schematic.getItemId());
        String title = schematic.getName() != null ? schematic.getName()
                : Core.bundle.get("browser.schematic.unnamed");

        return card(Styles.black8)
                .name("SchematicCard-" + schematic.getItemId())
                .growX()
                .rounded(6)
                .border(1f, Color.darkGray)
                .onClick(onClick)
                .children(() -> {
                    column().growX().padding(unit(2)).gap(unit(2)).children(() -> {
                        stack().growX().children(() -> {
                            networkImage(imageUrl)
                                    .placeholder(Icon.image)
                                    .fallback(Icon.image)
                                    .growX()
                                    .height(unit(38))
                                    .rounded(4)
                                    .scaling(Scaling.fit);
                        }).children(() -> {
                            column().grow().children(() -> {
                                spacer();
                                row().growX().background(Styles.black8).padding(unit(1)).children(() -> {
                                    text(title)
                                            .style(Styles.defaultLabel)
                                            .color(Color.white)
                                            .ellipsis(true)
                                            .left()
                                            .growX();
                                });
                            });
                        });

                        row().growX().gap(unit(1)).children(() -> {
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getLikes())),
                                    Icon.upOpenSmall, Color.scarlet, onDetails,
                                    Core.bundle.get("browser.schematic.details"));
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getComments())),
                                    Icon.chatSmall, Color.lightGray, onDetails,
                                    Core.bundle.get("browser.schematic.details"));
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getDownloads())),
                                    Icon.downloadSmall, Color.sky, onSave,
                                    Core.bundle.get("browser.schematic.save"));

                            button(onCopy).style(WebStyles.outlineText()).growX().height(unit(10))
                                    .tooltip(Core.bundle.get("browser.schematic.copy"))
                                    .children(() -> icon(Icon.copy).size(unit(5)));
                        });
                    });
                }).element();
    }

    private void statButton(String count, Drawable icon, Color tint, Runnable action, String tooltip) {
        button(action)
                .style(WebStyles.outlineText())
                .growX()
                .height(unit(10))
                .tooltip(tooltip)
                .children(() -> {
                    icon(icon).size(unit(5)).color(tint);
                    text(count).style(Styles.defaultLabel).fontScale(0.9f);
                });
    }
}
