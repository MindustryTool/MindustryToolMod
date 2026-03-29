package mindustrytool.features.chat.global.events;

import arc.struct.Seq;
import lombok.RequiredArgsConstructor;
import mindustrytool.features.chat.global.dto.ChatMessage;

@RequiredArgsConstructor
public class ChatMessageReceive {
    public final Seq<ChatMessage> messages;
}
