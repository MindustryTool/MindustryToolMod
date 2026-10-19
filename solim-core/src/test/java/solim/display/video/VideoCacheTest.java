package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import arc.files.Fi;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class VideoCacheTest {

	@Test
	void cacheNameIsStableLowercaseHex() {
		String first = VideoCache.cacheName("https://example.com/clip.mp4");
		String second = VideoCache.cacheName("https://example.com/clip.mp4");
		assertEquals(64, first.length());
		assertEquals(first, second);
		assertTrue(first.matches("[0-9a-f]+"));
	}

	@Test
	void cacheNameDiffersPerUrl() {
		assertNotEquals(VideoCache.cacheName("https://example.com/a.mp4"),
				VideoCache.cacheName("https://example.com/b.mp4"));
	}

	@Test
	void mediaNamesShareHashStemWithDistinctSuffixes() {
		String url = "https://example.com/clip.mp4";
		String stem = VideoCache.cacheName(url);
		assertEquals(stem + ".mp4", VideoCache.videoName(url));
		assertEquals(stem + "." + VideoCache.AUDIO_CACHE_VERSION + ".wav", VideoCache.audioName(url));
	}

	@Test
	void freshnessRejectsNullWithoutThrowing() {
		assertFalse(VideoCache.isFreshVideo(null));
		assertFalse(VideoCache.isFreshAudio(null));
	}

	@Test
	void audioEntryNoLargerThanHeaderIsStale() throws Exception {
		File file = File.createTempFile("solim-cache-audio", ".wav");
		file.deleteOnExit();
		Fi audio = new Fi(file);

		audio.writeBytes(new byte[VideoCache.WAV_HEADER_BYTES]);
		assertFalse(VideoCache.isFreshAudio(audio));

		audio.writeBytes(new byte[VideoCache.WAV_HEADER_BYTES + 2]);
		assertTrue(VideoCache.isFreshAudio(audio));
	}

	@Test
	void videoEntryThatCannotHoldAContainerIsStale() throws Exception {
		File directory = Files.createTempDirectory("solim-cache-video").toFile();
		assertFalse(VideoCache.isFreshVideo(new Fi(directory)));

		File file = File.createTempFile("solim-cache-video", ".mp4");
		file.deleteOnExit();
		Fi video = new Fi(file);

		video.writeBytes(new byte[(int) VideoCache.MIN_VIDEO_BYTES - 1]);
		assertFalse(VideoCache.isFreshVideo(video));

		video.writeBytes(new byte[(int) VideoCache.MIN_VIDEO_BYTES]);
		assertTrue(VideoCache.isFreshVideo(video));
	}

	@Test
	void diskWriteWithoutCacheDirIsIgnored() {
		assertDoesNotThrow(() -> VideoCache.writeVideo("https://example.com/clip.mp4", new byte[] { 1, 2, 3 }));
	}
}
