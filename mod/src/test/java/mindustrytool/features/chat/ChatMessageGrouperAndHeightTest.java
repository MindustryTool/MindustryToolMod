package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.ChatUser;
import mindustrytool.models.response.UserData;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.ui.Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import arc.mock.MockGL20;

class ChatMessageGrouperAndHeightTest {

    private static Font testFont;

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Core.gl == null) {
            Core.gl = new MockGL20();
            Core.gl20 = (MockGL20) Core.gl;
        }
        if (Core.scene == null) {
            Core.scene = new Scene();
        }

        if (testFont == null) {
            Font.FontData fontData = new Font.FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }

                @Override
                public Font.Glyph getGlyph(char ch) {
                    Font.Glyph g = super.getGlyph(ch);
                    if (g == null) {
                        g = new Font.Glyph();
                        g.id = ch;
                        g.width = 8;
                        g.height = 12;
                        g.xadvance = 8;
                        setGlyph(ch, g);
                    }
                    return g;
                }
            };
            fontData.lineHeight = 18f;
            fontData.capHeight = 14f;
            fontData.ascent = 14f;
            fontData.descent = -4f;
            fontData.down = -18f;
            testFont = new Font(fontData, new TextureRegion(), false);
        }
        Fonts.def = testFont;

        Label.LabelStyle labelStyle = new Label.LabelStyle(testFont, Color.white);
        Core.scene.addStyle(Label.LabelStyle.class, labelStyle);

        ChatMessageParser.clearCache();
        ChatMessageHeightCalculator.clearCache();
    }

    private static ChatMessage raw(String id, String author, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setCreatedBy(author);
        message.setCreatedAt("2026-09-12T10:00:00Z");
        message.setContent(content);
        return message;
    }

    private static List<String> idsOf(MessageGroup group) {
        List<String> ids = new ArrayList<>();
        for (var message : group.getMessages()) {
            ids.add(message.getId());
        }
        return ids;
    }

    @Test
    void testGrouperEmpty() {
        assertTrue(ChatMessageGrouper.group(Collections.emptyList()).isEmpty());
        assertTrue(ChatMessageGrouper.groupRaw(Collections.emptyList()).isEmpty());
    }

    @Test
    void testGrouperConsecutiveAuthor() {
        ChatMessage m1 = raw("1", "userA", "Hi 1");
        ChatMessage m2 = raw("2", "userA", "Hi 2");
        ChatMessage m3 = raw("3", "userB", "Hello from B");

        List<MessageGroup> grouped = ChatMessageGrouper.groupRaw(Arrays.asList(m1, m2, m3));
        assertEquals(2, grouped.size());

        // First group: consecutive messages from userA in order
        assertEquals("userA", grouped.get(0).getAuthorId());
        assertEquals(Arrays.asList("1", "2"), idsOf(grouped.get(0)));
        assertEquals("1_2_2", grouped.get(0).getKey());

        // Message from userB starts a new group
        assertEquals("userB", grouped.get(1).getAuthorId());
        assertEquals(Collections.singletonList("3"), idsOf(grouped.get(1)));
        assertEquals("3_1_3", grouped.get(1).getKey());
    }

    @Test
    void testGroupKeyChangesWhenMessageAppended() {
        List<MessageGroup> before = ChatMessageGrouper.groupRaw(Arrays.asList(
                raw("1", "alice", "Hello"),
                raw("2", "alice", "Again")));
        List<MessageGroup> after = ChatMessageGrouper.groupRaw(Arrays.asList(
                raw("1", "alice", "Hello"),
                raw("2", "alice", "Again"),
                raw("3", "alice", "Third")));

        assertEquals(1, before.size());
        assertEquals(1, after.size());
        assertEquals("1_2_2", before.get(0).getKey());
        assertEquals("1_3_3", after.get(0).getKey());
        assertNotEquals(before.get(0).getKey(), after.get(0).getKey());
    }

    @Test
    void testGroupCarriesAuthorAndFirstMessageTimestamp() {
        List<MessageGroup> grouped = ChatMessageGrouper.groupRaw(Arrays.asList(
                raw("1", "alice", "Hello"),
                raw("2", "alice", "Again")));

        MessageGroup group = grouped.get(0);
        assertEquals("alice", group.getAuthorId());
        assertEquals("2026-09-12T10:00:00Z", group.getCreatedAt());
        assertEquals(2, group.getMessageCount());
    }

    @Test
    void testGroupsWithSameContentAreEqual() {
        MessageGroup first = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Hello"))).get(0);
        MessageGroup second = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Hello"))).get(0);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void testHeightSingleLineGroupBoundByAvatar() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Hi"))).get(0);

        float height = ChatMessageHeightCalculator.calculateHeight(group, 400f);

        assertTrue(height >= ChatMessageHeightCalculator.AVATAR_SIZE + 8f,
                "Single-line group must be at least avatar height plus padding, was " + height);
    }

    @Test
    void testHeightGroupAccumulatesHeaderBodiesAndGaps() {
        MessageGroup group = ChatMessageGrouper.groupRaw(Arrays.asList(
                raw("1", "alice", "First"),
                raw("2", "alice", "Second"))).get(0);

        float textWidth = Math.max(100f, 400f - ChatMessageHeightCalculator.HORIZONTAL_PADDINGS);
        float first = ChatMessageHeightCalculator.measureTextHeight("First", textWidth, ChatMessageHeightCalculator.FONT_SCALE)
                + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING;
        float second = ChatMessageHeightCalculator.measureTextHeight("Second", textWidth, ChatMessageHeightCalculator.FONT_SCALE)
                + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING;
        float expected = Math.max(ChatMessageHeightCalculator.AVATAR_SIZE,
                ChatMessageHeightCalculator.HEADER_HEIGHT + ChatMessageHeightCalculator.HEADER_GAP
                        + first + second + ChatMessageHeightCalculator.MESSAGE_GAP) + 8f;

        assertEquals(expected, ChatMessageHeightCalculator.calculateHeight(group, 400f), 0.001f);
    }

    @Test
    void testHeightMultiMessageGroupTallerThanSingle() {
        MessageGroup single = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "First"))).get(0);
        MessageGroup multi = ChatMessageGrouper.groupRaw(Arrays.asList(
                raw("1", "alice", "First"),
                raw("2", "alice", "Second"))).get(0);

        assertTrue(ChatMessageHeightCalculator.calculateHeight(multi, 400f)
                > ChatMessageHeightCalculator.calculateHeight(single, 400f));
    }

    @Test
    void testHeightReplyPreviewIncreasesGroupHeight() {
        MessageGroup plain = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Hello"))).get(0);

        ChatMessage reply = raw("2", "alice", "Hello");
        reply.setReplyTo("other-id");
        ChatMessageParser.clearCache();
        MessageGroup withReply = ChatMessageGrouper.groupRaw(
                Collections.singletonList(reply)).get(0);

        assertTrue(ChatMessageHeightCalculator.calculateHeight(withReply, 400f)
                > ChatMessageHeightCalculator.calculateHeight(plain, 400f));
    }

    private float measureRealGroupHeight(MessageGroup group, float containerWidth) {
        return measureRealGroupHeight(group, containerWidth, new ChatStore());
    }

    private float measureRealGroupHeight(MessageGroup group, float containerWidth, ChatStore store) {
        ChatMessageListView.MessageGroupView view = new ChatMessageListView.MessageGroupView(group, store, null);
        Element element = view.element();

        Table root = new Table();
        root.setSize(containerWidth, 2000f);
        root.top().left();
        root.add(element).width(containerWidth).top().left();
        root.validate();

        return element.getPrefHeight();
    }

    @Test
    void testRealComponentHeightSingleLine() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Short text"))).get(0);
        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightMultiLine() {
        String longText = "This is a long message that should wrap to multiple lines when rendered in a limited width container.";
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("multi-1", "alice", longText))).get(0);
        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 250f);
        float realHeight = measureRealGroupHeight(group, 250f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightEmptyText() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("empty-1", "alice", ""))).get(0);
        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightWhitespaceOnly() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("ws-1", "alice", "   "))).get(0);
        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightMentioned() {
        ChatMessage rawMsg = raw("mention-1", "bob", "Hello someone!");
        ParsedChatMessage.TextMessage textMsg = new ParsedChatMessage.TextMessage(rawMsg, "Hello someone!", true);
        MessageGroup group = ChatMessageGrouper.group(Collections.singletonList(textMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightWithReply() {
        ChatMessage rawMsg = raw("reply-1", "bob", "Reply message body");
        rawMsg.setReplyTo("original-msg-id");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(rawMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightWithReplyMultiLine() {
        String longText = "Reply text that is long enough to wrap onto multiple separate lines in the chat message bubble.";
        ChatMessage rawMsg = raw("reply-multi", "bob", longText);
        rawMsg.setReplyTo("prev-id");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(rawMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 250f);
        float realHeight = measureRealGroupHeight(group, 250f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightMultiMessageGroup() {
        List<ChatMessage> list = Arrays.asList(
                raw("m-1", "alice", "First message"),
                raw("m-2", "alice", "Second consecutive message"),
                raw("m-3", "alice", "Third consecutive message")
        );
        MessageGroup group = ChatMessageGrouper.groupRaw(list).get(0);
        assertEquals(3, group.getMessageCount());

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightNarrowVsWide() {
        String wrappedText = "The quick brown fox jumps over the lazy dog repeatedly to test narrow and wide wrapping.";
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(raw("wrap-nw", "alice", wrappedText))).get(0);

        float calcNarrow = ChatMessageHeightCalculator.calculateHeight(group, 180f);
        float realNarrow = measureRealGroupHeight(group, 180f);
        assertEquals(calcNarrow, realNarrow, 0.001f);

        float calcWide = ChatMessageHeightCalculator.calculateHeight(group, 500f);
        float realWide = measureRealGroupHeight(group, 500f);
        assertEquals(calcWide, realWide, 0.001f);

        assertTrue(realNarrow > realWide, "Narrow container should produce taller component due to wrapping");
    }

    @Test
    void testRealComponentHeightReplySnippetTruncated() {
        ChatStore store = new ChatStore();
        store.selectChannel("ch1");
        ChatMessage target = raw("target-long", "charlie",
                "This is a very long message that definitely exceeds forty characters in total length for reply testing.");
        store.messages().replace("ch1", Collections.singletonList(target));

        ChatMessage replyMsg = raw("reply-long", "bob", "Responding to long message");
        replyMsg.setReplyTo("target-long");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(replyMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightReplySnippetShort() {
        ChatStore store = new ChatStore();
        store.selectChannel("ch1");
        ChatMessage target = raw("target-short", "charlie", "Short message");
        store.messages().replace("ch1", Collections.singletonList(target));

        ChatMessage replyMsg = raw("reply-short", "bob", "Responding to short");
        replyMsg.setReplyTo("target-short");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(replyMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightReplyTargetContentNull() {
        ChatStore store = new ChatStore();
        store.selectChannel("ch1");
        ChatMessage target = raw("target-null", "charlie", null);
        store.messages().replace("ch1", Collections.singletonList(target));

        ChatMessage replyMsg = raw("reply-target-null", "bob", "Responding to null content");
        replyMsg.setReplyTo("target-null");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(replyMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightReplyTargetNotFound() {
        ChatStore store = new ChatStore();
        store.selectChannel("ch1");

        ChatMessage replyMsg = raw("reply-target-not-found", "bob", "Responding to non-existent");
        replyMsg.setReplyTo("missing-id");
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(replyMsg)).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightWithUserDataAndRoles() {
        ChatStore store = new ChatStore();
        UserData user = new UserData();
        user.setId("alice");
        user.setName("Alice In Wonderland");
        user.setImageUrl("https://example.com/avatar.png");
        ChatUser.SimpleRole role = new ChatUser.SimpleRole();
        role.setId("admin");
        role.setColor("#00FF00");
        role.setLevel(10);
        user.setRoles(Collections.singletonList(role));
        store.users().putAll(Collections.singletonList(user));

        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Custom user data message"))).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightWithInvalidRoleColorFallback() {
        ChatStore store = new ChatStore();
        UserData user = new UserData();
        user.setId("alice");
        user.setName("Alice");
        ChatUser.SimpleRole role = new ChatUser.SimpleRole();
        role.setId("bad-color");
        role.setColor("not-a-valid-hex");
        role.setLevel(5);
        user.setRoles(Collections.singletonList(role));
        store.users().putAll(Collections.singletonList(user));

        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Invalid role color message"))).get(0);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f, store);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightNullAuthorId() {
        MessageGroup group = new MessageGroup(null, "2026-09-12T10:00:00Z",
                Collections.singletonList(new ParsedChatMessage.TextMessage(raw("1", null, "Null author"), "Null author", false)));

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testRealComponentHeightEmptyAndNullCreatedAt() {
        MessageGroup groupNullDate = new MessageGroup("alice", null,
                Collections.singletonList(new ParsedChatMessage.TextMessage(raw("1", "alice", "No date"), "No date", false)));
        float calcNull = ChatMessageHeightCalculator.calculateHeight(groupNullDate, 400f);
        float realNull = measureRealGroupHeight(groupNullDate, 400f);
        assertEquals(calcNull, realNull, 0.001f);

        MessageGroup groupEmptyDate = new MessageGroup("alice", "",
                Collections.singletonList(new ParsedChatMessage.TextMessage(raw("2", "alice", "Empty date"), "Empty date", false)));
        float calcEmpty = ChatMessageHeightCalculator.calculateHeight(groupEmptyDate, 400f);
        float realEmpty = measureRealGroupHeight(groupEmptyDate, 400f);
        assertEquals(calcEmpty, realEmpty, 0.001f);
    }

    @Test
    void testRealComponentHeightMixedMessageGroup() {
        ChatMessage m1 = raw("mix-1", "alice", "First message text");
        ChatMessage m2Raw = raw("mix-2", "alice", "Mentioned text here");
        ParsedChatMessage.TextMessage m2 = new ParsedChatMessage.TextMessage(m2Raw, "Mentioned text here", true);
        ChatMessage m3 = raw("mix-3", "alice", "Third message with reply");
        m3.setReplyTo("mix-1");

        List<ParsedChatMessage> messages = Arrays.asList(
                new ParsedChatMessage.TextMessage(m1, "First message text", false),
                m2,
                new ParsedChatMessage.TextMessage(m3, "Third message with reply", false)
        );
        MessageGroup group = new MessageGroup("alice", "2026-09-12T10:00:00Z", messages);

        float calculated = ChatMessageHeightCalculator.calculateHeight(group, 400f);
        float realHeight = measureRealGroupHeight(group, 400f);
        assertEquals(calculated, realHeight, 0.001f);
    }

    @Test
    void testHeightCalculatorNullAndEmptyGroup() {
        assertEquals(24f, ChatMessageHeightCalculator.calculateHeight(null, 400f));

        MessageGroup emptyGroup = new MessageGroup("alice", "2026-09-12T10:00:00Z", Collections.emptyList());
        assertEquals(24f, ChatMessageHeightCalculator.calculateHeight(emptyGroup, 400f));
    }

    @Test
    void testHeightCalculatorContainerWidthClamped() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("1", "alice", "Short text"))).get(0);

        float at100 = ChatMessageHeightCalculator.calculateHeight(group, 100f);
        float at50 = ChatMessageHeightCalculator.calculateHeight(group, 50f);
        float atZero = ChatMessageHeightCalculator.calculateHeight(group, 0f);
        float atNegative = ChatMessageHeightCalculator.calculateHeight(group, -50f);

        assertEquals(at100, at50, 0.001f);
        assertEquals(at100, atZero, 0.001f);
        assertEquals(at100, atNegative, 0.001f);
    }

    @Test
    void testHeightCalculatorCachingAndClearCache() {
        MessageGroup group = ChatMessageGrouper.groupRaw(
                Collections.singletonList(raw("cache-msg", "bob", "Cached test text"))).get(0);

        float first = ChatMessageHeightCalculator.calculateHeight(group, 350f);
        float second = ChatMessageHeightCalculator.calculateHeight(group, 350f);
        assertEquals(first, second, 0.0001f);

        ChatMessageHeightCalculator.clearCache();
        float third = ChatMessageHeightCalculator.calculateHeight(group, 350f);
        assertEquals(first, third, 0.0001f);
    }

    @Test
    void testHeightCalculatorEmptyReplyTo() {
        ChatMessage msgNull = raw("reply-null", "alice", "Message content");
        msgNull.setReplyTo(null);
        MessageGroup groupNull = ChatMessageGrouper.groupRaw(Collections.singletonList(msgNull)).get(0);

        ChatMessage msgEmpty = raw("reply-empty", "alice", "Message content");
        msgEmpty.setReplyTo("");
        MessageGroup groupEmpty = ChatMessageGrouper.groupRaw(Collections.singletonList(msgEmpty)).get(0);

        float heightNull = ChatMessageHeightCalculator.calculateHeight(groupNull, 400f);
        float heightEmpty = ChatMessageHeightCalculator.calculateHeight(groupEmpty, 400f);
        assertEquals(heightNull, heightEmpty, 0.001f);

        float realHeightEmpty = measureRealGroupHeight(groupEmpty, 400f);
        assertEquals(heightEmpty, realHeightEmpty, 0.001f);
    }

    @Test
    void testSchematicCardHeightMatchesRestructuredLayout() {
        // Title row 24 + gap 4 + preview 140 + gap 4 + actions row 28 + card padding 12 = 212.
        assertEquals(212f, ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT, 0.001f);
    }

    @Test
    void testHeightCalculatorSchematicMessageBranches() {        // Both prefix and suffix present
        ParsedChatMessage.SchematicMessage schemBoth = new ParsedChatMessage.SchematicMessage(
                raw("schem-both", "builder", "schematic content"), null, "Prefix description", "Suffix notes");
        MessageGroup groupBoth = new MessageGroup("builder", "2026-09-12T10:00:00Z", Collections.singletonList(schemBoth));
        float hBoth = ChatMessageHeightCalculator.calculateHeight(groupBoth, 400f);
        assertTrue(hBoth > ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT);

        // Prefix only
        ParsedChatMessage.SchematicMessage schemPrefix = new ParsedChatMessage.SchematicMessage(
                raw("schem-prefix", "builder", "schematic content"), null, "Prefix description", null);
        MessageGroup groupPrefix = new MessageGroup("builder", "2026-09-12T10:00:00Z", Collections.singletonList(schemPrefix));
        float hPrefix = ChatMessageHeightCalculator.calculateHeight(groupPrefix, 400f);
        assertTrue(hBoth > hPrefix);

        // Suffix only
        ParsedChatMessage.SchematicMessage schemSuffix = new ParsedChatMessage.SchematicMessage(
                raw("schem-suffix", "builder", "schematic content"), null, null, "Suffix notes");
        MessageGroup groupSuffix = new MessageGroup("builder", "2026-09-12T10:00:00Z", Collections.singletonList(schemSuffix));
        float hSuffix = ChatMessageHeightCalculator.calculateHeight(groupSuffix, 400f);
        assertTrue(hBoth > hSuffix);

        // Neither prefix nor suffix (null and empty strings)
        ParsedChatMessage.SchematicMessage schemNone = new ParsedChatMessage.SchematicMessage(
                raw("schem-none", "builder", "schematic content"), null, null, null);
        MessageGroup groupNone = new MessageGroup("builder", "2026-09-12T10:00:00Z", Collections.singletonList(schemNone));
        float hNone = ChatMessageHeightCalculator.calculateHeight(groupNone, 400f);

        ParsedChatMessage.SchematicMessage schemEmptyStrings = new ParsedChatMessage.SchematicMessage(
                raw("schem-empty", "builder", "schematic content"), null, "", "");
        MessageGroup groupEmpty = new MessageGroup("builder", "2026-09-12T10:00:00Z", Collections.singletonList(schemEmptyStrings));
        float hEmpty = ChatMessageHeightCalculator.calculateHeight(groupEmpty, 400f);
        assertEquals(hNone, hEmpty, 0.001f);
    }

    @Test
    void testHeightCalculatorOtherMessageTypes() {
        // Image message
        ParsedChatMessage.ImageMessage img = new ParsedChatMessage.ImageMessage(
                raw("img-msg", "user", "payload"), "https://example.com/test.png");
        MessageGroup groupImg = new MessageGroup("user", "2026-09-12T10:00:00Z", Collections.singletonList(img));
        float hImg = ChatMessageHeightCalculator.calculateHeight(groupImg, 400f);
        float expectedImg = Math.max(ChatMessageHeightCalculator.AVATAR_SIZE,
                ChatMessageHeightCalculator.HEADER_HEIGHT + ChatMessageHeightCalculator.HEADER_GAP
                        + ChatMessageHeightCalculator.IMAGE_CARD_HEIGHT + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING)
                + ChatMessageHeightCalculator.UNIT_1 * 2f;
        assertEquals(expectedImg, hImg, 0.001f);

        // Room invite message
        assertEquals(108f, ChatMessageHeightCalculator.INVITE_CARD_HEIGHT, 0.001f);
        ParsedChatMessage.RoomInviteMessage invite = new ParsedChatMessage.RoomInviteMessage(
                raw("invite-msg", "user", "payload"), "player-connect://127.0.0.1:6567");
        MessageGroup groupInvite = new MessageGroup("user", "2026-09-12T10:00:00Z", Collections.singletonList(invite));
        float hInvite = ChatMessageHeightCalculator.calculateHeight(groupInvite, 400f);
        float expectedInvite = Math.max(ChatMessageHeightCalculator.AVATAR_SIZE,
                ChatMessageHeightCalculator.HEADER_HEIGHT + ChatMessageHeightCalculator.HEADER_GAP
                        + ChatMessageHeightCalculator.INVITE_CARD_HEIGHT + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING)
                + ChatMessageHeightCalculator.UNIT_1 * 2f;
        assertEquals(expectedInvite, hInvite, 0.001f);

        // MindustryTool link message
        ParsedChatMessage.MindustryToolLinkMessage link = new ParsedChatMessage.MindustryToolLinkMessage(
                raw("link-msg", "user", "payload"), "https://mindustrytool.com/schematics/1", "schematics", "1");
        MessageGroup groupLink = new MessageGroup("user", "2026-09-12T10:00:00Z", Collections.singletonList(link));
        float hLink = ChatMessageHeightCalculator.calculateHeight(groupLink, 400f);
        float expectedLink = Math.max(ChatMessageHeightCalculator.AVATAR_SIZE,
                ChatMessageHeightCalculator.HEADER_HEIGHT + ChatMessageHeightCalculator.HEADER_GAP
                        + ChatMessageHeightCalculator.TOOL_LINK_CARD_HEIGHT + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING)
                + ChatMessageHeightCalculator.UNIT_1 * 2f;
        assertEquals(expectedLink, hLink, 0.001f);

        // Unknown fallback message type
        ParsedChatMessage unknown = new ParsedChatMessage(raw("unk-msg", "user", "payload")) {};
        MessageGroup groupUnknown = new MessageGroup("user", "2026-09-12T10:00:00Z", Collections.singletonList(unknown));
        float hUnknown = ChatMessageHeightCalculator.calculateHeight(groupUnknown, 400f);
        float expectedUnknown = Math.max(ChatMessageHeightCalculator.AVATAR_SIZE,
                ChatMessageHeightCalculator.HEADER_HEIGHT + ChatMessageHeightCalculator.HEADER_GAP
                        + 24f + ChatMessageHeightCalculator.MESSAGE_CARD_PADDING)
                + ChatMessageHeightCalculator.UNIT_1 * 2f;
        assertEquals(expectedUnknown, hUnknown, 0.001f);
    }

    @Test
    void testMeasureTextHeightBranchesWithFont() {
        float defaultHeight = (testFont.getCapHeight() - testFont.getDescent() * 2f) * 1.0f;

        assertEquals(defaultHeight, ChatMessageHeightCalculator.measureTextHeight(null, 400f, 1.0f), 0.001f);
        assertEquals(defaultHeight, ChatMessageHeightCalculator.measureTextHeight("", 400f, 1.0f), 0.001f);
        assertEquals(defaultHeight, ChatMessageHeightCalculator.measureTextHeight("   ", 400f, 1.0f), 0.001f);

        float scaledHeight = (testFont.getCapHeight() - testFont.getDescent() * 2f) * 1.5f;
        assertEquals(scaledHeight, ChatMessageHeightCalculator.measureTextHeight(null, 400f, 1.5f), 0.001f);
    }

    @Test
    void testMeasureTextHeightFallbackWithoutFont() {
        Font savedFont = Fonts.def;
        try {
            Fonts.def = null;

            assertEquals(18f, ChatMessageHeightCalculator.measureTextHeight(null, 400f, 1.0f), 0.001f);
            assertEquals(18f, ChatMessageHeightCalculator.measureTextHeight("", 400f, 1.0f), 0.001f);
            assertEquals(18f, ChatMessageHeightCalculator.measureTextHeight("   ", 400f, 1.0f), 0.001f);
            assertEquals(27f, ChatMessageHeightCalculator.measureTextHeight(null, 400f, 1.5f), 0.001f);

            float shortTextH = ChatMessageHeightCalculator.measureTextHeight("Short", 400f, 1.0f);
            assertTrue(shortTextH >= 18f);

            float longTextH = ChatMessageHeightCalculator.measureTextHeight(
                    "This is a long text fallback without font available in the test environment", 100f, 1.0f);
            assertTrue(longTextH > shortTextH);
        } finally {
            Fonts.def = savedFont;
        }
    }

    @Test
    void testHeightCalculatorTextWrapNarrowVsWide() {
        MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(raw("long-msg", "charlie",
                "This is a long message that should wrap to multiple lines when container width is small and fit fewer lines when wide."))).get(0);

        float wide = ChatMessageHeightCalculator.calculateHeight(group, 600f);
        float narrow = ChatMessageHeightCalculator.calculateHeight(group, 150f);

        assertTrue(narrow >= wide, "Narrow width should produce taller or equal height for wrapped text");
    }

    @Test
    void testHeightCalculatorMobileScaling() {
        ChatMessageHeightCalculator.clearCache();
        try {
            MessageGroup group = ChatMessageGrouper.groupRaw(Collections.singletonList(raw("scale-msg", "author", "Test message content"))).get(0);

            // Desktop scale (1.0f)
            Scl.setProduct(1.0f);
            float h1 = ChatMessageHeightCalculator.calculateHeight(group, 300f);

            // Mobile scale (2.0f)
            Scl.setProduct(2.0f);
            float h2 = ChatMessageHeightCalculator.calculateHeight(group, 300f);

            // With scale 2.0, height should be significantly larger (proportional to scale)
            assertTrue(h2 > h1 * 1.5f, "Height under Scl 2.0 (" + h2 + ") should scale up compared to Scl 1.0 (" + h1 + ")");

            // Empty group should also scale
            float emptyH1 = ChatMessageHeightCalculator.calculateHeight(null, 300f);
            assertEquals(48f, emptyH1, 0.001f); // 24f * 2.0 = 48f
        } finally {
            Scl.setProduct(1.0f);
            ChatMessageHeightCalculator.clearCache();
        }
    }
}
