package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.RandomAccessFile;
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
	}

	@Test
	void theExtractedAudioHoldsSoundAndNotOnlySilence() throws Exception {
		File clip = previewClip();
		assertNotNull(clip, "preview/quick-rotate.mp4 is not available");

		File wav = File.createTempFile("solim-audio", ".wav");
		wav.deleteOnExit();

		assertNotNull(AacToWav.decode(clip, wav), "the audio track must be extractable");

		int head = 0;
		int tail = 0;
		try (RandomAccessFile raf = new RandomAccessFile(wav, "r")) {
			raf.seek(44);
			byte[] pcm = new byte[(int) (raf.length() - 44)];
			raf.readFully(pcm);
			int third = pcm.length / 3;
			for (int index = 0; index + 1 < third; index += 2) {
				head = Math.max(head, Math.abs(value(pcm, index)));
			}
			for (int index = pcm.length - third; index + 1 < pcm.length; index += 2) {
				tail = Math.max(tail, Math.abs(value(pcm, index)));
			}
		}
		assertTrue(head > 0, "the extracted audio must hold sound, but the peak sample is " + head);
		assertTrue(tail > 0,
				"the sound must survive the whole track, but the last third peaks at " + tail);
	}

	private static int value(byte[] pcm, int index) {
		return Math.abs((short) ((pcm[index] & 0xff) | (pcm[index + 1] << 8)));
	}
}
