package mindustrytool.features.wavepreview;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.reactive.Readable;

public class WavePreviewPanelView extends Component {

    private final WavePreviewFeature feature;

    public WavePreviewPanelView(WavePreviewFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> opacity = feature.opacityConfig.signal();
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());

        return column()
                .props(left(), top(), border(1.5f, Color.darkGray), rounded(unit(3)), backgroundColor(WebStyles.Colors.SECONDARY_BG), opacity(opacity), visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                        && Vars.state.isGame() && Vars.state.rules != null && Vars.state.rules.waves))
                .children(() -> {

                    dynamic(feature.getState().waves(), waves -> column().props(growX(), left()).children(() -> {
                        if (waves == null || waves.isEmpty()) {
                            return;
                        }

                        boolean multiWave = waves.size > 1;
                        for (int w = 0; w < waves.size; w++) {
                            WaveSectionData section = waves.get(w);
                            if (w != 0) {
                                divider();
                            }
                            renderWaveSection(section, scale, multiWave);
                        }
                    })).props(padding(unit(2)), growX());
                }).element();
    }

    private void renderWaveSection(WaveSectionData section, Readable<Float> scale, boolean multiWave) {
        column().props(growX(), left()).children(() -> {
            if (multiWave) {
                text(Core.bundle.format("feature.wave-preview.section.wave", section.waveNumber))
                        .props(style(Styles.outlineLabel), color(Pal.accent), left(), fontScale(scale));
            } else {
                text(String.valueOf(section.waveNumber))
                        .props(style(Styles.outlineLabel), left(), fontScale(scale));
            }

            if (section.isEmpty()) {
                text(Core.bundle.get("feature.wave-preview.empty"))
                        .props(style(Styles.outlineLabel), color(Color.lightGray), left(), fontScale(scale.map(s -> 0.9f * (s != null ? s : 1f))));
            } else {
                renderDomainRow(Core.bundle.get("feature.wave-preview.domain.ground"), section.ground, scale);
                renderDomainRow(Core.bundle.get("feature.wave-preview.domain.air"), section.air, scale);
                renderDomainRow(Core.bundle.get("feature.wave-preview.domain.naval"), section.naval, scale);
            }
        });
    }

    private void renderDomainRow(String domainLabel, Seq<WaveUnitEntry> entries, Readable<Float> scale) {
        if (entries.isEmpty()) {
            return;
        }

        Readable<Float> iconSize = scale.map(s -> 16f * 1.5f * (s != null ? s : 1f));

        row().props(growX(), center(), left(), gap(unit(1))).children(() -> {
            text(domainLabel)
                    .props(style(Styles.outlineLabel), color(Color.lightGray), left(), fontScale(scale.map(s -> 0.85f * (s != null ? s : 1f))));

            wrap().props(growX(), maxWidth(230), gap(unit(1))).children(() -> {
                for (int i = 0; i < entries.size; i++) {
                    WaveUnitEntry entry = entries.get(i);
                    row().props(center(), gap(unit(0.5f))).children(() -> {
                        image(entry.type.uiIcon != null
                                ? new TextureRegionDrawable(entry.type.uiIcon)
                                : new TextureRegionDrawable())
                                .props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize));

                        text(String.valueOf(entry.amount))
                                .props(style(Styles.outlineLabel), fontScale(scale));
                    });
                }
            });
        });
    }
}
