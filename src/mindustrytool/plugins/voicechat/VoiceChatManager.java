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
        CONNECTED; // Actively connected and transmitting

        public boolean isError() {
            return this == MIC_ERROR || this == SPEAKER_ERROR;
        }
    }

    private VoiceStatus status = VoiceStatus.DISABLED;

    private volatile boolean initialized = false;
    private volatile boolean enabled = true; // Speaker enabled by default
    private volatile boolean muted = true; // Mic muted by default (user must unmute to speak)
    private volatile boolean forceMock = false; // Force usage of mock microphone

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
                if (mixer != null)
                    mixer.removePlayer(e.player.uuid()); // Fix Memory Leak
            }
        });

        // Sync status on World Load
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            syncStatus();
        });

        // Cleanup on Menu return
        arc.Events.on(mindustry.game.EventType.StateChangeEvent.class, e -> {
            if (e.to == mindustry.core.GameState.State.menu) {
                stopCapture();
                if (mixer != null)
                    mixer.removePlayer("all"); // Clear buffers
                if (speaker != null)
                    speaker.close();
                status = VoiceStatus.DISABLED;
                moddedClients.clear();
            }
        });

        // Retry Handshake Loop (Fix for Windows Client Connectivity)
        arc.Events.run(mindustry.game.EventType.Trigger.update, () -> {
            update();
        });

        initialized = true;

        // Check if already in game
        if (enabled && status == VoiceStatus.DISABLED && Vars.net.active()) {
            syncStatus();
        }
    }

    public void syncStatus() {
        if (!enabled || !Vars.net.active()) {
            Log.info("@ Sync skipped: Enabled=@, NetActive=@", TAG, enabled, Vars.net.active());
            return;
        }

        // Always re-handshake on WorldLoad (fixes rejoin issues)
        if (Vars.net.client()) {
            status = VoiceStatus.WAITING_HANDSHAKE;
            sendHandshake();
            Log.info("@ Client: Handshake sent to server", TAG);
        } else if (Vars.net.server() && !Vars.headless) {
            status = VoiceStatus.READY;
            Log.info("@ Host: Set status READY. Muted=@", TAG, muted);
            if (!muted) {
                startCapture();
                status = VoiceStatus.CONNECTED;
                Log.info("@ Host: Capture started", TAG);
            }
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

            // Enable Mixer on ALL platforms (Android now supports pull-mode)
            if (mixer == null) {
                mixer = new AudioMixer();
                mixer.setProcessor(processor);
                // Sync initial volumes
                if (playerVolume != null) {
                    playerVolume.each((id, vol) -> mixer.setVolume(id, vol));
                }

                // Inject mixer into speaker for pull-based playback
                speaker.setMixer(mixer);
            }
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
        // Check if sender is valid (prevent echo loop)
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
                // Batched packet: contains 2 frames with length prefixes
                // Format: [len1:2bytes][frame1:len1bytes][len2:2bytes][frame2:len2bytes]
                if (data.length > 4) {
                    int offset = 0;
                    int frameCount = 0;
                    while (offset < data.length - 2) {
                        int frameLen = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                        if (frameLen <= 0 || offset + 2 + frameLen > data.length)
                            break;

                        byte[] frameData = new byte[frameLen];
                        System.arraycopy(data, offset + 2, frameData, 0, frameLen);

                        // Route through mixer (It handles Decoding + PLC + Mixing)
                        if (mixer != null) {
                            String mixId = sender != null ? sender.uuid() : "unknown_" + packet.playerid;

                            // Update positions for Spatial Audio
                            if (sender != null) {
                                mixer.updatePosition(mixId, sender.x, sender.y);
                            }
                            if (mindustry.Vars.player != null) {
                                mixer.updateListener(mindustry.Vars.player.x, mindustry.Vars.player.y);
                            }

                            mixer.queueAudio(mixId, frameData);
                        }

                        offset += 2 + frameLen;
                        // frameCount++; // Unused variable removed
                    }
                }
            } catch (Throwable e) {
                // Log errors but limit spam (only log occasionally)
                if (Math.random() < 0.01) { // 1% chance to log
                    Log.warn("@ Audio processing error: @", TAG, e.getMessage());
                }
            }
        }
    }

    public void sendHandshake() {
        if (!Vars.net.client() || !Vars.net.active())
            return;
        Log.info("@ Client: Sending Handshake (VoiceResponsePacket)...", TAG);
        VoiceResponsePacket packet = new VoiceResponsePacket();
        packet.responseCode = LemmeSayConstants.RESPONSE_ACCEPTED;
        Vars.net.send(packet, true);
    }

    public void startCapture() {
        Log.info("@ startCapture() called. Active=@, Enabled=@, Muted=@", TAG, Vars.net.active(), enabled, muted);

        if (muted) {
            Log.info("@ Start capture aborted: Player is muted.", TAG);
            return;
        }

        if (captureThread != null && captureThread.isAlive()) {
            Log.info("@ Capture thread already alive. Skipping.", TAG);
            return;
        }
        ensureAudioInitialized();

        if (microphone == null) {
            try {
                microphone = new VoiceMicrophone();
            } catch (Exception e) {
                Log.warn("@ Failed to create VoiceMicrophone: @", TAG, e.getMessage());
            }
        }

        if (microphone == null && !forceMock) {
            Log.err("@ Microphone init failed (null). ForceMock=@", TAG, forceMock);
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
        // CRITICAL FIX: Close microphone immediately to unblock the reading thread
        // TargetDataLine.read is blocking and doesn't respect interruptions.
        if (microphone != null) {
            microphone.close();
            microphone = null; // FORCE NULL: Ensure fresh instance next time
        }

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
        Log.info("@ Muted: @", TAG, muted);

        // Auto-react to mute state change
        if (muted) {
            stopCapture();
        } else {
            // If unmuting...
            if (enabled) {
                // If we are active in a game but status is stuck (e.g. Disabled), try to sync
                // first
                // If we are active in a game but status is stuck (e.g. Disabled), try to sync
                // first
                if (Vars.net.active() && (status == VoiceStatus.DISABLED || status == VoiceStatus.MIC_ERROR)) {
                    Log.info("@ Unmute forcing status sync (Deep Reset)...", TAG);
                    stopCapture(); // CRITICAL: Force cleanup of any stale/broken mic instance
                    syncStatus();
                }

                // If valid state, start
                if (status == VoiceStatus.CONNECTED || status == VoiceStatus.READY
                        || status == VoiceStatus.WAITING_HANDSHAKE) {
                    // Determine if we should start based on connection type
                    if (Vars.net.client() && status == VoiceStatus.CONNECTED) {
                        startCapture();
                    } else if (Vars.net.server() && !Vars.headless) {
                        startCapture();
                    }
                }
            }
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
        if (mixer != null) {
            mixer.setVolume(id, v); // Forward to Audio Thread
        }
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

    private long lastHandshakeTime = 0;

    public void update() {
        if (!enabled)
            return;

        // Auto-Retry Handshake for Clients
        if (Vars.net.client() && Vars.net.active()
                && (status == VoiceStatus.WAITING_HANDSHAKE || status == VoiceStatus.DISABLED)) {
            if (arc.util.Time.timeSinceMillis(lastHandshakeTime) > 2000) {
                status = VoiceStatus.WAITING_HANDSHAKE;
                sendHandshake();
                lastHandshakeTime = arc.util.Time.millis();
                // Log.info("@ Retrying handshake...", TAG); // Debug spam check
            }
        }
    }
}
