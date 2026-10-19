package mindustrytool.features.settings;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.services.PreviewVideos;
import solim.core.Component;
import solim.core.Units;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class FeatureHelpView extends Component {

    private static final float PADDING = 16f;
    private static final float VIDEO_ASPECT = 9f / 16f;

    private final Feature feature;
    private final Signal<Boolean> videoAvailable = Signal.of(false);
    private final @Nullable String videoUrl;

    public FeatureHelpView(Feature feature) {
        this.feature = feature;
        this.videoUrl = feature.getPreviewVideoUrl();
        if (videoUrl != null) {
            PreviewVideos.exists(videoUrl)
                    .thenAccept(exists -> Core.app.post(() -> videoAvailable.set(Boolean.TRUE.equals(exists))));
        }
    }

    @Override
    protected Element build() {
        String help = feature.getHelp();
        boolean hasHelp = help != null && !help.trim().isEmpty();
        String text = hasHelp ? help : Core.bundle.get("feature.help.no-description");

        String description = feature.getDescription();
        boolean hasDescription = description != null && !description.trim().isEmpty();

        Readable<Float> viewWidth = new Computed<>(() -> Math.min(
                (Vars.mobile ? 0.8f : 0.6f) * Units.dvh.get(),
                0.9f * Units.dvw.get()));
        Readable<Float> videoWidth = viewWidth.map(w -> w - PADDING * 2f);
        Readable<Float> videoHeight = videoWidth.map(w -> w * VIDEO_ASPECT);

        return scroll()
                .props(grow(), center())
                .children(() -> {
                    column()
                            .props(growX(), center(), maxWidth(viewWidth), padding(PADDING), gap(12f))
                            .children(() -> {
                                row()
                                        .props(gap(10f))
                                        .children(() -> {
                                            image(feature.getMetadata().getIcon())
                                                    .props(scaling(Scaling.fit), size(32f), prefWidth(32f),
                                                            prefHeight(32f));

                                            text(feature.getName())
                                                    .props(style(Styles.defaultLabel), color(Color.white), left());
                                        });

                                if (hasDescription) {
                                    text(description)
                                            .props(color(Color.lightGray), fontScale(0.95f), textWrap(true), left());
                                    divider();
                                }

                                text(text)
                                        .props(color(hasHelp ? Color.white : Color.lightGray), textWrap(true), left());

                                if (videoUrl != null) {
                                    when(videoAvailable).thenDo(() -> videoPreview(videoWidth, videoHeight));
                                }
                            });
                })
                .element();
    }

    private void videoPreview(Readable<Float> width, Readable<Float> height) {
        divider();

        video(videoUrl)
                .props(
                        growX(),
                        maxWidth(width),
                        prefHeight(height),
                        imageRounded(unit(4)),
                        controls(true),
                        autoplay(true),
                        loop(false));
    }
}
