package solim.display.video;

import arc.util.Log;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.logging.Message;

final class VideoLog {

	private static final String BENIGN_PREFIX = "Broken atom of size";

	private static volatile boolean installed = false;

	private VideoLog() {
	}

	static void install() {
		if (installed)
			return;
		installed = true;
		try {
			Logger.addSink(VideoLog::post);
		} catch (Throwable ignored) {
		}
	}

	static void post(Message message) {
		String text = message.getMessage() != null ? message.getMessage() : "";
		switch (message.getLevel()) {
			case ERROR:
				if (text.startsWith(BENIGN_PREFIX))
					Log.debug("jcodec: " + text);
				else
					Log.err("jcodec: " + text);
				break;
			case WARN:
				Log.warn("jcodec: " + text);
				break;
			default:
				Log.debug("jcodec: " + text);
				break;
		}
	}
}
