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

	private void seekInternal(double target) {
		pendingSeek = target;
		silentClock = (float) target;
		lastPosition = (float) target;
		lastAudioPosition = -1f;
		holdback = null;
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
			if (starved || (pastEnd && !looping)) {
				toEnded();
			} else if (pastEnd) {
				seekInternal(0);
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
		try {
			File file = mp4File.file();
			decoder = VideoDecoders.open(mp4File);
			width = decoder.width();
			height = decoder.height();
			durationSec = decoder.duration();

			freeBuffers = new ArrayBlockingQueue<>(FRAME_POOL);
			for (int i = 0; i < FRAME_POOL; i++) {
				freeBuffers.offer(YuvToRgbaConverter.allocateRgba(width, height));
			}
			readyFrames = new ArrayBlockingQueue<>(FRAME_POOL);

			hasAudio = transcodeAudio(file);
			postMain(this::finishOpen);
		} catch (Throwable t) {
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

	private void finishOpen() {
		if (disposed || state != State.LOADING) {
			closeStream();
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
				DECODE_WORKER.execute(this::decodeLoop);
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

	private void decodeLoop() {
		try {
			while (!disposed) {
				State current = state;
				if (current != State.PLAYING && !singleFrame) {
					sleepBriefly();
					continue;
				}
				if (pendingSeek >= 0) {
					double target = pendingSeek;
					pendingSeek = -1;
					doSeek(target);
					if (disposed)
						break;
					if (state != State.PLAYING && !singleFrame)
						continue;
				}
				if (streamEnded)
					break;
				ByteBuffer buffer;
				try {
					buffer = freeBuffers.poll(50, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					break;
				}
				if (buffer == null || disposed) {
					if (buffer != null)
						freeBuffers.offer(buffer);
					continue;
				}
				if (pendingSeek >= 0 || (state != State.PLAYING && !singleFrame)) {
					freeBuffers.offer(buffer);
					continue;
				}
				double pts;
				try {
					pts = decoder.decodeNext(buffer);
				} catch (Throwable t) {
					freeBuffers.offer(buffer);
					if (switchToSoftwareDecoder())
						continue;
					fail(t);
					break;
				}
				if (pts < 0) {
					freeBuffers.offer(buffer);
					if (!handleStreamEnd())
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
				int offerAttempts = 0;
				try {
					while (!queued && !disposed) {
						queued = readyFrames.offer(frame, FRAME_WAIT_MILLIS, TimeUnit.MILLISECONDS);
						if (!queued && !disposed) {
							// The queue stayed full: pace the decoder instead of abandoning the
							// stream. A fast native decoder can otherwise outrun a slow display,
							// time out here and exit the decode thread, which silently stops
							// playback (and looping) after the first pass.
							sleepBriefly();
							if (++offerAttempts > 20) {
								// Give up waiting so a stuck display cannot pin the thread forever.
								break;
							}
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (!queued) {
					freeBuffers.offer(buffer);
					break;
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
			closeStream();
		}
	}

	private static void sleepBriefly() {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean handleStreamEnd() {
		if (looping && !disposed && framesSinceLoop > 0) {
			framesSinceLoop = 0;
			doSeek(0);
			loopRestartRequest = true;
			return true;
		}
		streamEnded = true;
		return false;
	}

	private void doSeek(double seconds) {
		drainQueues();
		VideoFrameDecoder active = decoder;
		if (active == null)
			return;
		try {
			active.seek(Math.max(0, seconds));
		} catch (Throwable t) {
			fail(t);
		}
	}

	private void drainQueues() {
		if (readyFrames == null || freeBuffers == null)
			return;
		DecodedFrame frame;
		while ((frame = readyFrames.poll()) != null) {
			freeBuffers.offer(frame.pixels);
		}
	}

	private boolean switchToSoftwareDecoder() {
		VideoFrameDecoder active = decoder;
		if (active == null || !active.isNative())
			return false;
		try {
			VideoFrameDecoder software = VideoDecoders.openSoftware(mp4File);
			decoder = software;
			active.close();
			drainQueues();
			software.seek(lastPosition);
			Log.warn("SolimVideo: native decoder failed, continuing with software decoding");
			return true;
		} catch (Throwable error) {
			Log.debug("SolimVideo: software fallback failed: " + error.getMessage());
			return false;
		}
	}

	private void closeStream() {
		VideoFrameDecoder active = decoder;
		decoder = null;
		if (active != null) {
			try {
				active.close();
			} catch (Throwable ignored) {
			}
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
