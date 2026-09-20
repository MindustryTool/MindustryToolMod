package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.models.response.ChatUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.QueryCache;
import solim.reactive.QueryKey;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class ChatChannelsAndMembersTest extends SolimEnv {

    @BeforeEach
    void setUp() {
        QueryCache.getInstance().clear();
    }

    @Test
    void testChannelsLoadingAndError() {
        Signal<String> activeId = Signal.of(null);
        ChatChannels channels = new ChatChannels(activeId);

        assertNotNull(channels.channelsQuery());

        ChannelDto dto = new ChannelDto();
        dto.setId("ch1");
        dto.setName("General");
        channels.replace(Collections.singletonList(dto));

        assertEquals("ch1", channels.activeId().get());
        assertEquals(1, channels.all().get().size());
        assertEquals("General", channels.active().get().getName());

        flushEffects();
        channels.channelsQuery().dispose();
    }

    @Test
    void testMembersQueryReactivity() {
        Signal<String> activeId = Signal.of(null);
        ChatMembers members = new ChatMembers(activeId);

        assertNotNull(members.query());
        assertEquals(QueryKey.of("chat", "members"), members.query().getKey());
        assertTrue(members.currentActive().isEmpty());

        ChatUser user = new ChatUser();
        user.setName("Player1");
        members.query().mutate(Collections.singletonList(user));

        assertEquals(1, members.active().get().size());
        assertEquals("Player1", members.currentActive().get(0).getName());
        assertFalse(members.isActiveLoading());
        assertNull(members.currentActiveError());

        flushEffects();
        members.query().dispose();
    }

    @Test
    void testChannelsQuery() {
        Signal<String> activeId = Signal.of(null);
        ChatChannels channels = new ChatChannels(activeId);

        assertNotNull(channels.channelsQuery());
        assertEquals(QueryKey.of("chat", "channels"), channels.channelsQuery().getKey());

        channels.channelsQuery().dispose();
    }
}
