package mindustrytool.features.wavepreview;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.Seq;
import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class WavePreviewStateTest extends ArcTestEnv {

    @Test
    void testToggleCollapsedIgnoredWhenSingleOrEmptyWave() {
        WavePreviewState state = new WavePreviewState();
        assertFalse(state.isCollapsed());

        state.toggleCollapsed();
        assertFalse(state.isCollapsed());

        Seq<WaveSectionData> singleWave = new Seq<>();
        singleWave.add(new WaveSectionData(1, new Seq<>(), new Seq<>(), new Seq<>(), 100f));
        state.setWaves(singleWave);

        state.toggleCollapsed();
        assertFalse(state.isCollapsed());
    }

    @Test
    void testToggleCollapsedWithMultipleWaves() {
        WavePreviewState state = new WavePreviewState();
        Seq<WaveSectionData> multiWaves = new Seq<>();
        multiWaves.add(new WaveSectionData(1, new Seq<>(), new Seq<>(), new Seq<>(), 100f));
        multiWaves.add(new WaveSectionData(2, new Seq<>(), new Seq<>(), new Seq<>(), 200f));
        state.setWaves(multiWaves);

        assertFalse(state.isCollapsed());

        state.toggleCollapsed();
        assertTrue(state.isCollapsed());

        state.toggleCollapsed();
        assertFalse(state.isCollapsed());
    }

    @Test
    void testClearResetsCollapsed() {
        WavePreviewState state = new WavePreviewState();
        Seq<WaveSectionData> multiWaves = new Seq<>();
        multiWaves.add(new WaveSectionData(1, new Seq<>(), new Seq<>(), new Seq<>(), 100f));
        multiWaves.add(new WaveSectionData(2, new Seq<>(), new Seq<>(), new Seq<>(), 200f));
        state.setWaves(multiWaves);

        state.toggleCollapsed();
        assertTrue(state.isCollapsed());

        state.clear();
        assertFalse(state.isCollapsed());
        assertTrue(state.waves().get().isEmpty());
    }

    @Test
    void testHpFormatting() {
        assertEquals("0", WavePreviewPanelView.formatHp(0f));
        assertEquals("520", WavePreviewPanelView.formatHp(520f));
        assertEquals("1.2k", WavePreviewPanelView.formatHp(1200f));
        assertEquals("45.2k", WavePreviewPanelView.formatHp(45200f));
        assertEquals("100k", WavePreviewPanelView.formatHp(100000f));
        assertEquals("1.5M", WavePreviewPanelView.formatHp(1500000f));
    }
}
