package mindustrytool.features.web;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Log;
import lombok.AllArgsConstructor;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import solim.core.BaseComponent;

@AllArgsConstructor
public class WebFeatureCard extends BaseComponent {

    private final WebFeature webFeature;

    @Override
    protected Element build() {

        return card(Styles.black8).name("WebFeatureCard-" + webFeature.getId()).height(unit(60)).growX()
                .rounded(unit(4))
                .border(1.5f, Color.darkGray)
                .padding(unit(1))
                .backgroundColor(Color.black)
                .onClick(this::openWebFeature)
                .children(() -> {
                    column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                        row().growX().children(() -> {
                            row().gap(unit(2)).children(() -> {
                                icon(webFeature.getIcon()).size(unit(7));
                                text(webFeature.getName()).style(Styles.defaultLabel).color(Color.white).ellipsis(true)
                                        .left();
                            });

                            spacer();

                            button(this::openWebFeature).style(Styles.clearNonei).size(unit(11))
                                    .tooltip(Core.bundle.get("web-feature.button.open", "Open in browser"))
                                    .children(() -> icon(Icon.linkSmall).size(unit(7)));
                        });

                        text(webFeature.getDescription()).color(Color.lightGray).fontScale(0.9f).wrap(true).left();
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
