package mindustrytool.update;

import static org.junit.jupiter.api.Assertions.*;

import mindustrytool.services.update.VersionUtils;
import org.junit.jupiter.api.Test;

class VersionUtilsTest {

	@Test
	void parseVersion_null_returnsEmpty() {
		assertArrayEquals(new int[0], VersionUtils.parseVersion(null));
	}

	@Test
	void parseVersion_empty_returnsEmpty() {
		assertArrayEquals(new int[0], VersionUtils.parseVersion(""));
		assertArrayEquals(new int[0], VersionUtils.parseVersion("   "));
	}

	@Test
	void parseVersion_invalid_returnsEmpty() {
		assertArrayEquals(new int[0], VersionUtils.parseVersion("abc"));
		assertArrayEquals(new int[0], VersionUtils.parseVersion("v"));
		assertArrayEquals(new int[0], VersionUtils.parseVersion("-beta"));
	}

	@Test
	void parseVersion_simple() {
		assertArrayEquals(new int[] {1, 2, 3}, VersionUtils.parseVersion("1.2.3"));
		assertArrayEquals(new int[] {0}, VersionUtils.parseVersion("0"));
	}

	@Test
	void parseVersion_withVPrefix() {
		assertArrayEquals(new int[] {8}, VersionUtils.parseVersion("v8"));
		assertArrayEquals(new int[] {8, 0}, VersionUtils.parseVersion("v8.0"));
		assertArrayEquals(new int[] {1, 2, 3}, VersionUtils.parseVersion("v1.2.3"));
	}

	@Test
	void parseVersion_withSuffixDash() {
		assertArrayEquals(new int[] {1, 2, 3}, VersionUtils.parseVersion("1.2.3-beta"));
		assertArrayEquals(new int[] {8}, VersionUtils.parseVersion("v8-136"));
		assertArrayEquals(new int[] {1, 0}, VersionUtils.parseVersion("1.0-alpha+001"));
	}

	@Test
	void parseVersion_withPlusSuffix() {
		assertArrayEquals(new int[] {2, 0}, VersionUtils.parseVersion("2.0+build"));
		assertArrayEquals(new int[] {1, 2, 3}, VersionUtils.parseVersion("1.2.3+20130313"));
	}

	@Test
	void parseVersion_vWithoutDash_noCrash() {
		// legacy bug: v present without '-' crashed; should not throw
		assertDoesNotThrow(() -> VersionUtils.parseVersion("v8"));
		assertDoesNotThrow(() -> VersionUtils.parseVersion("v1.2.3"));
		assertArrayEquals(new int[] {8}, VersionUtils.parseVersion("v8"));
	}

	@Test
	void parseVersion_malformedDots() {
		assertArrayEquals(new int[] {1, 2}, VersionUtils.parseVersion("1..2"));
		assertArrayEquals(new int[] {1, 2}, VersionUtils.parseVersion("1..2."));
		assertArrayEquals(new int[] {1, 2}, VersionUtils.parseVersion(".1.2."));
	}

	@Test
	void isGreater_equal_false() {
		assertFalse(VersionUtils.isGreater(new int[] {1, 2}, new int[] {1, 2}));
		assertFalse(VersionUtils.isGreater(new int[0], new int[0]));
	}

	@Test
	void isGreater_majorGreater() {
		assertTrue(VersionUtils.isGreater(new int[] {2, 0, 0}, new int[] {1, 9, 9}));
		assertTrue(VersionUtils.isGreater(new int[] {1, 3, 0}, new int[] {1, 2, 9}));
	}

	@Test
	void isGreater_lesser() {
		assertFalse(VersionUtils.isGreater(new int[] {1, 2, 9}, new int[] {1, 3, 0}));
		assertFalse(VersionUtils.isGreater(new int[] {1, 0}, new int[] {1, 1}));
	}

	@Test
	void isGreater_longerWins() {
		assertTrue(VersionUtils.isGreater(new int[] {1, 2, 0}, new int[] {1, 2}));
		assertFalse(VersionUtils.isGreater(new int[] {1, 2}, new int[] {1, 2, 0}));
	}

	@Test
	void isGreater_nullSafe() {
		assertFalse(VersionUtils.isGreater(null, null));
		assertTrue(VersionUtils.isGreater(new int[] {1}, null));
		assertFalse(VersionUtils.isGreater(null, new int[] {1}));
	}

	@Test
	void format_empty() {
		assertEquals("", VersionUtils.format(new int[0]));
		assertEquals("", VersionUtils.format(null));
	}

	@Test
	void format_single() {
		assertEquals("8", VersionUtils.format(new int[] {8}));
	}

	@Test
	void format_multi() {
		assertEquals("1.2.3", VersionUtils.format(new int[] {1, 2, 3}));
	}

	@Test
	void format_roundTrip() {
		int[] parsed = VersionUtils.parseVersion("1.2.3-beta");
		assertEquals("1.2.3", VersionUtils.format(parsed));
	}
}
