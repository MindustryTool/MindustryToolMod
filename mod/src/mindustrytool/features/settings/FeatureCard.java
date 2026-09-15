package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import solim.core.BaseComponent;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Component responsible for building and managing a single feature's visual
 * card. Handles display of metadata, action shortcuts (help, settings, main
 * dialog), and state toggling with direct property reactivity using pure Solim
 * components.
 */
public class FeatureCard extends BaseComponent {

    private final Feature feature;
    private final Readable<Boolean> reorderAllowed;

    public FeatureCard(Feature feature, Readable<Boolean> reorderAllowed) {
        this.feature = feature;
        this.reorderAllowed = reorderAllowed;
    }

    @Override
    protected Element build() {
        var metadata = feature.getMetadata();
        boolean inDevelopment = metadata.isDevelopment();

        Readable<Color> statusColor = inDevelopment
                ? Readable.of(Color.orange)
                : feature.enabled().map(value -> Boolean.TRUE.equals(value) ? Color.green : Color.scarlet);

        Readable<String> statusText = inDevelopment
                ? Readable.of(Core.bundle.get("feature.status.in-development"))
                : feature.enabled().map(val -> Boolean.TRUE.equals(val) ? Core.bundle.get("feature.status.enabled")
                        : Core.bundle.get("feature.status.disabled"));

        Readable<Boolean> canMoveLeft = inDevelopment
                ? Readable.of(false)
                : Signal.computed(() -> Boolean.TRUE.equals(reorderAllowed.get())
                        && Boolean.TRUE.equals(FeatureManager.canMoveLeftSignal(feature).get()));

        Readable<Boolean> canMoveRight = inDevelopment
                ? Readable.of(false)
                : Signal.computed(() -> Boolean.TRUE.equals(reorderAllowed.get())
                        && Boolean.TRUE.equals(FeatureManager.canMoveRightSignal(feature).get()));

        return card()
                .name("FeatureCard-" + metadata.getId()).height(unit(60)).growX()
                .rounded(unit(4))
                .border(1.5f, Color.darkGray)
                .padding(unit(1))
                .backgroundColor(Color.black)
                .onClick(() -> {
                    if (!inDevelopment) {
                        feature.setEnabled(!feature.isEnabled());
                    }
                }).children(() -> {
                    column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                        row().growX().center().children(() -> {
                            icon(metadata.getIcon()).size(unit(7)).cellPaddingRight(unit(2));

                            text(feature.getName()).style(Styles.defaultLabel).color(Color.white).ellipsis(true).left();

                            spacer();

                            if (feature.getMainDialog() != null) {
                                button(() -> feature.getMainDialog().show()).style(WebStyles.ghost()).size(unit(11))
                                        .tooltip(Core.bundle.get("feature.button.open-dialog"))
                                        .children(() -> icon(Icon.linkSmall).size(unit(7)));
                            }

                            if (feature.getSettingDialog() != null) {
                                button(() -> feature.getSettingDialog().show()).style(WebStyles.ghost()).size(unit(11))
                                        .tooltip(Core.bundle.get("feature.button.settings"))
                                        .children(() -> icon(Icon.settings).size(unit(7)));
                            }

                            button(() -> new FeatureHelpDialog(feature).show()).style(WebStyles.ghost()).size(unit(11))
                                    .tooltip(Core.bundle.get("feature.button.help"))
                                    .children(() -> icon(FileIcon.of("info.png")).size(unit(7)));
                        });

                        text(feature.getDescription()).color(Color.lightGray).fontScale(0.9f).wrap(true).left();

                        spacer();

                        row().growX().center().children(() -> {
                            text(statusText).style(Styles.defaultLabel)
                                    .color(statusColor)
                                    .left();

                            spacer();

                            if (!inDevelopment) {
                                button(() -> FeatureManager.moveLeft(feature))
                                        .style(WebStyles.ghost())
                                        .size(unit(9))
                                        .enabled(canMoveLeft)
                                        .tooltip(Core.bundle.get("feature.button.move-left"))
                                        .children(() -> icon(FileIcon.of("chevron-left.png", Icon.left)).size(unit(6)));

                                button(() -> FeatureManager.moveRight(feature))
                                        .style(WebStyles.ghost())
                                        .size(unit(9))
                                        .enabled(canMoveRight)
                                        .tooltip(Core.bundle.get("feature.button.move-right"))
                                        .children(() -> icon(FileIcon.of("chevron-right.png", Icon.right)).size(unit(6)));
                            }
                        });

                        divider().color(statusColor);
                    });
                }).element();
    }
}
