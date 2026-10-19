package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class AudioPipelineTest extends SolimEnv {

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

	@Test
	void thePreviewClipHasAnAudioTrackThatCanBeExtracted() throws Exception {
		File clip = previewClip();
		assertNotNull(clip, "preview/quick-rotate.mp4 is not available");

		File wav = File.createTempFile("solim-audio", ".wav");
		wav.deleteOnExit();

		AacToWav.AudioInfo info = AacToWav.decode(clip, wav);
		assertNotNull(info, "the audio track of the clip must be extractable");
		assertTrue(info.sampleRate > 0, "the extracted audio must report a sample rate");
		assertTrue(info.channels > 0, "the extracted audio must report a channel count");
		assertTrue(wav.length() > 44, "the extracted audio must hold samples");
		assertTrue(wav.length() > 0, "the wave file must exist");
	}
}
