package mindustrytool.features.music;

import static org.junit.jupiter.api.Assertions.*;

import arc.files.Fi;
import arc.util.ArcRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solim.test.SolimEnv;

class MusicTrackImportTest extends SolimEnv {

    @Test
    void isReadableReturnsFalseForNull() {
        assertFalse(MusicFeature.isReadable(null));
    }

    @Test
    void isReadableReturnsTrueForExistingLocalFile(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "test.mp3");
        assertTrue(file.createNewFile());
        Fi fi = new Fi(file);

        assertTrue(fi.exists());
        assertTrue(MusicFeature.isReadable(fi));
    }

    @Test
    void isReadableReturnsTrueForAndroidSafVirtualFile() {
        // Simulates an Android SAF anonymous Fi where file().exists() is false,
        // but read() succeeds via ContentResolver openInputStream.
        byte[] content = "fake-mp3-bytes".getBytes();
        Fi androidSafFi = new Fi("/document/audio:12345") {
            @Override
            public boolean exists() {
                return false;
            }

            @Override
            public InputStream read() {
                return new ByteArrayInputStream(content);
            }
        };

        assertFalse(androidSafFi.exists(), "Android SAF content URI must report exists() == false");
        assertTrue(MusicFeature.isReadable(androidSafFi), "isReadable must succeed when stream is available");
    }

    @Test
    void isReadableReturnsFalseWhenStreamFails() {
        Fi failingFi = new Fi("/document/invalid") {
            @Override
            public boolean exists() {
                return false;
            }

            @Override
            public InputStream read() {
                throw new ArcRuntimeException("File not found or permission denied");
            }
        };

        assertFalse(MusicFeature.isReadable(failingFi));
    }

    @Test
    void sanitizeFileNameRemovesIllegalCharacters() {
        assertEquals("audio_12345", MusicFeature.sanitizeFileName("audio:12345"));
        assertEquals("track_name", MusicFeature.sanitizeFileName("track/name"));
        assertEquals("song_title", MusicFeature.sanitizeFileName("song\\title?*\"<:>|"));
        assertEquals("clean_track.mp3", MusicFeature.sanitizeFileName("clean_track.mp3"));
        assertEquals("track", MusicFeature.sanitizeFileName("   "));
    }

    @Test
    void extensionOfExtractsCorrectExtension() {
        assertEquals("mp3", MusicFeature.extensionOf("track.mp3"));
        assertEquals("ogg", MusicFeature.extensionOf("MUSIC.OGG"));
        assertEquals("wav", MusicFeature.extensionOf("effect.final.wav"));
        assertEquals("", MusicFeature.extensionOf("audio_12345"));
        assertEquals("", MusicFeature.extensionOf("no_extension"));
    }

    @Test
    void resolveTrackNameAppendsDefaultExtensionWhenMissing() {
        // On Android, content URI document IDs often have no file extension
        Fi safFiWithoutExt = new Fi("/document/audio:1000000034") {
            @Override
            public String name() {
                return "audio:1000000034";
            }
        };

        String resolved = MusicFeature.resolveTrackName(safFiWithoutExt);
        assertEquals("audio_1000000034.mp3", resolved);
        assertTrue(MusicFeature.VALID_EXTENSIONS.contains(MusicFeature.extensionOf(resolved)));
    }

    @Test
    void resolveTrackNamePreservesExistingExtension() {
        Fi safFiWithExt = new Fi("/document/primary:Music/my_song.ogg") {
            @Override
            public String name() {
                return "primary:Music_my_song.ogg";
            }
        };

        String resolved = MusicFeature.resolveTrackName(safFiWithExt);
        assertEquals("primary_Music_my_song.ogg", resolved);
        assertEquals("ogg", MusicFeature.extensionOf(resolved));
    }
}
