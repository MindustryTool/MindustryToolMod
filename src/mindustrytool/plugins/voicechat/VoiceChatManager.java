package mindustrytool.plugins.voicechat;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Nullable;
import lemmesay.shared.LemmeSayConstants;
import lemmesay.shared.packet.MicPacket;
import lemmesay.shared.packet.VoiceRequestPacket;
import lemmesay.shared.packet.VoiceResponsePacket;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import arc.util.Time;

/**
 * VoiceChatManager - Core voice chat logic.
 * Handles packet registration, audio capture, and playback.
 * This class follows the LazyComponent pattern for on-demand initialization.
 */
public class VoiceChatManager {

    private static final String TAG = "[VoiceChat]";

    public enum VoiceMode {
        ALL, TEAM
    }

    public enum VoiceStatus {
        DISABLED, // Voice off
        WAITING_HANDSHAKE, // Waiting for server handshake
        READY, // Ready to transmit/receive
        MIC_ERROR, // Mic initialization failed
        SPEAKER_ERROR, // Speaker initialization failed
        CONNECTED // Actively connected and transmitting
    }

    private VoiceStatus status = VoiceStatus.DISABLED;

    private boolean initialized = false;
    private boolean enabled = true; // Speaker enabled by default
    private boolean muted = true; // Mic muted by default (user must unmute to speak)
    private boolean forceMock = false; // Force usage of mock microphone

    // Modes
    private VoiceMode speakerMode = VoiceMode.ALL;
    private VoiceMode micMode = VoiceMode.ALL;

    // Per-player settings
    private final ObjectMap<String, Boolean> playerMuted = new ObjectMap<>();
    private final ObjectMap<String, Float> playerVolume = new ObjectMap<>(); // 0.0 - 1.0
    private final ObjectMap<String, Long> lastSpeakingTime = new ObjectMap<>();
    private static final long SPEAKING_THRESHOLD_MS = 300;

    // Track clients that have been verified to have the mod installed
    private final ObjectSet<NetConnection> moddedClients = new ObjectSet<>();

    @Nullable
    private VoiceMicrophone microphone;
    @Nullable
    private VoiceSpeaker speaker;
    @Nullable
    private VoiceProcessor processor;
    @Nullable
    private AudioMixer mixer;
    @Nullable
    private Thread captureThread;

    public VoiceChatManager() {
        Log.info("@ Manager created", TAG);
    }

    /**
     * Initialize voice chat system.
     */
    public void init() {
        if (initialized)
            return;

        registerPackets();

        // Server-side Logic: Receive Handshake from Client
        Vars.net.handleServer(VoiceResponsePacket.class, (con, packet) -> {
            if (con.player != null) {
                // Handshake accepted (Legacy mode: No protocol check)
                Log.info("@ Client @ verified modded. Adding to voice recipients.", TAG, con.player.name);
                moddedClients.add(con);

                // Send Ack back to client
                VoiceRequestPacket ack = new VoiceRequestPacket();
                ack.protocolVersion = LemmeSayConstants.PROTOCOL_VERSION;
                con.send(ack, true); // Reliable TCP
            }
        });

        // Server-side Logic: Forward Audio
        Vars.net.handleServer(MicPacket.class, (con, packet) -> {
            if (con.player == null)
                return;

            // Only accept voice from verified modded clients
            if (!moddedClients.contains(con))
                return;

            // Enforce sender ID
            packet.playerid = con.player.id;

            // Forward to other verified clients
            for (NetConnection other : moddedClients) {
                if (other != con && other.isConnected()) {
                    other.send(packet, true); // Reliable (TCP) for delivery guarantee
                }
            }

            // If Host (PC), play audio locally
            if (!Vars.headless) {
                processIncomingVoice(packet);
            }
        });

        // Cleanup: Player Leave
        arc.Events.on(mindustry.game.EventType.PlayerLeave.class, e -> {
            if (Vars.net.server() && e.player.con != null) {
                moddedClients.remove(e.player.con);
            }
        });

        // Sync status on World Load
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            syncStatusForCurrentConnection();
        });

        // Cleanup on Menu return
        arc.Events.on(mindustry.game.EventType.StateChangeEvent.class, e -> {
            if (e.to == mindustry.core.GameState.State.menu) {
                stopCapture();
                if (speaker != null)
                    speaker.close();
                status = VoiceStatus.DISABLED;
                moddedClients.clear();
            }
        });

        initialized = true;

        // Check if already in game
        if (enabled && status == VoiceStatus.DISABLED && Vars.net.active()) {
            syncStatusForCurrentConnection();
        }
    }

    private void syncStatusForCurrentConnection() {
        if (!enabled || !Vars.net.active())
            return;

        // Always re-handshake on WorldLoad (fixes rejoin issues)
        if (Vars.net.client()) {
            status = VoiceStatus.WAITING_HANDSHAKE;
            sendHandshake();
            Log.info("@ Client: Handshake sent to server", TAG);
        } else if (Vars.net.server() && !Vars.headless) {
            status = VoiceStatus.READY;
            if (!muted) {
                startCapture();
                status = VoiceStatus.CONNECTED;
            }
            Log.info("@ Host: Voice ready", TAG);
        }
    }

    private void ensureAudioInitialized() {
        if (processor != null && speaker != null)
            return;
        try {
            if (processor == null)
                processor = new VoiceProcessor();
            if (speaker == null)
                speaker = new VoiceSpeaker();
            // Mixer disabled - causes timing issues. Direct playback is more stable.
        } catch (Throwable e) {
            Log.err("@ Failed to init audio: @", TAG, e.getMessage());
        }
    }

    private static boolean packetsRegistered = false;

    private void registerPackets() {
        if (packetsRegistered)
            return;
        try {
            Net.registerPacket(MicPacket::new);
            Net.registerPacket(VoiceRequestPacket::new);
            Net.registerPacket(VoiceResponsePacket::new);
            packetsRegistered = true;
        } catch (Exception e) {
            Log.warn("@ Packets registration error: @", TAG, e.getMessage());
        }

        // Client Logic: Receive Ack
        Vars.net.handleClient(VoiceRequestPacket.class, packet -> {
            // Protocol check removed for compatibility
            status = VoiceStatus.READY;
            if (enabled && !muted)
                startCapture();
        });

        // Client Logic: Receive Audio
        Vars.net.handleClient(MicPacket.class, packet -> {
            processIncomingVoice(packet);
        });
    }

    private void processIncomingVoice(MicPacket packet) {
        if (!enabled)
            return;
        if (Vars.player != null && packet.playerid == Vars.player.id)
            return;

        mindustry.gen.Player sender = mindustry.gen.Groups.player.find(p -> p.id == packet.playerid);
        if (sender != null) {
            lastSpeakingTime.put(sender.uuid(), arc.util.Time.millis());
            if (playerMuted.get(sender.uuid(), false))
                return;
            if (speakerMode == VoiceMode.TEAM && Vars.player.team() != sender.team())
                return;
        }

        ensureAudioInitialized();
        if (speaker != null && processor != null) {
            try {
                byte[] data = packet.audioData;
                // Check if this is a batched packet (has length prefix)
                if (data.length > 4) {
                    int offset = 0;
                    while (offset < data.length - 2) {
                        int frameLen = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                        if (frameLen <= 0 || offset + 2 + frameLen > data.length)
                            break;
                        byte[] frameData = new byte[frameLen];
                        System.arraycopy(data, offset + 2, frameData, 0, frameLen);
                        short[] decoded = processor.decode(frameData);
                        if (sender != null) {
                            float vol = playerVolume.get(sender.uuid(), 1f);
                            if (vol != 1f) {
                                for (int i = 0; i < decoded.length; i++)
                                    decoded[i] = (short) (decoded[i] * vol);
                            }
                        }
                        // Direct playback - mixer disabled for stability
                        speaker.play(decoded);
                        offset += 2 + frameLen;
                    }
                }
            } catch (Throwable e) {
                // prevent spam Log.err
            }
        }
    }

    public void sendHandshake() {
        if (!Vars.net.client() || !Vars.net.active())
            return;
        VoiceResponsePacket packet = new VoiceResponsePacket();
        packet.responseCode = LemmeSayConstants.RESPONSE_ACCEPTED;
        Vars.net.send(packet, true);
    }

    public void startCapture() {
        if (captureThread != null && captureThread.isAlive())
            return;
        ensureAudioInitialized();

        if (microphone == null) {
            try {
                microphone = new VoiceMicrophone();
            } catch (Exception e) {
                Log.warn(e.getMessage());
            }
        }

        if (microphone == null && !forceMock) {
            status = VoiceStatus.MIC_ERROR;
            return;
        }

        captureThread = new Thread(() -> {
            try {
                if (!forceMock) {
                    microphone.open();
                    microphone.start();
                }

                long lastSpeakingTimeVAD = 0;
                boolean wasSpeaking = false;
                long mockTime = 0;
                byte[] pendingFrame = null; // For true frame batching

                while (enabled && !muted && Vars.net.active()) {
                    short[] rawAudio;
                    if (!forceMock) {
                        if (microphone.available() >= VoiceConstants.BUFFER_SIZE) {
                            rawAudio = microphone.read();
                        } else {
                            try {
                                Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS / 2);
                            } catch (Exception e) {
                            }
                            continue;
                        }
                    } else {
                        // Mock
                        rawAudio = new short[VoiceConstants.BUFFER_SIZE];
                        boolean beep = (System.currentTimeMillis() / 1000) % 2 == 0;
                        if (beep) {
                            for (int i = 0; i < rawAudio.length; i++) {
                                rawAudio[i] = (short) (Math.sin((mockTime + i) * 2.0 * Math.PI * 440.0 / 48000.0)
                                        * 10000);
                            }
                        }
                        mockTime += rawAudio.length;
                        Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS);
                    }

                    if (processor != null) {
                        // VAD Logic
                        if (!forceMock) {
                            double rms = processor.calculateRMS(rawAudio);
                            if (rms > VoiceProcessor.VAD_THRESHOLD) { // Threshold 50.0
                                lastSpeakingTimeVAD = System.currentTimeMillis();
                                wasSpeaking = true;
                            } else {
                                if (System.currentTimeMillis() - lastSpeakingTimeVAD > 400)
                                    wasSpeaking = false;
                            }
                            if (!wasSpeaking)
                                continue;
                        }

                        // True frame batching: accumulate 2 frames, send together in 1 packet
                        byte[] encoded = processor.encode(rawAudio);
                        if (pendingFrame == null) {
                            pendingFrame = encoded; // Store first frame
                            continue;
                        }

                        // Second frame ready: combine both with length prefixes
                        byte[] batchedData = new byte[pendingFrame.length + encoded.length + 4];
                        batchedData[0] = (byte) ((pendingFrame.length >> 8) & 0xFF);
                        batchedData[1] = (byte) (pendingFrame.length & 0xFF);
                        System.arraycopy(pendingFrame, 0, batchedData, 2, pendingFrame.length);
                        int offset = 2 + pendingFrame.length;
                        batchedData[offset] = (byte) ((encoded.length >> 8) & 0xFF);
                        batchedData[offset + 1] = (byte) (encoded.length & 0xFF);
                        System.arraycopy(encoded, 0, batchedData, offset + 2, encoded.length);
                        pendingFrame = null;

                        MicPacket packet = new MicPacket();
                        packet.audioData = batchedData;
                        packet.playerid = Vars.player.id;

                        if (Vars.net.server() && !Vars.headless) {
                            for (NetConnection con : moddedClients) {
                                if (con.isConnected())
                                    con.send(packet, true);
                            }
                        } else {
                            Vars.net.send(packet, true);
                        }
                    }
                }
            } catch (Exception e) {
                Log.err("@ Capture Loop Error: @", TAG, e.getMessage());
            } finally {
                if (microphone != null && !forceMock)
                    microphone.close();
            }
        }, "VoiceChat-Capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopCapture() {
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    // Getters/Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopCapture();
            status = VoiceStatus.DISABLED;
        } else {
            if (status == VoiceStatus.DISABLED) {
                if (Vars.net.client()) {
                    status = VoiceStatus.WAITING_HANDSHAKE;
                    sendHandshake();
                } else if (Vars.net.server() && !Vars.headless) {
                    status = VoiceStatus.READY;
                }
            }
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted)
            stopCapture();
        else if (status == VoiceStatus.READY || status == VoiceStatus.CONNECTED) {
            startCapture();
            status = VoiceStatus.CONNECTED;
        }
    }

    public void toggleMute() {
        setMuted(!muted);
    }

    public VoiceMode getSpeakerMode() {
        return speakerMode;
    }

    public void setSpeakerMode(VoiceMode mode) {
        this.speakerMode = mode;
    }

    public VoiceMode getMicMode() {
        return micMode;
    }

    public void setMicMode(VoiceMode mode) {
        this.micMode = mode;
    }

    public boolean isForceMock() {
        return forceMock;
    }

    public void setForceMock(boolean f) {
        this.forceMock = f;
        stopCapture();
        if (enabled && !muted && status == VoiceStatus.CONNECTED)
            startCapture();
    }

    public void setPlayerMuted(String id, boolean m) {
        playerMuted.put(id, m);
    }

    public boolean isPlayerMuted(String id) {
        return playerMuted.get(id, false);
    }

    public void setPlayerVolume(String id, float v) {
        playerVolume.put(id, v);
    }

    public float getPlayerVolume(String id) {
        return playerVolume.get(id, 1f);
    }

    public boolean isSpeaking(String id) {
        return Time.timeSinceMillis(lastSpeakingTime.get(id, 0L)) < SPEAKING_THRESHOLD_MS;
    }

    public boolean isSelfSpeaking() {
        return isRecording() && microphone != null && microphone.available() > 0;
    }

    public boolean isRecording() {
        return enabled && !muted && status == VoiceStatus.CONNECTED;
    }

    public VoiceStatus getStatus() {
        return status;
    }

    public String getStatusText() {
        if (status == VoiceStatus.CONNECTED)
            return "[green]Connected";
        if (status == VoiceStatus.READY)
            return "[lime]Ready";
        if (status == VoiceStatus.WAITING_HANDSHAKE)
            return "[yellow]Connecting...";
        if (status == VoiceStatus.DISABLED)
            return "[gray]Disabled";
        return "[red]Error";
    }

    public void showSettings() {
        new VoiceChatSettingsDialog(this).show();
    }

    public void dispose() {
        stopCapture();
        if (microphone != null)
            microphone.close();
        if (speaker != null)
            speaker.close();
    }
}
