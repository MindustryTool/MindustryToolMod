package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import arc.files.Fi;
import java.io.File;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class VideoPlayerTest extends SolimEnv {

	private static File previewClip() {
		File dir = new File(System.getProperty("user.dir"));
		while (dir != null) {
			File candidate = new File(dir, "preview/quick-rotate.mp4");
			if (candidate.isFile())
				return candidate;
			dir = dir.getParentFile();
		}
		return null;
	}

	private static VideoPlayer.Listener accepting() {
		return new VideoPlayer.Listener() {
			@Override
			public void onReady(int width, int height, double durationSec) {
			}

			@Override
			public void onEnded() {
			}

			@Override
			public void onFailed(Throwable error) {
			}
		};
	}

	@Test
	void playbackOfArealClipAdvancesThroughItBeforeEnding() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		VideoPlayer player = new VideoPlayer();
		try {
			player.setLooping(false);
			player.open(new Fi(clip), null, false, accepting());

			for (int i = 0; i < 2000 && player.getState() == VideoPlayer.State.LOADING; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}
			assertEquals(VideoPlayer.State.READY, player.getState(), "the clip must load before it can play");
			assertEquals(11.096, player.getDuration(), 0.5);

			player.play();
			for (int i = 0; i < 2000 && player.getState() != VideoPlayer.State.ENDED; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}

			assertEquals(VideoPlayer.State.ENDED, player.getState(), "a non-looping clip must end");
			assertTrue(player.getPosition() > 5f,
					"playback must be driven by decoded frames, not end at the start; was " + player.getPosition());
		} finally {
			player.dispose();
		}
	}

	@Test
	void playbackOfALoopingClipRestartsInsteadOfEnding() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		VideoPlayer player = new VideoPlayer();
		try {
			player.setLooping(true);
			player.open(new Fi(clip), null, false, accepting());

			for (int i = 0; i < 2000 && player.getState() == VideoPlayer.State.LOADING; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}
			assertEquals(VideoPlayer.State.READY, player.getState(), "the clip must load before it can play");
			player.play();

			float previous = player.getPosition();
			boolean restarted = false;
			for (int i = 0; i < 1600 && player.getState() != VideoPlayer.State.ENDED; i++) {
				player.update(1f / 60f);
				float position = player.getPosition();
				if (position + 1f < previous)
					restarted = true;
				previous = position;
				Thread.sleep(2);
			}

			assertTrue(restarted, "a looping clip must restart its presentation clock");
			assertEquals(VideoPlayer.State.PLAYING, player.getState(), "a looping clip must not end");
		} finally {
			player.dispose();
		}
	}

	@Test
	void thePlaybackClockStaysOnTheDecodedTimelineAfterRapidScrubbing() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		VideoPlayer player = new VideoPlayer();
		try {
			player.setLooping(true);
			player.open(new Fi(clip), null, false, accepting());

			for (int i = 0; i < 2000 && player.getState() == VideoPlayer.State.LOADING; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}
			assertEquals(VideoPlayer.State.READY, player.getState(), "the clip must load before it can play");
			player.play();
			for (int i = 0; i < 30; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}

			for (int i = 0; i < 60; i++) {
				player.seek(i % 2 == 0 ? 1.0 : 8.0);
				player.update(1f / 60f);
				Thread.sleep(2);
			}
			int presented = player.presentedFrameCount();
			for (int i = 0; i < 60; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}

			assertTrue(player.presentedFrameCount() > presented,
					"the display must keep receiving frames after scrubbing");
			double decoded = player.decodedPosition();
			assertTrue(decoded >= 0, "the decoder must still be producing frames after scrubbing");
			assertTrue(Math.abs(player.getPosition() - decoded) < 2.0,
					"the playback clock must follow the decoded frames; clock=" + player.getPosition()
							+ " decoded=" + decoded);
		} finally {
			player.dispose();
		}
	}

	/**
	 * A decoder that hands out one frame from the position it was at before the seek before
	 * it continues from the requested one, which is what a hardware source reader does while
	 * it catches up with the new position.
	 */
	private static final class StaleFrameDecoder implements VideoFrameDecoder {

		private double position;
		private boolean stalePending;
		private final double duration;

		StaleFrameDecoder(double duration) {
			this.duration = duration;
		}

		@Override
		public String name() {
			return "stale";
		}

		@Override
		public boolean isNative() {
			return true;
		}

		@Override
		public int width() {
			return 16;
		}

		@Override
		public int height() {
			return 16;
		}

		@Override
		public double duration() {
			return duration;
		}

		@Override
		public double frameRate() {
			return 30;
		}

		@Override
		public double decodeNext(ByteBuffer rgba) {
			if (stalePending) {
				stalePending = false;
				return position + 30;
			}
			position += 1 / 30.0;
			if (position > duration)
				return -1;
			return position;
		}

		@Override
		public void seek(double seconds) {
			position = seconds;
			stalePending = true;
		}

		@Override
		public void close() {
		}
	}

	@Test
	void aFrameFromThePreviousPositionDoesNotFreezeTheDisplay() throws Exception {
		StaleFrameDecoder decoder = new StaleFrameDecoder(20);
		VideoDecoders.setFactory(file -> decoder);
		VideoPlayer player = new VideoPlayer();
		try {
			player.setLooping(true);
			player.open(new Fi(new File("unused.mp4")), null, false, accepting());

			for (int i = 0; i < 2000 && player.getState() == VideoPlayer.State.LOADING; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}
			assertEquals(VideoPlayer.State.READY, player.getState(), "the clip must load before it can play");
			player.play();
			for (int i = 0; i < 60; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}

			// Jump backwards: the decoder answers with a frame from the old position first.
			player.seek(2);
			int presented = player.presentedFrameCount();
			for (int i = 0; i < 120; i++) {
				player.update(1f / 60f);
				Thread.sleep(2);
			}

			assertTrue(player.presentedFrameCount() > presented,
					"a frame from the previous position must not stop the display");
			assertTrue(Math.abs(player.getPosition() - player.decodedPosition()) < 2.0,
					"the playback clock must follow the decoded frames; clock=" + player.getPosition()
							+ " decoded=" + player.decodedPosition());
		} finally {
			player.dispose();
			VideoDecoders.setFactory(null);
		}
	}

	@Test
	void initialStateIsIdle() {
		VideoPlayer player = new VideoPlayer();
		assertEquals(VideoPlayer.State.IDLE, player.getState());
		assertFalse(player.isPlaying());
		assertFalse(player.hasAudio());
		assertEquals(0f, player.getPosition());
		assertEquals(0.0, player.getDuration());
		assertFalse(player.isDisposed());
		player.dispose();
	}

	@Test
	void volumeIsClamped() {
		VideoPlayer player = new VideoPlayer();
		player.setVolume(2f);
		assertEquals(1f, player.getVolume());
		player.setVolume(-1f);
		assertEquals(0f, player.getVolume());
		player.setVolume(0.5f);
		assertEquals(0.5f, player.getVolume());
		player.dispose();
	}

	@Test
	void flagsAreRetained() {
		VideoPlayer player = new VideoPlayer();
		player.setMuted(true);
		player.setLooping(true);
		player.setCornerRadius(8);
		assertTrue(player.isMuted());
		assertTrue(player.isLooping());
		player.setMuted(false);
		assertFalse(player.isMuted());
		player.dispose();
	}

	@Test
	void transportIsInertWithoutMedia() {
		VideoPlayer player = new VideoPlayer();
		player.play();
		assertEquals(VideoPlayer.State.IDLE, player.getState());
		player.pause();
		player.togglePlay();
		assertEquals(VideoPlayer.State.IDLE, player.getState());
		player.seek(10);
		assertEquals(0f, player.getPosition());
		player.update(0.016f);
		assertEquals(0f, player.getPosition());
		player.dispose();
	}

	@Test
	void disposeIsIdempotent() {
		VideoPlayer player = new VideoPlayer();
		player.dispose();
		assertTrue(player.isDisposed());
		assertDoesNotThrow(player::dispose);
		player.play();
		player.pause();
		player.seek(5);
		player.update(0.016f);
	}

	@Test
	void clockAdvancesFromWallTimeWhileAudioIsInert() {
		float clock = 0f;
		for (int tick = 0; tick < 3; tick++) {
			clock = VideoPlayer.advanceClock(clock, 0.016f, 0f, true);
		}
		assertEquals(0.048f, clock, 1e-4f);
	}

	@Test
	void clockAdvancesWhenAudioIsNotAdvancing() {
		assertEquals(0.016f, VideoPlayer.advanceClock(0f, 0.016f, 0f, false), 1e-4f);
		assertEquals(5.016f, VideoPlayer.advanceClock(5f, 0.016f, 0f, false), 1e-4f);
	}

	@Test
	void clockIgnoresAudioOutsideTheResyncWindow() {
		assertEquals(0.016f, VideoPlayer.advanceClock(0f, 0.016f, 5f, true), 1e-4f);
		assertEquals(1.016f, VideoPlayer.advanceClock(1f, 0.016f, 0.5f, true), 1e-4f);
	}

	@Test
	void clockAdoptsAudioSlightlyAhead() {
		assertEquals(1.02f, VideoPlayer.advanceClock(1f, 0.016f, 1.02f, true), 1e-4f);
	}

	@Test
	void clockAdoptsAudioThatLagsSlightly() {
		// A picture that runs ahead of the sound is the offset a viewer notices, so the
		// clock has to give way to a sound that trails it a little.
		assertEquals(0.9f, VideoPlayer.advanceClock(1f, 0.016f, 0.9f, true), 1e-4f);
	}

	@Test
	void clockNeverRewindsOnNegativeDelta() {
		assertEquals(2f, VideoPlayer.advanceClock(2f, -0.5f, -1f, false), 1e-4f);
	}
}
