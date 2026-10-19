package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import arc.util.Log;
import arc.util.Log.LogHandler;
import java.util.ArrayList;
import java.util.List;
import org.jcodec.common.logging.LogLevel;
import org.jcodec.common.logging.Message;
import org.junit.jupiter.api.Test;

class VideoLogTest {

	private static Message message(LogLevel level, String text) {
		return new Message(level, "", "", "", 0, text, new Object[0]);
	}

	@Test
	void benignContainerNoiseIsNotReportedAsError() {
		LogHandler previousLogger = Log.logger;
		Log.LogLevel previousLevel = Log.level;
		List<String> errors = new ArrayList<>();
		List<String> debugs = new ArrayList<>();
		Log.level = Log.LogLevel.debug;
		Log.logger = (level, text) -> {
			if (level == Log.LogLevel.err) {
				errors.add(text);
			} else if (level == Log.LogLevel.debug) {
				debugs.add(text);
			}
		};
		try {
			VideoLog.post(message(LogLevel.ERROR, "Broken atom of size 0"));
			assertTrue(errors.isEmpty(), "benign metadata noise must not surface as an error");
			assertEquals(1, debugs.size());

			VideoLog.post(message(LogLevel.ERROR, "moov atom not found"));
			assertEquals(1, errors.size(), "genuine container failures must still surface");
		} finally {
			Log.level = previousLevel;
			Log.logger = previousLogger;
		}
	}
}
