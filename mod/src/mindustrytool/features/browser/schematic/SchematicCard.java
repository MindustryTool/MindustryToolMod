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
import solim.core.Component;
import solim.reactive.Readable;

public class SchematicCard extends Component {

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
                .props(prefWidth(size), gap(unit(2)))
                .children(() -> {
                    card(WebStyles.previewCardBackground())
                            .name("SchematicCard-preview-" + schematic.getItemId())
                            .props(prefWidth(size), prefHeight(size), onClick(onClick))
                            .children(() -> {
                                stack().props(grow())
                                        .layer(() -> image()
                                                .props(
                                                        grow(),
                                                        imageRounded(8),
                                                        scaling(Scaling.fit),
                                                        src(imageUrl),
                                                        placeholder(Icon.image),
                                                        fallback(Icon.image)
                                                ))
                                        .layer(() -> column().props(grow()).children(() -> {
                                            spacer();
                                            row().props(growX(), background(Styles.black8), padding(unit(1.5f), unit(2f), unit(1.5f), unit(2f)), center())
                                                    .children(() -> {
                                                        text(title)
                                                                .props(style(Styles.defaultLabel), color(Color.white), ellipsis(true), center(), growX());
                                                    });
                                        }));
                            });

                    row().props(prefWidth(size), gap(unit(1.5f))).children(() -> {
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
                                .props(style(WebStyles.actionCardText()), growX(), prefHeight(unit(9)), tooltip(Core.bundle.get("browser.schematic.copy")))
                                .children(() -> icon(Icon.copy).props(color(Color.white), prefWidth(unit(4.5f)), prefHeight(unit(4.5f))));
                    });
                }).element();
    }

    private void statButton(String count, Drawable iconDrawable, Color textColor, Runnable action, String tooltip) {
        button(action)
                .props(style(WebStyles.actionCardText()), growX(), prefHeight(unit(9)), gap(unit(1)), tooltip(tooltip))
                .children(() -> {
                    icon(iconDrawable).props(color(Color.white), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)), marginRight(unit(1)));
                    text(count).props(style(Styles.defaultLabel), color(textColor), fontScale(0.85f));
                });
    }
}
