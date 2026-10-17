package mindustrytool.features.settings;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.maxWidth;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Scaling;
import lombok.AllArgsConstructor;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import solim.core.Component;

@AllArgsConstructor
public class FeatureHelpView extends Component {

    private final Feature feature;

    @Override
    protected Element build() {
        String help = feature.getHelp();
        boolean hasHelp = help != null && !help.trim().isEmpty();
        String text = hasHelp ? help : Core.bundle.get("feature.help.no-description");

        String description = feature.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();

        return scroll()
                .props(grow(), center())
                .children(() -> {
                    column()
                            .props(growX(), center(), maxWidth(Vars.mobile ? dvh(80) : dvh(50)), padding(16f), gap(12f))
                            .children(() -> {
                                row()
                                        .props(gap(10f))
                                        .children(() -> {
                                            image(feature.getMetadata().getIcon())
                                                    .scaling(Scaling.fit)
                                                    .props(size(32f), prefWidth(32f), prefHeight(32f));

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
