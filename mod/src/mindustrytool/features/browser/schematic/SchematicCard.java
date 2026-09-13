package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.SchematicData;
import solim.core.BaseComponent;

/**
 * Card showing a schematic preview thumbnail, title, stats, and quick actions
 * for copy, save, and details.
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

        return card(Styles.black8)
                .name("SchematicCard-" + schematic.getItemId())
                .growX()
                .rounded(6)
                .border(1f, Color.darkGray)
                .onClick(onClick)
                .children(() -> {
                    column().growX().padding(unit(2)).gap(unit(1)).children(() -> {
                        networkImage(imageUrl)
                                .growX()
                                .height(unit(30))
                                .rounded(4)
                                .scaling(Scaling.fit);

                        text(schematic.getName() != null ? schematic.getName()
                                : Core.bundle.get("browser.schematic.unnamed"))
                                .style(Styles.defaultLabel)
                                .color(Color.white)
                                .ellipsis(true)
                                .left()
                                .growX();

                        new BrowserStatsBadge(
                                BrowserImages.count(schematic.getLikes()),
                                BrowserImages.count(schematic.getComments()),
                                BrowserImages.count(schematic.getDownloads()));

                        row().growX().gap(unit(1)).children(() -> {
                            button(onCopy).style(WebStyles.outlineText()).size(unit(10))
                                    .tooltip(Core.bundle.get("browser.schematic.copy"))
                                    .children(() -> icon(Icon.copy).size(unit(5)));

                            button(onSave).style(WebStyles.outlineText()).size(unit(10))
                                    .tooltip(Core.bundle.get("browser.schematic.save"))
                                    .children(() -> icon(Icon.save).size(unit(5)));

                            spacer();

                            button(onDetails).style(WebStyles.outlineText()).size(unit(10))
                                    .tooltip(Core.bundle.get("browser.schematic.details"))
                                    .children(() -> icon(Icon.infoCircle).size(unit(5)));
                        });
                    });
                }).element();
    }
}
