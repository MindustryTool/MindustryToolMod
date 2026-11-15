package mindustrytool.features.wavepreview;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Strings;
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

        Readable<Seq<WaveSectionData>> displayWaves = computed(() -> {
            Seq<WaveSectionData> all = feature.getState().waves().get();
            if (all == null || all.isEmpty()) {
                return new Seq<>();
            }
            if (feature.getState().isCollapsed()) {
                Seq<WaveSectionData> single = new Seq<>(1);
                single.add(all.first());
                return single;
            }
            return all;
        });

        Element root = column()
                .props(left(), top(), border(1.5f, Color.darkGray), rounded(unit(3)),
                        backgroundColor(WebStyles.Colors.SECONDARY_BG), opacity(opacity),
                        visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                                && Vars.state.isGame() && Vars.state.rules != null && Vars.state.rules.waves))
                .children(() -> {

                    dynamic(displayWaves, waves -> column().props(growX(), left()).children(() -> {
                        if (waves == null || waves.isEmpty()) {
                            return;
                        }

                        boolean multiWave = feature.getState().waves().get() != null
                                && feature.getState().waves().get().size > 1;

                        for (int w = 0; w < waves.size; w++) {
                            WaveSectionData section = waves.get(w);
                            if (w != 0) {
                                divider();
                            }
                            renderWaveSection(section, scale, multiWave);
                        }
                    })).props(padding(unit(2)), growX());
                }).element();

        root.clicked(feature.getState()::toggleCollapsed);
        return root;
    }

    static String formatHp(float hp) {
        if (hp < 1000f) {
            return String.valueOf(Math.round(hp));
        }
        if (hp < 1_000_000f) {
            return Strings.autoFixed(hp / 1000f, 1) + "k";
        }
        return Strings.autoFixed(hp / 1_000_000f, 1) + "M";
    }

    private void renderWaveSection(WaveSectionData section, Readable<Float> scale, boolean multiWave) {
        column().props(growX(), left()).children(() -> {
            boolean hasHp = !section.isEmpty() && section.totalHealth > 0f;
            String hpStr = hasHp ? formatHp(section.totalHealth) : null;

            if (multiWave) {
                String headerText = hasHp
                        ? Core.bundle.format("feature.wave-preview.section.wave-hp", section.waveNumber, hpStr)
                        : Core.bundle.format("feature.wave-preview.section.wave", section.waveNumber);
                text(headerText)
                        .props(style(Styles.outlineLabel), color(Pal.accent), left(), fontScale(scale));
            } else {
                String headerText = hasHp
                        ? Core.bundle.format("feature.wave-preview.section.single-hp", section.waveNumber, hpStr)
                        : String.valueOf(section.waveNumber);
                text(headerText)
                        .props(style(Styles.outlineLabel), left(), fontScale(scale));
            }

            if (section.isEmpty()) {
                text(Core.bundle.get("feature.wave-preview.empty"))
                        .props(style(Styles.outlineLabel), color(Color.lightGray), left(),
                                fontScale(scale));
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
            column()
            .props(growY(),width(unit(20)))
            .children(() -> {
                text(domainLabel)
                        .props(color(Color.lightGray), left(),
                                fontScale(scale));
            });

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
