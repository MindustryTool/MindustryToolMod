package mindustrytool.features.web;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import solim.core.Component;

public class WebFeatureCard extends Component {

    private final WebFeature webFeature;

    public WebFeatureCard(WebFeature webFeature) {
        this.webFeature = webFeature;
    }

    @Override
    protected Element build() {

        return card(Styles.black8).name("WebFeatureCard-" + webFeature.getId())
                .props(height(unit(60)), prefHeight(unit(60)), growX(), rounded(unit(4)), border(1.5f, Color.darkGray),
                        padding(unit(1)), backgroundColor(Color.black), onClick(this::openWebFeature))
                .children(() -> {
                    column().props(grow(), padding(unit(2)), gap(unit(2))).children(() -> {
                        row().props(growX()).children(() -> {
                            row().props(center(), gap(unit(2))).children(() -> {
                                icon(webFeature.getIcon()).props(size(unit(7)), prefWidth(unit(7)),
                                        prefHeight(unit(7)));
                                text(webFeature.getName())
                                        .props(style(Styles.defaultLabel), color(Color.white), ellipsis(true), left());
                            });

                            spacer();

                            button(this::openWebFeature)
                                    .props(style(Styles.clearNonei), size(unit(11)), prefWidth(unit(11)),
                                            prefHeight(unit(11)),
                                            tooltip(Core.bundle.get("web-feature.button.open", "Open in browser")))
                                    .children(() -> icon(FileIcon.of("external-link.png")).props(size(unit(7)),
                                            prefWidth(unit(7)), prefHeight(unit(7))));
                        });

                        text(webFeature.getDescription())
                                .props(
                                        color(Color.lightGray),
                                        fontScale(0.9f),
                                        textWrap(true),
                                        left());
                    });
                }).element();
    }

    private void openWebFeature() {
        try {
            if (!Core.app.openURI(webFeature.getUrl())) {
                copyUrlToClipboard();
            }
        } catch (Exception e) {
            Log.err("Failed to open web feature URL: " + webFeature.getUrl(), e);
            copyUrlToClipboard();
        }
    }

    private void copyUrlToClipboard() {
        Core.app.setClipboardText(webFeature.getUrl());
        Vars.ui.showInfoFade(Core.bundle.get("feature.toast.copied", "Copied to clipboard!"));
    }
}
