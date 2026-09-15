package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import mindustrytool.models.response.ChannelDto;
import org.junit.jupiter.api.Test;

class ChatChannelsTest {

    @Test
    void testInitialState() {
        ChatChannels channels = new ChatChannels();
        assertTrue(channels.all().get().isEmpty());
        assertNull(channels.currentActiveId());
        assertNull(channels.currentActive());
    }

    @Test
    void testReplaceAutoSelectsFirstChannel() {
        ChatChannels channels = new ChatChannels();
        ChannelDto c1 = new ChannelDto();
        c1.setId("ch1");
        c1.setName("General");
        ChannelDto c2 = new ChannelDto();
        c2.setId("ch2");
        c2.setName("Offtopic");

        channels.replace(Arrays.asList(c1, c2));

        assertEquals(2, channels.all().get().size());
        assertEquals("ch1", channels.currentActiveId());
        assertNotNull(channels.currentActive());
        assertEquals("General", channels.currentActive().getName());
    }

    @Test
    void testReplacePreservesActiveChannelIfPresent() {
        ChatChannels channels = new ChatChannels();
        ChannelDto c1 = new ChannelDto();
        c1.setId("ch1");
        ChannelDto c2 = new ChannelDto();
        c2.setId("ch2");
        channels.replace(Arrays.asList(c1, c2));
        channels.select("ch2");

        ChannelDto c1Updated = new ChannelDto();
        c1Updated.setId("ch1");
        ChannelDto c2Updated = new ChannelDto();
        c2Updated.setId("ch2");
        c2Updated.setName("Offtopic Renamed");

        channels.replace(Arrays.asList(c1Updated, c2Updated));

        assertEquals("ch2", channels.currentActiveId());
        assertEquals("Offtopic Renamed", channels.currentActive().getName());
    }

    @Test
    void testReplaceResetsIfActiveChannelRemoved() {
        ChatChannels channels = new ChatChannels();
        ChannelDto c1 = new ChannelDto();
        c1.setId("ch1");
        ChannelDto c2 = new ChannelDto();
        c2.setId("ch2");
        channels.replace(Arrays.asList(c1, c2));
        channels.select("ch2");

        ChannelDto c3 = new ChannelDto();
        c3.setId("ch3");
        channels.replace(Collections.singletonList(c3));

        assertEquals("ch3", channels.currentActiveId());
    }

    @Test
    void testSelectChannel() {
        ChatChannels channels = new ChatChannels();
        ChannelDto c1 = new ChannelDto();
        c1.setId("ch1");
        ChannelDto c2 = new ChannelDto();
        c2.setId("ch2");
        channels.replace(Arrays.asList(c1, c2));

        channels.select("ch2");
        assertEquals("ch2", channels.currentActiveId());
        assertEquals("ch2", channels.active().get().getId());

        channels.select(null);
        assertNull(channels.currentActiveId());
        assertNull(channels.currentActive());
    }
}
