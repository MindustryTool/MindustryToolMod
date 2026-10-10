package mindustrytool.features.browser.patch;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.ContentPatchData;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Text and metadata focused card layout for content patches.
 * Displays title, type badge, description snippet, and action buttons.
 */
public class PatchCard extends BaseComponent {

    public static final float CARD_SIZE = BrowserLayout.CARD_SIZE;

    private final ContentPatchData patch;
    private final @Nullable Readable<Float> cardHeight;
    private final Runnable onClick;
    private final Runnable onCopy;
    private final Runnable onSave;
    private final Runnable onDetails;

    public PatchCard(
            ContentPatchData patch,
            Runnable onClick,
            Runnable onCopy,
            Runnable onSave,
            Runnable onDetails) {
        this(patch, null, onClick, onCopy, onSave, onDetails);
    }

    public PatchCard(
            ContentPatchData patch,
            @Nullable Readable<Float> cardHeight,
            Runnable onClick,
            Runnable onCopy,
            Runnable onSave,
            Runnable onDetails) {
        this.patch = patch;
        this.cardHeight = cardHeight;
        this.onClick = onClick;
        this.onCopy = onCopy;
        this.onSave = onSave;
        this.onDetails = onDetails;
    }

    @Override
    protected Element build() {
        String title = patch.getName() != null && !patch.getName().trim().isEmpty()
                ? patch.getName().trim()
                : Core.bundle.get("browser.patch.unnamed");

        boolean isPack = "DATAPACK".equalsIgnoreCase(patch.getType());
        String typeLabel = isPack
                ? Core.bundle.get("browser.patch.type.datapack")
                : Core.bundle.get("browser.patch.type.data");
        Color typeColor = isPack ? Color.royal : Color.coral;

        String description = patch.getDescription() != null && !patch.getDescription().trim().isEmpty()
                ? patch.getDescription().trim()
                : "";

        String likesText = BrowserStatsBadge.formatCount(BrowserImages.count(patch.getLikes()));
        String commentsText = BrowserStatsBadge.formatCount(BrowserImages.count(patch.getComments()));
        String downloadsText = BrowserStatsBadge.formatCount(BrowserImages.count(patch.getDownloads()));

        Readable<Float> size = cardHeight != null ? cardHeight : Readable.of(CARD_SIZE);

        return column()
                .name("PatchCard-" + patch.getItemId())
                .width(size)
                .gap(unit(2))
                .children(() -> {
                    card(WebStyles.previewCardBackground())
                            .name("PatchCard-content-" + patch.getItemId())
                            .size(size)
                            .onClick(onClick)
                            .padding(unit(3))
                            .children(() -> column().grow().gap(unit(2)).children(() -> {
                                // Header: Type Badge + Title
                                row().growX().gap(unit(1.5f)).children(() -> {
                                    card(WebStyles.previewCardBackground())
                                            .padding(unit(0.5f), unit(1.5f), unit(0.5f), unit(1.5f))
                                            .children(() -> text(typeLabel)
                                                    .style(Styles.defaultLabel)
                                                    .fontScale(0.75f)
                                                    .color(typeColor));

                                    text(title)
                                            .style(Styles.defaultLabel)
                                            .color(Color.white)
                                            .ellipsis(true)
                                            .growX();
                                });

                                // Body: Description
                                row().grow().top().left().children(() -> text(description)
                                        .style(Styles.defaultLabel)
                                        .fontScale(0.85f)
                                        .color(Color.lightGray)
                                        .left()
                                        .wrap()
                                        .growX());
                            }));

                    // Action buttons row
                    row().width(size).gap(unit(1.5f)).children(() -> {
                        statButton(
                                likesText,
                                FileIcon.of("heart.png", Icon.upOpenSmall),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.patch.details"));
                        statButton(
                                commentsText,
                                FileIcon.of("message-circle.png"),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.patch.details"));
                        statButton(
                                downloadsText,
                                Icon.downloadSmall,
                                patch.getDownloads() != null && patch.getDownloads() > 0 ? Color.sky : Color.white,
                                onSave,
                                Core.bundle.get("browser.patch.save"));

                        button(onCopy)
                                .style(WebStyles.cardActionText())
                                .growX()
                                .height(unit(9))
                                .tooltip(Core.bundle.get("browser.patch.copy"))
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
