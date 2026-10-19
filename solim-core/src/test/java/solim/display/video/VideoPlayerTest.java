package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import arc.files.Fi;
import java.io.File;
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
	void clockNeverRewindsOnNegativeDelta() {
		assertEquals(2f, VideoPlayer.advanceClock(2f, -0.5f, -1f, false), 1e-4f);
	}
}
