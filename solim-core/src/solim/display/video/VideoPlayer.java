package solim.display.video;

import arc.Core;
import arc.audio.Music;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Threads;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import solim.core.Disposable;

public final class VideoPlayer implements Disposable {

	public enum State {
		IDLE, LOADING, READY, PLAYING, PAUSED, ENDED, FAILED
	}

	public interface Listener {
		void onReady(int width, int height, double durationSec);

		void onEnded();

		void onFailed(Throwable error);
	}

	private static final ExecutorService DECODE_WORKER = Threads.unboundedExecutor("solim-video-decode", 2);
	private static final int FRAME_POOL = 4;
	private static final int FRAME_WAIT_MILLIS = 250;
	private static final double END_EPSILON = 0.08;
	private static final float AUDIO_EPSILON = 0.001f;
	private static final float RESYNC_THRESHOLD = 0.25f;

	static {
		VideoLog.install();
	}

	private static final class DecodedFrame {
		final ByteBuffer pixels;
		final double pts;

		DecodedFrame(ByteBuffer pixels, double pts) {
			this.pixels = pixels;
			this.pts = pts;
		}
	}

	private volatile State state = State.IDLE;
	private volatile boolean disposed = false;
	private volatile boolean looping = false;
	private volatile boolean streamEnded = false;
	private volatile boolean decodeDone = false;
	private volatile boolean singleFrame = false;
	private volatile boolean loopRestartRequest = false;
	private volatile int framesSinceLoop = 0;
	private volatile double pendingSeek = -1;
	private volatile int seekEpoch = 0;
	private volatile double seekTarget = 0;
	private final AtomicInteger sessionCounter = new AtomicInteger();
	private volatile @Nullable Thread decodeThread;
	private volatile boolean pendingPlay = false;
	private volatile int cornerRadius = 0;
	private volatile float lastPosition = 0f;
	private volatile double durationSec = 0;

	private float volume = 1f;
	private boolean muted = false;
	private float silentClock = 0f;
	private float lastAudioPosition = -1f;
	private float lastAppliedVolume = -1f;

	private Fi mp4File;
	private String audioUrl;
	private boolean autoplay;
	private Listener listener;
	private Fi wavFile;

	private @Nullable VideoFrameDecoder decoder;
	private int width;
	private int height;
	private BlockingQueue<ByteBuffer> freeBuffers;
	private BlockingQueue<DecodedFrame> readyFrames;

	private Texture texture;
	private Pixmap scratch;
	private Music music;
	private boolean hasAudio = false;
	private DecodedFrame holdback;

	public State getState() {
		return state;
	}

	public float getPosition() {
		return lastPosition;
	}

	public double getDuration() {
		return durationSec;
	}

	public boolean isPlaying() {
		return state == State.PLAYING;
	}

	public boolean hasAudio() {
		return hasAudio;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public @Nullable Texture texture() {
		return texture;
	}

	public float getVolume() {
		return volume;
	}

	public boolean isMuted() {
		return muted;
	}

	public boolean isLooping() {
		return looping;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	public void setVolume(float volume) {
		this.volume = volume < 0f ? 0f : volume > 1f ? 1f : volume;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public void setLooping(boolean loop) {
		this.looping = loop;
		if (music != null) {
			try {
				music.setLooping(loop);
			} catch (Throwable ignored) {
			}
		}
	}

	public void setCornerRadius(int radius) {
		this.cornerRadius = Math.max(0, radius);
	}

	public void open(Fi mp4File, @Nullable String audioUrl, boolean autoplay, Listener listener) {
		if (disposed || state != State.IDLE)
			return;
		this.mp4File = mp4File;
		this.audioUrl = audioUrl;
		this.autoplay = autoplay;
		this.listener = listener;
		this.state = State.LOADING;
		try {
			DECODE_WORKER.execute(this::openInBackground);
		} catch (Throwable t) {
			failOnMain(t);
		}
	}

	public void play() {
		if (disposed)
			return;
		if (state == State.PLAYING)
			return;
		if (state == State.PAUSED || state == State.READY) {
			state = State.PLAYING;
			startMusic();
			return;
		}
		if (state == State.ENDED) {
			seekInternal(0);
			reopen();
			pendingPlay = true;
			return;
		}
		if (state == State.LOADING) {
			pendingPlay = true;
		}
	}

	public void pause() {
		if (disposed || state != State.PLAYING)
			return;
		state = State.PAUSED;
		if (music != null) {
			try {
				music.pause(true);
			} catch (Throwable ignored) {
			}
		}
	}

	public void togglePlay() {
		if (state == State.PLAYING) {
			pause();
		} else {
			play();
		}
	}

	public void seek(double seconds) {
		if (disposed)
			return;
		if (state == State.IDLE || state == State.FAILED)
			return;
		double target = seconds < 0 ? 0 : seconds;
		if (durationSec > 0 && target > durationSec)
			target = durationSec;
		seekInternal(target);
		if (state == State.ENDED) {
			reopen();
			pendingPlay = true;
		} else if (state == State.PAUSED || state == State.READY) {
			singleFrame = true;
		}
	}

	private void reopen() {
		stopDecodeSession();
		closeStream();
		if (music != null) {
			try {
				music.dispose();
			} catch (Throwable ignored) {
			}
			music = null;
		}
		hasAudio = false;
		holdback = null;
		streamEnded = false;
		decodeDone = false;
		singleFrame = false;
		framesSinceLoop = 0;
		freeBuffers = null;
		readyFrames = null;
		lastAppliedVolume = -1f;
		lastAudioPosition = -1f;
		state = State.LOADING;
		try {
			DECODE_WORKER.execute(this::openInBackground);
		} catch (Throwable t) {
			failOnMain(t);
		}
	}

	/**
	 * Ends the running decode session and waits for its thread to finish. A session that is
	 * still winding down would push frames into the queues of the session that replaces it,
	 * which starves the new decoder and can pull the native reader out from under a running
	 * decode.
	 */
	private void stopDecodeSession() {
		sessionCounter.incrementAndGet();
		Thread previous = decodeThread;
		decodeThread = null;
		if (previous == null || previous == Thread.currentThread()) {
			return;
		}
		try {
			previous.interrupt();
		} catch (Throwable ignored) {
		}
		try {
			previous.join(500);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	private void seekInternal(double target) {
		seekEpoch++;
		seekTarget = target;
		pendingSeek = target;
		silentClock = (float) target;
		lastPosition = (float) target;
		lastAudioPosition = -1f;
		holdback = null;
		drainQueues();
		if (music != null) {
			try {
				music.setPosition((float) target);
			} catch (Throwable ignored) {
			}
		}
	}

	public void update(float delta) {
		if (disposed)
			return;
		// Guard against a huge delta after the window was backgrounded: otherwise the
		// clock jumps past the end and the player seeks forever without advancing.
		if (delta > 0.1f)
			delta = 0.1f;
		if (loopRestartRequest) {
			loopRestartRequest = false;
			lastAudioPosition = -1f;
			silentClock = 0f;
			lastPosition = 0f;
			holdback = null;
			if (music != null) {
				try {
					music.setPosition(0);
					if (state == State.PLAYING)
						music.play();
				} catch (Throwable ignored) {
				}
			}
		}
		if (state == State.PLAYING) {
			float audioPosition = audioPosition();
			boolean audioAdvancing = audioPosition > lastAudioPosition + AUDIO_EPSILON;
			if (audioPosition >= 0f)
				lastAudioPosition = audioPosition;
			float master = advanceClock(silentClock, delta, audioPosition, audioAdvancing);
			if (master < 0f)
				master = 0f;
			if (audioAdvancing && master - audioPosition > RESYNC_THRESHOLD)
				resyncAudio(master);
			silentClock = master;
			lastPosition = master;
			presentUpTo(master);
			boolean starved = streamEnded && decodeDone && readyFramesIsEmpty() && holdback == null;
			boolean pastEnd = durationSec > 0 && master >= durationSec - END_EPSILON;
			// A looping clip is restarted by the decode thread, which signals the restart
			// through loopRestartRequest. Seeking from here as well would fight that
			// restart and leave the presentation clock pinned at the end of the clip.
			if (starved || (pastEnd && !looping)) {
				toEnded();
			}
		} else if (state == State.PAUSED || state == State.READY || state == State.ENDED) {
			presentUpTo(Double.MAX_VALUE);
		}
		applyVolume();
	}

	static float advanceClock(float clock, float delta, float audioPosition, boolean audioAdvancing) {
		float next = clock + Math.max(0f, delta);
		if (!audioAdvancing)
			return next;
		float lead = audioPosition - next;
		return lead > 0f && lead <= RESYNC_THRESHOLD ? audioPosition : next;
	}

	private float audioPosition() {
		if (!hasAudio || music == null)
			return -1f;
		try {
			return music.getPosition();
		} catch (Throwable t) {
			return -1f;
		}
	}

	private void resyncAudio(float master) {
		try {
			music.setPosition(master);
		} catch (Throwable ignored) {
		}
	}

	private boolean readyFramesIsEmpty() {
		return readyFrames == null || readyFrames.isEmpty();
	}

	private void presentUpTo(double master) {
		DecodedFrame best = null;
		if (holdback != null) {
			if (holdback.pts <= master + 0.005) {
				best = holdback;
				holdback = null;
			} else {
				return;
			}
		}
		if (readyFrames != null) {
			DecodedFrame frame;
			while ((frame = readyFrames.poll()) != null) {
				if (frame.pts <= master + 0.005) {
					if (best != null)
						recycle(best);
					best = frame;
				} else {
					holdback = frame;
					break;
				}
			}
		}
		if (best != null) {
			upload(best.pixels);
			recycle(best);
		}
	}

	private void upload(ByteBuffer pixels) {
		if (texture == null || scratch == null)
			return;
		try {
			if (texture.isDisposed() || scratch.isDisposed())
				return;
		} catch (Throwable ignored) {
			return;
		}
		try {
			ByteBuffer dest = scratch.getPixels();
			dest.clear();
			// The converter leaves position at 0 and the limit at the produced byte count, so the copy
			// stays inside the finished frame instead of dragging along stale buffer content.
			dest.put(pixels);
			dest.flip();
			texture.draw(scratch, 0, 0);
		} catch (Throwable t) {
			Log.debug("SolimVideo: frame upload failed: " + t.getMessage());
		}
	}

	private void recycle(DecodedFrame frame) {
		if (frame != null && freeBuffers != null) {
			freeBuffers.offer(frame.pixels);
		}
	}

	private void toEnded() {
		state = State.ENDED;
		if (music != null) {
			try {
				music.stop();
			} catch (Throwable ignored) {
			}
		}
		if (listener != null) {
			try {
				listener.onEnded();
			} catch (Throwable ignored) {
			}
		}
	}

	private void startMusic() {
		if (music != null && hasAudio) {
			try {
				music.play();
			} catch (Throwable ignored) {
			}
		}
	}

	private void applyVolume() {
		if (music == null)
			return;
		float effective = muted ? 0f : volume * sfxVolume();
		if (effective != lastAppliedVolume) {
			lastAppliedVolume = effective;
			try {
				music.setVolume(effective);
			} catch (Throwable ignored) {
			}
		}
	}

	private static boolean audioReady() {
		try {
			return Core.audio != null && Core.audio.initialized();
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static float sfxVolume() {
		try {
			if (Core.settings != null)
				return Core.settings.getInt("sfxvol", 100) / 100f;
		} catch (Throwable ignored) {
		}
		return 1f;
	}

	@Override
	public void dispose() {
		if (disposed)
			return;
		disposed = true;
		// Invalidates the running session so a decoder that is still opening is released
		// instead of being installed into a player that nobody owns any more.
		sessionCounter.incrementAndGet();
		pendingPlay = false;
		if (music != null) {
			try {
				music.stop();
			} catch (Throwable ignored) {
			}
			try {
				music.dispose();
			} catch (Throwable ignored) {
			}
			music = null;
		}
		disposeQuietly();
	}

	private void openInBackground() {
		VideoFrameDecoder opened = null;
		int session = sessionCounter.incrementAndGet();
		try {
			File file = mp4File.file();
			opened = VideoDecoders.open(mp4File);
			if (session != sessionCounter.get()) {
				closeDecoder(opened);
				return;
			}
			VideoFrameDecoder instance = opened;
			decoder = instance;
			width = instance.width();
			height = instance.height();
			durationSec = instance.duration();

			freeBuffers = new ArrayBlockingQueue<>(FRAME_POOL);
			for (int i = 0; i < FRAME_POOL; i++) {
				freeBuffers.offer(YuvToRgbaConverter.allocateRgba(width, height));
			}
			readyFrames = new ArrayBlockingQueue<>(FRAME_POOL);

			hasAudio = transcodeAudio(file);
			postMain(() -> finishOpen(session, instance));
		} catch (Throwable t) {
			closeDecoder(opened);
			fail(t);
		}
	}

	private boolean transcodeAudio(File file) {
		try {
			wavFile = audioUrl != null ? VideoCache.audioFile(audioUrl) : null;
			if (wavFile == null)
				return false;
			if (VideoCache.isFreshAudio(wavFile))
				return true;
			return AacToWav.decode(file, wavFile.file()) != null;
		} catch (Throwable t) {
			Log.debug("SolimVideo: audio unavailable: " + t.getMessage());
			return false;
		}
	}

	private void finishOpen(int session, @Nullable VideoFrameDecoder instance) {
		if (disposed || state != State.LOADING || session != sessionCounter.get()) {
			// A newer session already owns the stream, so only this decoder is released.
			closeDecoder(instance);
			if (decoder == instance) {
				decoder = null;
			}
			return;
		}
		try {
			if (scratch == null || isDisposed(scratch)) {
				disposeQuietly();
				scratch = new Pixmap(width, height);
				texture = new Texture(width, height);
				texture.setFilter(TextureFilter.linear);
			}
			if (hasAudio && wavFile != null && audioReady()) {
				try {
					music = new Music(wavFile);
					music.setLooping(looping);
					if (!music.valid()) {
						music.dispose();
						music = null;
					}
				} catch (Throwable t) {
					music = null;
				}
			}
			hasAudio = music != null;
			state = State.READY;
			if (listener != null)
				listener.onReady(width, height, durationSec);
			try {
				DECODE_WORKER.execute(() -> decodeLoop(session));
			} catch (Throwable t) {
				failOnMain(t);
				return;
			}
			if (autoplay || pendingPlay) {
				pendingPlay = false;
				play();
			} else if (pendingSeek >= 0) {
				singleFrame = true;
			}
		} catch (Throwable t) {
			failOnMain(t);
		}
	}

	private static boolean isDisposed(Pixmap pixmap) {
		try {
			return pixmap.isDisposed();
		} catch (Throwable ignored) {
			return true;
		}
	}

	private void disposeQuietly() {
		if (texture != null) {
			try {
				texture.dispose();
			} catch (Throwable ignored) {
			}
			texture = null;
		}
		if (scratch != null) {
			try {
				scratch.dispose();
			} catch (Throwable ignored) {
			}
			scratch = null;
		}
	}

	private void decodeLoop(int session) {
		VideoFrameDecoder active = decoder;
		BlockingQueue<ByteBuffer> buffers = freeBuffers;
		BlockingQueue<DecodedFrame> frames = readyFrames;
		decodeThread = Thread.currentThread();
		try {
			while (!disposed && session == sessionCounter.get()) {
				State current = state;
				if (current != State.PLAYING && !singleFrame) {
					sleepBriefly();
					continue;
				}
				if (pendingSeek >= 0) {
					double target = pendingSeek;
					pendingSeek = -1;
					doSeek(active, target, frames, buffers);
					if (disposed)
						break;
					if (state != State.PLAYING && !singleFrame)
						continue;
				}
				if (streamEnded)
					break;
				ByteBuffer buffer;
				try {
					buffer = buffers.poll(50, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					break;
				}
				if (buffer == null || disposed) {
					if (buffer != null)
						buffers.offer(buffer);
					continue;
				}
				if (pendingSeek >= 0 || (state != State.PLAYING && !singleFrame)) {
					buffers.offer(buffer);
					continue;
				}
				double pts;
				if (active == null) {
					buffers.offer(buffer);
					break;
				}
				int epoch = seekEpoch;
				try {
					pts = active.decodeNext(buffer);
				} catch (Throwable t) {
					buffers.offer(buffer);
					VideoFrameDecoder software = switchToSoftwareDecoder(active);
					if (software != null) {
						active = software;
						continue;
					}
					fail(t);
					break;
				}
				// A seek that landed while this frame was being decoded invalidates it, unless the
				// frame already sits at the new position. Dropping every frame would starve the
				// display while the user scrubs, because each seek invalidates the frame that was
				// just decoded.
				if (epoch != seekEpoch && pts + 0.001 < seekTarget) {
					buffers.offer(buffer);
					continue;
				}
				if (pts < 0) {
					buffers.offer(buffer);
					if (!handleStreamEnd(active, frames, buffers))
						break;
					continue;
				}
				if (cornerRadius > 0)
					RgbaCorners.apply(buffer, width, height, cornerRadius);
				// H.264 frames cannot be skipped without breaking reference chains, so the decoder is
				// paced by the display instead: hold this frame until the queue has room rather than
				// decoding ahead and throwing the result away. Without this the loop outruns the
				// display and burns every core for the whole clip.
				DecodedFrame frame = new DecodedFrame(buffer, pts);
				boolean queued = false;
				try {
					while (!queued && !disposed && (state == State.PLAYING || singleFrame)) {
						queued = frames.offer(frame, FRAME_WAIT_MILLIS, TimeUnit.MILLISECONDS);
						if (!queued && !disposed && (state == State.PLAYING || singleFrame)) {
							// The queue stayed full: pace the decoder instead of abandoning the
							// stream. A fast native decoder can otherwise outrun a slow display,
							// time out here and exit the decode thread, which silently stops
							// playback (and looping) after the first pass. When the window is
							// backgrounded the display stops consuming, so wait patiently and
							// resume once it comes back instead of exiting.
							sleepBriefly();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (!queued) {
					buffers.offer(buffer);
					if (disposed)
						break;
					continue;
				}
				framesSinceLoop++;
				if (singleFrame)
					singleFrame = false;
			}
		} catch (Throwable t) {
			if (!disposed)
				fail(t);
		} finally {
			decodeDone = true;
			closeDecoder(active);
			if (decoder == active) {
				decoder = null;
			}
			decodeThread = null;
		}
	}

	private static void sleepBriefly() {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean handleStreamEnd(VideoFrameDecoder active, BlockingQueue<DecodedFrame> frames,
			BlockingQueue<ByteBuffer> buffers) {
		if (looping && !disposed && framesSinceLoop > 0) {
			framesSinceLoop = 0;
			doSeek(active, 0, frames, buffers);
			loopRestartRequest = true;
			return true;
		}
		streamEnded = true;
		return false;
	}

	private void doSeek(@Nullable VideoFrameDecoder active, double seconds,
			BlockingQueue<DecodedFrame> frames, BlockingQueue<ByteBuffer> buffers) {
		drainQueues(frames, buffers);
		if (active == null)
			return;
		try {
			active.seek(Math.max(0, seconds));
		} catch (Throwable t) {
			Log.err(t);
		}
	}

	private void drainQueues() {
		drainQueues(readyFrames, freeBuffers);
	}

	private static void drainQueues(@Nullable BlockingQueue<DecodedFrame> frames,
			@Nullable BlockingQueue<ByteBuffer> buffers) {
		if (frames == null || buffers == null)
			return;
		DecodedFrame frame;
		while ((frame = frames.poll()) != null) {
			buffers.offer(frame.pixels);
		}
	}

	private @Nullable VideoFrameDecoder switchToSoftwareDecoder(VideoFrameDecoder current) {
		if (current == null || !current.isNative())
			return null;
		try {
			VideoFrameDecoder software = VideoDecoders.openSoftware(mp4File);
			decoder = software;
			closeDecoder(current);
			drainQueues();
			software.seek(lastPosition);
			Log.warn("SolimVideo: native decoder failed, continuing with software decoding");
			return software;
		} catch (Throwable error) {
			Log.debug("SolimVideo: software fallback failed: " + error.getMessage());
			return null;
		}
	}

	private void closeStream() {
		VideoFrameDecoder active = decoder;
		decoder = null;
		closeDecoder(active);
	}

	private static void closeDecoder(@Nullable VideoFrameDecoder instance) {
		if (instance == null)
			return;
		try {
			instance.close();
		} catch (Throwable ignored) {
		}
	}

	private void fail(Throwable error) {
		postMain(() -> failOnMain(error));
	}

	private void failOnMain(Throwable error) {
		if (disposed) {
			closeStream();
			return;
		}
		state = State.FAILED;
		closeStream();
		Log.err("SolimVideo playback failed", error);
		if (listener != null) {
			try {
				listener.onFailed(error);
			} catch (Throwable ignored) {
			}
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
}
