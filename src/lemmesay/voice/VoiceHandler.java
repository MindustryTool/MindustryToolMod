package lemmesay.voice;

import arc.Events;
import arc.util.Log;
import lemmesay.shared.packet.MicPacket;
import lemmesay.shared.packet.VoiceRequestPacket;
import lemmesay.shared.packet.VoiceResponsePacket;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Net;

public class VoiceHandler {

    private static VoiceHandler instance;
    private VoiceManager voiceManager;

    public static VoiceHandler getInstance() {
        if (instance == null) {
            instance = new VoiceHandler();
        }
        return instance;
    }

    public void init() {
        registerPackets();

        // Initialize VoiceManager
        voiceManager = new VoiceManager();
        Thread voiceThread = new Thread(voiceManager, "VoiceChatThread");
        voiceThread.setDaemon(true);
        voiceThread.start();

        // Register event listeners
        Events.on(EventType.ClientLoadEvent.class, e -> {
            Log.info("VoiceHandler initialized.");
        });

        // Handle receiving audio
        Vars.net.handleClient(MicPacket.class, packet -> {
            VoiceProcessing.getInstance().handleMicPacket(packet);
        });
    }

    private void registerPackets() {
        Net.registerPacket(VoiceRequestPacket::new);
        Net.registerPacket(VoiceResponsePacket::new);
        Net.registerPacket(MicPacket::new);
        Log.info("Voice chat packets registered.");
    }
}
