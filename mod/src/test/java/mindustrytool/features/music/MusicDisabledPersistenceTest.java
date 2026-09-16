package mindustrytool.features.music;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import arc.audio.Music;
import arc.struct.Seq;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Verifies the list-based disabled model: persistence across reloads, the
 * original/custom key format, reactive state derivation, and pruning of stale
 * custom entries.
 */
class MusicDisabledPersistenceTest {

    @BeforeAll
    static void initSettings() {
        Core.settings = new Settings();
    }

    @AfterAll
    static void clearSettings() {
        Core.settings.clear();
    }

    @Test
    void disabledTracksPersistAcrossReload() {
        String groupNamespace = "mindustrytool.features.music";
        OrderedSeqPersister persister = new OrderedSeqPersister();

        ConfigValue<Seq<String>> disabled = ConfigGroup.of(groupNamespace)
                .value("disabled", new Seq<>(), persister);
        disabled.set(Seq.with("AMBIENT_o:song1", "DARK_c:boss-theme"));

        // Simulate a fresh application start: a new ConfigValue reading the same key
        ConfigValue<Seq<String>> reloaded = ConfigGroup.of(groupNamespace)
                .value("disabled", new Seq<>(), persister);

        assertTrue(reloaded.get().contains("AMBIENT_o:song1"), "disabled track should survive reload");
        assertTrue(reloaded.get().contains("DARK_c:boss-theme"), "disabled track should survive reload");
        assertEquals(2, reloaded.get().size);
    }

    @Test
    void reEnablingRemovesKeyPersistently() {
        String groupNamespace = "mindustrytool.features.music";
        OrderedSeqPersister persister = new OrderedSeqPersister();

        ConfigValue<Seq<String>> disabled = ConfigGroup.of(groupNamespace)
                .value("disabled", new Seq<>(), persister);
        disabled.set(Seq.with("AMBIENT_o:song1"));
        disabled.set(new Seq<>());

        ConfigValue<Seq<String>> reloaded = ConfigGroup.of(groupNamespace)
                .value("disabled", new Seq<>(), persister);
        assertFalse(reloaded.get().contains("AMBIENT_o:song1"), "re-enabled track should stay enabled after reload");
    }

    @Test
    void keyFormatDistinguishesOriginalAndCustom() {
        assertEquals("AMBIENT_o:song1", TrackState.disabledKey(MusicType.AMBIENT, "song1", false));
        assertEquals("AMBIENT_c:song1", TrackState.disabledKey(MusicType.AMBIENT, "song1", true));
    }

    @Test
    void trackStateDerivesDisabledAndPlaying() {
        Music music = new Music();
        Signal<Seq<String>> disabled = Signal.of(new Seq<>());
        Signal<Music> current = Signal.of((Music) null);
        TrackState state = new TrackState(music, MusicType.AMBIENT, "song", true, disabled, current);

        //bind once and reuse, as the UI does, to prove the derived readable is reactive
        Readable<Boolean> isDisabled = state.isDisabled();
        Readable<Boolean> isPlaying = state.isPlaying();

        assertFalse(isDisabled.get());
        assertFalse(isPlaying.get());

        disabled.set(Seq.with(state.disabledKey()));
        assertTrue(isDisabled.get(), "disabled state must follow the persisted list");

        current.set(music);
        assertTrue(isPlaying.get(), "playing state must follow the game's current track");

        current.set(null);
        assertFalse(isPlaying.get(), "same readable must react to the track stopping");
    }

    @Test
    void pruneRemovesOnlyMissingCustomKeysOfThatType() {
        Seq<String> disabled = Seq.with("AMBIENT_o:song1", "AMBIENT_c:gone", "AMBIENT_c:kept", "DARK_c:other");
        Seq<String> validCustomKeys = Seq.with("AMBIENT_c:kept");

        Seq<String> pruned = MusicFeature.pruneDisabled(disabled, MusicType.AMBIENT, validCustomKeys);

        assertTrue(pruned.contains("AMBIENT_o:song1"), "original entries must be kept");
        assertTrue(pruned.contains("AMBIENT_c:kept"), "existing custom entries must be kept");
        assertFalse(pruned.contains("AMBIENT_c:gone"), "missing custom entry must be pruned");
        assertTrue(pruned.contains("DARK_c:other"), "entries of other types must be untouched");
    }

    @Test
    void slotFallsBackToVanillaWhenNothingPlayable() {
        Music vanilla = new Music();
        Music picked = MusicFeature.pickSlotMusic(new Seq<>(), vanilla, vanilla);
        assertSame(vanilla, picked, "empty slot list must fall back to the vanilla track");
    }

    @Test
    void slotAvoidsRepeatingCurrentTrack() {
        Music vanilla = new Music();
        Music a = new Music();
        Music b = new Music();

        assertSame(a, MusicFeature.pickSlotMusic(Seq.with(a), vanilla, b),
                "single playable track is chosen regardless of current");

        Music picked = MusicFeature.pickSlotMusic(Seq.with(a, b), vanilla, a);
        assertSame(b, picked, "must avoid repeating the currently assigned track when alternatives exist");

        picked = MusicFeature.pickSlotMusic(Seq.with(a, b), vanilla, null);
        assertTrue(picked == a || picked == b, "random pick must come from the playable set");
    }
}
