package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;

public class GodModeSettingsView extends BaseComponent {

    private final GodModeFeature feature;

    public GodModeSettingsView(GodModeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(3)).children(() -> {
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.god-mode.settings.scale")).left().color(WebStyles.Colors.GHOST_FG);

                        spacer();
                        slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);

                        row().width(unit(14)).children(() -> {
                            text(feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    divider();

                    column().growX().gap(unit(1.5f)).children(() -> {
                        text(Core.bundle.get("feature.god-mode.settings.provider")).left().color(WebStyles.Colors.GHOST_FG);

                        wrap().gap(unit(1.5f)).children(() -> {
                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_AUTO))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_AUTO::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.auto")));

                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_INTERNAL))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_INTERNAL::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.internal")));

                            button(() -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_JS))
                                    .style(WebStyles.filterChip())
                                    .checked(feature.providerModeConfig.signal()
                                            .map(GodModeFeature.PROVIDER_JS::equals))
                                    .padding(unit(1.5f))
                                    .children(() -> text(Core.bundle.get("feature.god-mode.settings.provider.js")));
                        });
                    });

                    divider();

                    button(feature::resetPosition)
                            .style(WebStyles.secondary())
                            .growX()
                            .padding(unit(2))
                            .children(() -> text(Core.bundle.get("feature.god-mode.settings.reset-position")));
                });
            });
        }).element();
    }
}
