package mindustrytool.features.social.voice;

import arc.util.Log;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustrytool.features.social.voice.protocol.shared.VoiceProtocol;
import mindustrytool.features.social.voice.protocol.shared.packet.MicPacket;
import mindustrytool.features.social.voice.protocol.shared.packet.VoiceRequestPacket;
import mindustrytool.features.social.voice.protocol.shared.packet.VoiceResponsePacket;

/**
 * Service for Voice Network operations.
 * Handles packet registration, sending, and receiving.
 * <p>
 * This component abstracts the network layer for Voice Chat:
 * <ul>
 * <li>Registers custom network packets ({@code MicPacket},
 * {@code VoiceRequestPacket}, {@code VoiceResponsePacket}).</li>
 * <li>Sets up client and server packet handlers.</li>
 * <li>Exposes methods to send handshake and audio data packets.</li>
 * </ul>
 */
public class VoiceNetworkService {
    private static final String TAG = "[VoiceNet]";
    private final VoiceChatManager manager;
    private boolean packetsRegistered = false;

    public VoiceNetworkService(VoiceChatManager manager) {
        this.manager = manager;
    }

    public void registerPackets() {
        if (packetsRegistered)
            return;
        try {
            Net.registerPacket(MicPacket::new);
            Net.registerPacket(VoiceRequestPacket::new);
            Net.registerPacket(VoiceResponsePacket::new);
            packetsRegistered = true;

            // Client Handler: Voice Request (Handshake Initiated by Server)
            Vars.net.handleClient(VoiceRequestPacket.class, packet -> {
                manager.onServerHandshakeRequest();
            });

            // Client Handler: Audio Data
            Vars.net.handleClient(MicPacket.class, packet -> {
                manager.processIncomingVoice(packet);
            });

            // Server Handler: Handshake Response
            Vars.net.handleServer(VoiceResponsePacket.class, (con, packet) -> {
                manager.onClientHandshakeResponse(con, packet);
            });

            // Server Handler: Audio Data
            Vars.net.handleServer(MicPacket.class, (con, packet) -> {
                manager.onServerReceiveAudio(con, packet);
            });

        } catch (Exception e) {
            Log.warn("@ Packets registration error: @", TAG, e.getMessage());
        }
    }

    public void sendHandshakeResponse() {
        if (!Vars.net.client() || !Vars.net.active())
            return;
        try {
            VoiceResponsePacket packet = new VoiceResponsePacket();
            packet.responseCode = VoiceProtocol.RESPONSE_ACCEPTED;
            Vars.net.send(packet, true);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void sendAudioPacket(MicPacket packet, boolean reliable) {
        try {
            Vars.net.send(packet, reliable);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void forwardAudioPacket(NetConnection target, MicPacket packet) {
        if (target != null && target.isConnected()) {
            target.send(packet, false); // Audio is always unreliable
        }
    }

    public void sendRequest(NetConnection target) {
        VoiceRequestPacket request = new VoiceRequestPacket();
        request.protocolVersion = VoiceProtocol.PROTOCOL_VERSION;
        target.send(request, true);
    }
}
