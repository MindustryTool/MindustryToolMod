package mindustrytool.features.chat.global.dto;

import arc.struct.Seq;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChatMessageReceive {
    public final Seq<ChatMessage> messages;
}
