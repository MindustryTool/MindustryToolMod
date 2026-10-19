package mindustrytool.features.background;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import solim.config.ConfigValue;
import solim.display.video.VideoPlayer;

public class VideoMenuRenderer extends BackgroundMenuRenderer {

    public interface Listener {
        void onFailed(Throwable error);
    }

    private final ConfigValue<Integer> volumeConfig;
    private @Nullable VideoPlayer player;

    public VideoMenuRenderer(ConfigValue<Integer> opacityConfig, ConfigValue<String> scalingConfig,
            ConfigValue<Integer> volumeConfig) {
        // A video fully replaces the default menu renderer, so it is never drawn underneath it.
        super(null, opacityConfig, scalingConfig);
        this.volumeConfig = volumeConfig;
    }

    public void load(Fi file, Listener listener) {
        release();

        VideoPlayer instance = new VideoPlayer();
        player = instance;
        instance.setLooping(true);
        instance.setVolume(volume());
        instance.open(file, audioKey(file), true, new VideoPlayer.Listener() {
            @Override
            public void onReady(int width, int height, double durationSec) {
            }

            @Override
            public void onEnded() {
            }

            @Override
            public void onFailed(Throwable error) {
                listener.onFailed(error);
            }
        });
    }

    public void release() {
        VideoPlayer instance = player;
        player = null;
        if (instance != null) {
            instance.dispose();
        }
    }

    public boolean isActive() {
        return player != null;
    }

    private static String audioKey(Fi file) {
        return file.path() + "|" + file.length() + "|" + file.lastModified();
    }

    private float volume() {
        Integer configured = volumeConfig.get();
        int value = configured != null ? configured : 100;
        return value / 100f;
    }

    @Override
    public void update() {
        VideoPlayer instance = player;
        // VideoPlayer measures its clock in seconds, which is what the unscaled frame delta is.
        if (instance != null) {
            instance.update(Core.graphics.getDeltaTime());
        }
    }

    @Override
    public void render() {
        VideoPlayer instance = player;
        if (instance == null) {
            return;
        }
        drawBackground(instance.texture());
    }

    @Override
    public boolean isVideo() {
        return true;
    }

    @Override
    public boolean blocksMenuMusic() {
        VideoPlayer instance = player;
        return instance != null && instance.hasAudio() && volume() > 0f && !instance.isMuted();
    }

    @Override
    public void setVolume(int volume) {
        VideoPlayer instance = player;
        if (instance != null) {
            instance.setVolume(volume / 100f);
        }
    }

    @Override
    public void dispose() {
        release();
    }

    public static void showCodecError(Throwable error) {
        Log.err("Background video could not be played", error);
        Core.app.post(() -> Vars.ui.showErrorMessage(Core.bundle.get("feature.background.error.unsupported-video")));
    }
}