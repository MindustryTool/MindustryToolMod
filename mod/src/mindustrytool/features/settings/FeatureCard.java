package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import lombok.AllArgsConstructor;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import solim.core.BaseComponent;
import solim.signal.Readable;

/**
 * Component responsible for building and managing a single feature's visual
 * card. Handles display of metadata, action shortcuts (help, settings, main
 * dialog), and state toggling with direct property reactivity using pure Solim
 * components.
 */
@AllArgsConstructor
public class FeatureCard extends BaseComponent {

    private final Feature feature;

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
                        row().growX().children(() -> {
                            icon(metadata.getIcon()).size(unit(7)).cellPaddingRight(unit(2));

                            text(feature.getName()).style(Styles.defaultLabel).color(Color.white).ellipsis(true).left();

                            spacer();

                            if (feature.getMainDialog() != null) {
                                button(() -> feature.getMainDialog().show()).style(Styles.clearNonei).size(unit(11))
                                        .tooltip(Core.bundle.get("feature.button.open-dialog"))
                                        .children(() -> icon(Icon.linkSmall).size(unit(7)));
                            }

                            if (feature.getSettingDialog() != null) {
                                button(() -> feature.getSettingDialog().show()).style(Styles.clearNonei).size(unit(11))
                                        .tooltip(Core.bundle.get("feature.button.settings"))
                                        .children(() -> icon(Icon.settings).size(unit(7)));
                            }

                            button(() -> new FeatureHelpDialog(feature).show()).style(Styles.clearNonei).size(unit(11))
                                    .tooltip(Core.bundle.get("feature.button.help"))
                                    .children(() -> icon(Icon.infoCircle).size(unit(7)));
                        });

                        text(feature.getDescription()).color(Color.lightGray).fontScale(0.9f).wrap(true).left();

                        spacer();

                        text(statusText).style(Styles.defaultLabel)
                                .growX()
                                .color(statusColor)
                                .left();

                        divider().color(statusColor);
                    });
                }).element();
    }
}
