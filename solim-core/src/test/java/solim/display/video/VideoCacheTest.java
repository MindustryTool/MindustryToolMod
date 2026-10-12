package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

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
		assertEquals(stem + ".wav", VideoCache.audioName(url));
	}

	@Test
	void freshnessRejectsNullWithoutThrowing() {
		assertFalse(VideoCache.isFresh(null));
	}

	@Test
	void diskWriteWithoutCacheDirIsIgnored() {
		assertDoesNotThrow(() -> VideoCache.writeVideo("https://example.com/clip.mp4", new byte[] { 1, 2, 3 }));
	}
}
