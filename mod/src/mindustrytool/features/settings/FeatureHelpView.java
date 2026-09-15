package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Scaling;
import lombok.AllArgsConstructor;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import solim.core.BaseComponent;

@AllArgsConstructor
public class FeatureHelpView extends BaseComponent {

    private final Feature feature;

    @Override
    protected Element build() {
        String help = feature.getHelp();
        boolean hasHelp = help != null && !help.trim().isEmpty();
        String text = hasHelp ? help : Core.bundle.get("feature.help.no-description");

        String description = feature.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();

        return scroll()
                .grow()
                .center()
                .children(() -> {
                    column()
                            .growX()
                            .center()
                            .maxWidth(Vars.mobile ? dvh(80) : dvh(50))
                            .padding(16f)
                            .gap(12f)
                            .children(() -> {
                                row()
                                        .gap(10f)
                                        .children(() -> {
                                            image(feature.getMetadata().getIcon())
                                                    .scaling(Scaling.fit)
                                                    .size(32f);

                                            text(feature.getName())
                                                    .style(Styles.defaultLabel)
                                                    .color(Color.white)
                                                    .left();
                                        });

                                if (hasDescription) {
                                    text(description)
                                            .color(Color.lightGray)
                                            .fontScale(0.95f)
                                            .wrap(true)
                                            .left();
                                    divider();
                                }

                                text(text)
                                        .color(hasHelp ? Color.white : Color.lightGray)
                                        .wrap(true)
                                        .left();
                            });
                })
                .element();
    }
}
