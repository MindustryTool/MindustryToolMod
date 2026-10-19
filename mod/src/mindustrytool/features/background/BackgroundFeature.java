package mindustrytool.features.background;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Texture;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.graphics.MenuRenderer;
import mindustrytool.Folders;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;

public class BackgroundFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<String> pathConfig;
    public final ConfigValue<Integer> opacityConfig;
    public final ConfigValue<Integer> videoVolumeConfig;
    public final ConfigValue<String> scalingConfig;

    private MenuRenderer defaultRenderer;
    private boolean defaultRendererReleased;
    private ImageMenuRenderer imageRenderer;
    private VideoMenuRenderer videoRenderer;
    private @Nullable BackgroundSettingsDialog settingDialog;
    private int rendererGeneration;
    private boolean releasedForGameplay;
    private boolean videoFailed;

    public BackgroundFeature() {
        super(FeatureMetadata.builder()
                .id("background")
                .icon(Icon.image)
                .build());

        config = configGroup();

        pathConfig = config.stringValue("path", "");
        opacityConfig = config.intValue("opacity", 100);
        videoVolumeConfig = config.intValue("videoVolume", 100);
        scalingConfig = config.stringValue("scaling", BackgroundScaling.CROP);

        Effect.of(pathConfig.signal(), path -> {
            if (isEnabled()) {
                applyPath(path);
            }
        });

        Effect.of(videoVolumeConfig.signal(), volume -> {
            if (!isEnabled()) {
                return;
            }
            BackgroundMenuRenderer renderer = activeRenderer();
            if (renderer != null) {
                renderer.setVolume(volume != null ? volume : 100);
            }
        });

        Events.run(Trigger.update, this::onUpdate);
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingDialog == null) {
                settingDialog = new BackgroundSettingsDialog(this);
            }
            return settingDialog;
        };
    }

    @Override
    public void onEnable() {
        videoFailed = false;
        releasedForGameplay = false;
        applyPath(pathConfig.get());
    }

    @Override
    public void onDisable() {
        restoreOriginalRenderer();
    }

    public void resetBackground() {
        pathConfig.reset();
        opacityConfig.reset();
        videoVolumeConfig.reset();
        scalingConfig.reset();
        restoreOriginalRenderer();
    }

    private void onUpdate() {
        if (!isEnabled()) {
            return;
        }

        BackgroundMenuRenderer renderer = activeRenderer();
        boolean menu = Vars.state.isMenu();

        switch (BackgroundLifecycle.next(true, menu, renderer != null, renderer != null && renderer.isVideo())) {
            case RELEASE_VIDEO:
                releaseVideo();
                return;
            case RELOAD:
                reloadAfterGameplay();
                return;
            case NONE:
            default:
                break;
        }

        if (!menu || renderer == null) {
            return;
        }

        renderer.update();
        if (renderer.blocksMenuMusic()) {
            Vars.control.sound.keepSilent();
        }
    }

    private void releaseVideo() {
        VideoMenuRenderer renderer = videoRenderer;
        if (renderer == null || !renderer.isActive()) {
            return;
        }
        // The decoder, worker thread and texture are gone; the renderer stays installed and draws
        // nothing until the video is reloaded at the menu, so no default renderer is rebuilt here.
        renderer.release();
        releasedForGameplay = true;
        rendererGeneration++;
    }

    private void reloadAfterGameplay() {
        if (!releasedForGameplay || videoFailed) {
            return;
        }
        releasedForGameplay = false;
        applyPath(pathConfig.get());
    }

    public void applyPath(@Nullable String path) {
        videoFailed = false;
        releasedForGameplay = false;
        if (path != null && !path.trim().isEmpty()) {
            Fi file = Folders.backgroundsDir.child(path);
            if (!file.exists()) {
                file = Core.files.absolute(path);
            }
            if (file.exists() && !file.isDirectory()) {
                applyBackground(file);
                return;
            }
        }
        restoreOriginalRenderer();
    }

    public void applyBackground(Fi file) {
        if (!file.exists() || file.isDirectory()) {
            Core.app.post(() -> {
                Vars.ui.showInfo(Core.bundle.format("feature.background.error.invalid-file", file.absolutePath()));
            });
            return;
        }

        try {
            captureDefaultRenderer();
            rendererGeneration++;
            videoFailed = false;

            BackgroundMenuRenderer renderer = BackgroundMediaType.of(file.name()).isVideo()
                    ? loadVideo(file)
                    : loadImage(file);

            applyRenderer(renderer);
        } catch (Exception e) {
            Core.app.post(() -> {
                Vars.ui.showException(Core.bundle.get("feature.background.error.apply"), e);
            });
            restoreOriginalRenderer();
        }
    }

    private BackgroundMenuRenderer loadImage(Fi file) {
        disposeVideo();
        imageRenderer = new ImageMenuRenderer(new Texture(file), liveDefaultRenderer(), opacityConfig, scalingConfig);
        return imageRenderer;
    }

    private BackgroundMenuRenderer loadVideo(Fi file) {
        disposeImage();
        if (videoRenderer == null) {
            videoRenderer = new VideoMenuRenderer(opacityConfig, scalingConfig, videoVolumeConfig);
        }

        int generation = rendererGeneration;
        videoRenderer.load(file, error -> onVideoFailed(generation, error));

        // The video replaces the default menu renderer, so its resources are released while it plays.
        releaseDefaultRenderer();
        return videoRenderer;
    }

    private void disposeImage() {
        ImageMenuRenderer renderer = imageRenderer;
        imageRenderer = null;
        if (renderer != null) {
            renderer.dispose();
        }
    }

    private void disposeVideo() {
        VideoMenuRenderer renderer = videoRenderer;
        videoRenderer = null;
        if (renderer != null) {
            renderer.dispose();
        }
    }

    private @Nullable BackgroundMenuRenderer activeRenderer() {
        if (videoRenderer != null && videoRenderer.isActive()) {
            return videoRenderer;
        }
        return imageRenderer;
    }

    private void onVideoFailed(int generation, Throwable error) {
        if (generation != rendererGeneration) {
            return;
        }
        videoFailed = true;
        disposeVideo();
        applyDefaultRenderer();
        VideoMenuRenderer.showCodecError(error);
    }

    private void captureDefaultRenderer() {
        if (defaultRenderer != null) {
            return;
        }
        MenuRenderer current = Reflect.get(Vars.ui.menufrag, "renderer");
        if (!(current instanceof BackgroundMenuRenderer)) {
            defaultRenderer = current;
        }
    }

    private @Nullable MenuRenderer liveDefaultRenderer() {
        if (defaultRendererReleased) {
            defaultRenderer = new MenuRenderer();
            defaultRendererReleased = false;
        }
        return defaultRenderer;
    }

    private void releaseDefaultRenderer() {
        if (defaultRenderer == null || defaultRendererReleased) {
            return;
        }
        defaultRenderer.dispose();
        defaultRendererReleased = true;
    }

    private void applyDefaultRenderer() {
        MenuRenderer renderer = liveDefaultRenderer();
        if (renderer != null) {
            applyRenderer(renderer);
        }
    }

    private void applyRenderer(MenuRenderer renderer) {
        try {
            Reflect.set(Vars.ui.menufrag, "renderer", renderer);
        } catch (Exception e) {
            Log.err("Failed to apply background renderer", e);
        }
    }

    public void restoreOriginalRenderer() {
        videoFailed = false;
        releasedForGameplay = false;
        rendererGeneration++;

        disposeImage();
        disposeVideo();
        applyDefaultRenderer();
    }
}