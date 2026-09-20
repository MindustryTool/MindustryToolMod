package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.SchematicData;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Image-first card layout showing a dedicated preview card with a centered
 * title strip and a compact row of interactive stat action buttons.
 */
public class SchematicCard extends BaseComponent {

    public static final float CARD_SIZE = BrowserLayout.CARD_SIZE;

    private final SchematicData schematic;
    private final @Nullable Readable<Float> previewHeight;
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
        this(schematic, null, onClick, onCopy, onSave, onDetails);
    }

    public SchematicCard(
            SchematicData schematic,
            @Nullable Readable<Float> previewHeight,
            Runnable onClick,
            Runnable onCopy,
            Runnable onSave,
            Runnable onDetails) {
        this.schematic = schematic;
        this.previewHeight = previewHeight;
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
        String likesText = BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getLikes()));
        String commentsText = BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getComments()));
        String downloadsText = BrowserStatsBadge.formatCount(BrowserImages.count(schematic.getDownloads()));

        Readable<Float> size = previewHeight != null ? previewHeight : Readable.of(CARD_SIZE);

        return column()
                .name("SchematicCard-" + schematic.getItemId())
                .width(size)
                .gap(unit(2))
                .children(() -> {
                    card(WebStyles.previewCardBackground())
                            .name("SchematicCard-preview-" + schematic.getItemId())
                            .size(size)
                            .onClick(onClick)
                            .children(() -> {
                                stack().grow()
                                        .layer(() -> networkImage()
                                                .rounded(8)
                                                .scaling(Scaling.fit)
                                                .grow()
                                                .url(imageUrl)
                                                .placeholder(Icon.image)
                                                .fallback(Icon.image))
                                        .layer(() -> column().grow().children(() -> {
                                            spacer();
                                            row().growX()
                                                    .background(Styles.black8)
                                                    .padding(unit(1.5f), unit(2f), unit(1.5f), unit(2f))
                                                    .center()
                                                    .children(() -> {
                                                        text(title)
                                                                .style(Styles.defaultLabel)
                                                                .color(Color.white)
                                                                .ellipsis(true)
                                                                .center()
                                                                .growX();
                                                    });
                                        }));
                            });

                    row().width(size).gap(unit(1.5f)).children(() -> {
                        statButton(
                                likesText,
                                FileIcon.of("heart.png", Icon.upOpenSmall),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.schematic.details"));
                        statButton(
                                commentsText,
                                FileIcon.of("message-circle.png"),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.schematic.details"));
                        statButton(
                                downloadsText,
                                Icon.downloadSmall,
                                schematic.getDownloads() != null && schematic.getDownloads() > 0 ? Color.sky
                                        : Color.white,
                                onSave,
                                Core.bundle.get("browser.schematic.save"));

                        button(onCopy)
                                .style(WebStyles.cardActionText())
                                .growX()
                                .height(unit(9))
                                .tooltip(Core.bundle.get("browser.schematic.copy"))
                                .children(() -> icon(Icon.copy).size(unit(4.5f)).color(Color.white));
                    });
                }).element();
    }

    private void statButton(String count, Drawable iconDrawable, Color textColor, Runnable action, String tooltip) {
        button(action)
                .style(WebStyles.cardActionText())
                .growX()
                .height(unit(9))
                .gap(unit(1))
                .tooltip(tooltip)
                .children(() -> {
                    icon(iconDrawable).size(unit(4.5f)).color(Color.white).marginRight(unit(1));
                    text(count).style(Styles.defaultLabel).fontScale(0.85f).color(textColor);
                });
    }
}
