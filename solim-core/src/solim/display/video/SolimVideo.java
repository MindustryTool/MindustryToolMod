package solim.display.video;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.gen.Icon;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.core.Units;
import solim.display.Text;
import solim.input.Button;
import solim.input.SolimSlider;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.core.DisposableAction;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.IconCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.Values;
import solim.modifier.VideoCapability;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.runtime.AttachmentStack;

public final class SolimVideo extends Component
        implements Configurable<SolimVideo>, ElementCapability, CellCapability, AlignmentCapability, VideoCapability, IconCapability {

    private static final long OVERLAY_HIDE_DELAY = 2500;

    private static VideoLoader loader = new DefaultVideoLoader();

    private static @Nullable Drawable defaultPlayIcon;
    private static @Nullable Drawable defaultPauseIcon;
    private static @Nullable Drawable defaultReplayIcon;
    private static @Nullable Drawable defaultVolumeIcon;
    private static @Nullable Drawable defaultMuteIcon;

    public static void setVideoLoader(@Nullable VideoLoader customLoader) {
        loader = customLoader != null ? customLoader : new DefaultVideoLoader();
    }

    public static void setDefaultIcons(@Nullable Drawable play, @Nullable Drawable pause,
            @Nullable Drawable replay, @Nullable Drawable volume, @Nullable Drawable mute) {
        defaultPlayIcon = play;
        defaultPauseIcon = pause;
        defaultReplayIcon = replay;
        defaultVolumeIcon = volume;
        defaultMuteIcon = mute;
    }

    private static Drawable playIconDefault() {
        return defaultPlayIcon != null ? defaultPlayIcon : Icon.play;
    }

    private static Drawable pauseIconDefault() {
        return defaultPauseIcon != null ? defaultPauseIcon : Icon.pause;
    }

    private static Drawable replayIconDefault() {
        return defaultReplayIcon != null ? defaultReplayIcon : Icon.refresh;
    }

    private static Drawable volumeIconDefault() {
        return defaultVolumeIcon != null ? defaultVolumeIcon : Icon.volumeUp;
    }

    private static Drawable muteIconDefault() {
        return defaultMuteIcon != null ? defaultMuteIcon : Icon.volumeOff;
    }

    static String formatTime(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0)
            seconds = 0;
        long total = (long) seconds;
        long minutes = total / 60;
        long secs = total % 60;
        return (minutes < 10 ? "0" : "") + minutes + ":" + (secs < 10 ? "0" : "") + secs;
    }

    private final Image videoImage;
    private final Table overlay;
    private final Stack element;
    private final PendingCellConfig constraints = new PendingCellConfig();

    private final Signal<Boolean> playing = Signal.of(false);
    private final Signal<Boolean> mutedState = Signal.of(false);
    private final Signal<Float> progress = Signal.of(0f);
    private final Signal<Float> positionSig = Signal.of(0f);
    private final Signal<Double> durationSig = Signal.of(0.0);
    private final Signal<Boolean> controlsEnabled = Signal.of(true);
    private final Signal<Boolean> overlayShown = Signal.of(true);

    private @Nullable Drawable playIcon;
    private @Nullable Drawable pauseIcon;
    private @Nullable Drawable replayIcon;
    private @Nullable Drawable volumeIcon;
    private @Nullable Drawable muteIcon;
    private @Nullable Drawable placeholder;
    private @Nullable Drawable fallback;
    private @Nullable Props<SolimVideo> pending;
    private Scaling scaling = Scaling.fit;

    private @Nullable VideoPlayer player;
    private @Nullable String currentUrl;
    private long loadGeneration = 0;
    private boolean failed = false;
    private @Nullable Disposable urlBinding;
    private @Nullable Disposable controlsBinding;

    private boolean autoplay = false;
    private boolean loop = true;
    private float volume = 1f;
    private int cornerRadius = 0;
    private long lastPoke = 0;
    private float refreshAccum = 0;
    private boolean scrubbing = false;

    public SolimVideo() {
        this(new VideoStage());
    }

    private SolimVideo(Stack element) {
        super(element);
        this.element = element;
        SolimToken.bind(element, this, constraints);
        ((VideoStage) element).owner = this;

        videoImage = new Image((Drawable) null);
        videoImage.setScaling(Scaling.fit);
        element.add(videoImage);

        overlay = new Table();
        overlay.bottom();
        element.add(overlay);

        OwnershipContext.push(this);
        try {
            AttachmentStack.isolate(() -> {
                buildOverlay();
                return null;
            });
        } finally {
            OwnershipContext.pop();
        }

        videoImage.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                poke();
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                poke();
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                poke();
                return false;
            }
        });

        OwnershipContext.push(this);
        try {
            Effect.of(() -> overlay.visible = controlsEnabled.get() && overlayShown.get());
            Effect.of(this::onScrub);
        } finally {
            OwnershipContext.pop();
        }
    }

    public SolimVideo(String url) {
        this();
        setUrl(url);
    }

    public SolimVideo(Readable<String> url) {
        this();
        setUrl(url);
    }

    private void buildOverlay() {
        Button playButton = new Button(this::togglePlay);
        playButton.icon(playing.map(active -> active ? pauseIconOrDefault() : playIconOrDefault()));
        playButton.props(Values.size(Units.unit(11)), Values.prefWidth(Units.unit(11)), Values.prefHeight(Units.unit(11)));

        SolimSlider scrub = SolimSlider.of(progress, 0f, 1f, 0.001f);
        scrub.element().addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                scrubbing = true;
                poke();
                return false;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                scrubbing = false;
                poke();
            }
        });

        Text time = Text
                .of(new Computed<>(() -> formatTime(positionSig.get()) + " / " + formatTime(durationSig.get())));

        Button replayButton = new Button(this::replay);
        replayButton.icon(replayIconOrDefault());
        replayButton.props(Values.size(Units.unit(11)), Values.prefWidth(Units.unit(11)), Values.prefHeight(Units.unit(11)));

        Button muteButton = new Button(this::toggleMute);
        muteButton.icon(mutedState.map(muted -> muted ? muteIconOrDefault() : volumeIconOrDefault()));
        muteButton.props(Values.size(Units.unit(11)), Values.prefWidth(Units.unit(11)), Values.prefHeight(Units.unit(11)));

        overlay.add(playButton.element()).size(Units.unit(11)).pad(Units.unit(1));
        overlay.add(scrub.element()).growX().pad(Units.unit(1));
        overlay.add(time.label()).pad(Units.unit(1));
        overlay.add(replayButton.element()).size(Units.unit(11)).pad(Units.unit(1));
        overlay.add(muteButton.element()).size(Units.unit(11)).pad(Units.unit(1));
    }

    private Drawable playIconOrDefault() {
        return playIcon != null ? playIcon : playIconDefault();
    }

    private Drawable pauseIconOrDefault() {
        return pauseIcon != null ? pauseIcon : pauseIconDefault();
    }

    private Drawable replayIconOrDefault() {
        return replayIcon != null ? replayIcon : replayIconDefault();
    }

    private Drawable volumeIconOrDefault() {
        return volumeIcon != null ? volumeIcon : volumeIconDefault();
    }

    private Drawable muteIconOrDefault() {
        return muteIcon != null ? muteIcon : muteIconDefault();
    }

    public void setPlayIcon(@Nullable Drawable icon) {
        this.playIcon = icon;
    }

    public void setPauseIcon(@Nullable Drawable icon) {
        this.pauseIcon = icon;
    }

    public void setReplayIcon(@Nullable Drawable icon) {
        this.replayIcon = icon;
    }

    public void setVolumeIcon(@Nullable Drawable icon) {
        this.volumeIcon = icon;
    }

    public void setMuteIcon(@Nullable Drawable icon) {
        this.muteIcon = icon;
    }

    public void setAutoplay(boolean autoplay) {
        this.autoplay = autoplay;
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        if (player != null)
            player.setLooping(loop);
    }

    public void setVolume(float volume) {
        this.volume = volume < 0f ? 0f : volume > 1f ? 1f : volume;
        if (player != null)
            player.setVolume(this.volume);
    }

    public void setMuted(boolean muted) {
        mutedState.set(muted);
        if (player != null)
            player.setMuted(muted);
    }

    public void bindMutedSignal(Signal<Boolean> signal) {
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    mutedState::peek,
                    this::setMuted,
                    onChange -> {
                        Effect.of(() -> {
                            mutedState.get();
                            onChange.run();
                        });
                        return DisposableAction.empty();
                    }
            );
        } finally {
            OwnershipContext.pop();
        }
    }

    public void setRounded(int radius) {
        this.cornerRadius = Math.max(0, radius);
        if (player != null)
            player.setCornerRadius(this.cornerRadius);
    }

    public void setPlaceholder(@Nullable Drawable placeholder) {
        this.placeholder = placeholder;
        if (videoImage.getDrawable() == null && placeholder != null) {
            videoImage.setDrawable(placeholder);
        }
    }

    public void setFallback(@Nullable Drawable fallback) {
        this.fallback = fallback;
        if ((failed || currentUrl == null || currentUrl.trim().isEmpty()) && fallback != null) {
            videoImage.setDrawable(fallback);
            videoImage.invalidateHierarchy();
        }
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
        videoImage.setScaling(scaling);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public void setControls(boolean controls) {
        if (controlsBinding != null) {
            controlsBinding.dispose();
            controlsBinding = null;
        }
        controlsEnabled.set(controls);
    }

    public void setControls(@Nullable Readable<Boolean> controls) {
        if (controlsBinding != null) {
            controlsBinding.dispose();
            controlsBinding = null;
        }
        if (controls != null) {
            withinOwnership(() -> controlsBinding = Effect.of(() -> controlsEnabled.set(controls.get())));
        }
    }

    public void setUrl(@Nullable String url) {
        if (urlBinding != null) {
            urlBinding.dispose();
            urlBinding = null;
        }
        loadUrl(url);
    }

    public void setUrl(@Nullable Readable<String> url) {
        if (urlBinding != null) {
            urlBinding.dispose();
            urlBinding = null;
        }
        if (url != null) {
            withinOwnership(() -> urlBinding = Effect.of(() -> loadUrl(url.get())));
        }
    }

    private void loadUrl(@Nullable String url) {
        currentUrl = url;
        final long gen = ++loadGeneration;
        failed = false;
        stopPlayer();
        playing.set(false);
        progress.set(0f);
        positionSig.set(0f);
        durationSig.set(0.0);
        overlayShown.set(false);
        if (url == null || url.trim().isEmpty()) {
            if (fallback != null)
                videoImage.setDrawable(fallback);
            return;
        }
        if (placeholder != null)
            videoImage.setDrawable(placeholder);
        VideoCache.scheduleCleanup();
        loader.load(url,
                file -> postMain(() -> openPlayer(file, url, gen)),
                error -> postMain(() -> onLoadFailed(gen, url)));
    }

    private void openPlayer(@Nullable Fi file, String url, long gen) {
        if (isDisposed() || gen != loadGeneration || !url.equals(currentUrl))
            return;
        if (file == null || !file.exists()) {
            onLoadFailed(gen, url);
            return;
        }
        stopPlayer();
        VideoPlayer next = new VideoPlayer();
        next.setVolume(volume);
        next.setMuted(mutedState.peek());
        next.setLooping(loop);
        next.setCornerRadius(cornerRadius);
        player = next;
        next.open(file, url, autoplay, new VideoPlayer.Listener() {
            @Override
            public void onReady(int width, int height, double durationSec) {
                postMain(() -> onPlayerReady(url, gen, width, height, durationSec));
            }

            @Override
            public void onEnded() {
                postMain(() -> {
                    if (gen == loadGeneration && url.equals(currentUrl)) {
                        playing.set(false);
                        progress.set(1f);
                        poke();
                    }
                });
            }

            @Override
            public void onFailed(Throwable error) {
                postMain(() -> onLoadFailed(gen, url));
            }
        });
    }

    private void onPlayerReady(String url, long gen, int width, int height, double durationSec) {
        if (isDisposed() || gen != loadGeneration || !url.equals(currentUrl) || player == null)
            return;
        Texture texture = player.texture();
        if (texture == null) {
            onLoadFailed(gen, url);
            return;
        }
        videoImage.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
        videoImage.invalidateHierarchy();
        durationSig.set(durationSec);
        poke();
    }

    private void onLoadFailed(long gen, String url) {
        if (gen != loadGeneration || !url.equals(currentUrl))
            return;
        stopPlayer();
        playing.set(false);
        failed = true;
        if (fallback != null) {
            videoImage.setDrawable(fallback);
            videoImage.invalidateHierarchy();
        }
    }

    private void stopPlayer() {
        if (player != null) {
            player.dispose();
            player = null;
        }
    }

    private void togglePlay() {
        poke();
        if (player == null)
            return;
        player.togglePlay();
        playing.set(player.isPlaying());
    }

    private void replay() {
        poke();
        if (player == null)
            return;
        player.seek(0);
        player.play();
        playing.set(player.isPlaying());
    }

    private void toggleMute() {
        poke();
        boolean next = !mutedState.peek();
        mutedState.set(next);
        if (player != null)
            player.setMuted(next);
    }

    private void poke() {
        lastPoke = Time.millis();
        if (!overlayShown.peek())
            overlayShown.set(true);
    }

    private void onScrub() {
        Float position = progress.get();
        VideoPlayer active = player;
        if (active == null || position == null)
            return;
        double duration = active.getDuration();
        if (duration <= 0)
            return;
        if (Math.abs(position * duration - active.getPosition()) > 0.6) {
            active.seek(position * duration);
        }
    }

    void onAct(float delta) {
        if (isDisposed())
            return;
        VideoPlayer active = player;
        if (active == null)
            return;
        active.update(delta);
        refreshAccum += delta;
        if (refreshAccum >= 0.1f) {
            refreshAccum = 0;
            positionSig.set(active.getPosition());
            durationSig.set(active.getDuration());
            playing.set(active.isPlaying());
            double duration = active.getDuration();
            progress.set(duration > 0 ? (float) (active.getPosition() / duration) : 0f);
        }
        if (scrubbing)
            poke();
        if (active.isPlaying() && !scrubbing && Time.millis() - lastPoke > OVERLAY_HIDE_DELAY) {
            if (overlayShown.peek())
                overlayShown.set(false);
        }
    }

    private static void postMain(Runnable task) {
        try {
            if (Core.app != null) {
                Core.app.post(task);
                return;
            }
        } catch (Throwable ignored) {
        }
        task.run();
    }

    @Nullable
    VideoPlayer player() {
        return player;
    }

    Table overlay() {
        return overlay;
    }

    public Stack stack() {
        return element;
    }

    public Signal<Float> progressSignal() {
        return progress;
    }

    @Override
    protected void onDispose() {
        loadGeneration++;
        stopPlayer();
        if (urlBinding != null) {
            urlBinding.dispose();
            urlBinding = null;
        }
        if (controlsBinding != null) {
            controlsBinding.dispose();
            controlsBinding = null;
        }
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    // TODO: Those component arent table, they should not impl these method and having children function causing user missuse 
    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public @Nullable Props<SolimVideo> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimVideo> props) {
        this.pending = props;
    }

    @Override
    public SolimVideo name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimVideo self() {
        return this;
    }

    private static final class VideoStage extends Stack {
        @Nullable
        SolimVideo owner;

        @Override
        public void act(float delta) {
            super.act(delta);
            if (owner != null)
                owner.onAct(delta);
        }
    }
}
