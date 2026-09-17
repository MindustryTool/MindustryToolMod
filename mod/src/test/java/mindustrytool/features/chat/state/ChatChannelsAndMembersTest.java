package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.models.response.ChatUser;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class ChatChannelsAndMembersTest {

    @Test
    void testChannelsLoadingAndError() {
        Signal<String> activeId = Signal.of(null);
        ChatChannels channels = new ChatChannels(activeId);

        assertFalse(channels.isLoading());
        assertNull(channels.currentError());

        channels.setLoading(true);
        assertTrue(channels.isLoading());
        assertTrue(channels.loading().get());

        channels.setLoading(false);
        channels.setError("Failed to reach server");
        assertEquals("Failed to reach server", channels.currentError());
        assertEquals("Failed to reach server", channels.error().get());

        ChannelDto dto = new ChannelDto();
        dto.setId("ch1");
        dto.setName("General");
        channels.replace(Collections.singletonList(dto));

        assertEquals("ch1", channels.activeId().get());
        assertEquals(1, channels.all().get().size());
    }

    @Test
    void testMembersLoadingAndError() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMembers members = new ChatMembers(activeId);

        assertFalse(members.isActiveLoading());
        assertNull(members.currentActiveError());

        members.setLoading("ch1", true);
        assertTrue(members.isActiveLoading());
        assertTrue(members.activeLoading().get());

        members.setLoading("ch1", false);
        members.setError("ch1", "HTTP 500");
        assertFalse(members.isActiveLoading());
        assertEquals("HTTP 500", members.currentActiveError());

        ChatUser user = new ChatUser();
        user.setName("Player1");
        members.replace("ch1", Collections.singletonList(user));

        assertNull(members.currentActiveError());
        assertEquals(1, members.active().get().size());
    }
}
