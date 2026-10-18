package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class VideoPlayerTest extends SolimEnv {

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
}
