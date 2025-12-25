package mindustrytool.plugins.voicechat;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Nullable;
import lemmesay.shared.LemmeSayConstants;
import lemmesay.shared.packet.MicPacket;
import lemmesay.shared.packet.VoiceRequestPacket;
import lemmesay.shared.packet.VoiceResponsePacket;
import mindustry.Vars;
import mindustry.net.Net;

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

    @Nullable
    private VoiceMicrophone microphone;
    @Nullable
    private VoiceSpeaker speaker;
    @Nullable
    private VoiceProcessor processor;
    @Nullable
    private Thread captureThread;

    public VoiceChatManager() {
        Log.info("@ Manager created", TAG);
    }

    /**
     * Initialize voice chat system.
     * Registers packets and sets up handlers.
     */
    public void init() {
        if (initialized)
            return;

        registerPackets();

        // Server-side Logic: Handshake with new players (Reverse Ping)
        // Check for specific version compatibility using a unique ID
        final long MAGIC_PING_ID = -291104L; // Changed from -291103L to avoid conflict with older versions

        arc.Events.on(mindustry.game.EventType.PlayerJoin.class, e -> {
            if (Vars.net.server()) {
                // Send Magic Ping to probe client
                mindustry.gen.PingCallPacket ping = new mindustry.gen.PingCallPacket();
                ping.time = MAGIC_PING_ID;
                if (e.player.con != null)
                    e.player.con.send(ping, true);
            }
        });

        // Client-side Logic: Reply to Magic Ping
        Vars.net.handleClient(mindustry.gen.PingCallPacket.class, ping -> {
            if (ping.time == MAGIC_PING_ID) {
                mindustry.gen.PingResponseCallPacket response = new mindustry.gen.PingResponseCallPacket();
                response.time = MAGIC_PING_ID;
                Vars.net.send(response, true);
            }
        });

        // Server-side Logic: Receive Reply -> Start Voice
        Vars.net.handleServer(mindustry.gen.PingResponseCallPacket.class, (con, ping) -> {
            if (ping.time == MAGIC_PING_ID && con.player != null) {
                Log.info("@ Client @ verified modded. Sending voice handshake.", TAG, con.player.name);
                VoiceRequestPacket request = new VoiceRequestPacket();
                request.protocolVersion = LemmeSayConstants.PROTOCOL_VERSION;
                con.send(request, true);
            }
        });

        // Sync voice chat status when world is loaded (joining server)
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            Log.info("@ WorldLoadEvent fired, syncing status", TAG);
            syncStatusForCurrentConnection();
        });

        // Initialize audio components (lazy - only when actually used)
        initialized = true;
        Log.info("@ Initialized successfully", TAG);

        // IMPORTANT: Check if already connected to server when init() is called
        // This handles the case where Voice Chat is enabled AFTER joining server
        if (enabled && status == VoiceStatus.DISABLED && Vars.net.active()) {
            Log.info("@ Already connected to server, syncing status immediately", TAG);
            syncStatusForCurrentConnection();
        }
    }

    /**
     * Sync voice chat status for current connection.
     * Called after init() if already in server, or on WorldLoadEvent.
     */
    private void syncStatusForCurrentConnection() {
        if (!enabled || status != VoiceStatus.DISABLED || !Vars.net.active()) {
            return;
        }

        // Simplified: Set status to READY immediately when connected
        // Handshake verification can be reimplemented later if needed
        if (Vars.net.client() || (Vars.net.server() && !Vars.headless)) {
            status = VoiceStatus.READY;
            Log.info("@ Status set to READY (connected to: @)", TAG,
                    Vars.net.client() ? "server" : "hosting");
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
            Log.info("@ Audio components initialized", TAG);
        } catch (Throwable e) {
            Log.err("@ Failed to initialize audio components: @", TAG, e.getMessage());
        }
    }

    /**
     * Register LemmeSay shared packets with Mindustry's network system.
     * Uses static flag to prevent re-registration causing packet ID mismatch.
     */
    private static boolean packetsRegistered = false;

    private void registerPackets() {
        if (packetsRegistered) {
            Log.info("@ Packets already registered, skipping", TAG);
            return;
        }

        try {
            Net.registerPacket(MicPacket::new);
            Net.registerPacket(VoiceRequestPacket::new);
            Net.registerPacket(VoiceResponsePacket::new);
            packetsRegistered = true;
            Log.info("@ Packets registered successfully", TAG);
        } catch (Exception e) {
            Log.warn("@ Packets registration error: @", TAG, e.getMessage());
        }

        // Handle incoming voice request from server
        Vars.net.handleClient(VoiceRequestPacket.class, packet -> {
            Log.info("@ Received voice request from server (protocol: @)", TAG, packet.protocolVersion);

            VoiceResponsePacket response = new VoiceResponsePacket();
            if (packet.protocolVersion != LemmeSayConstants.PROTOCOL_VERSION) {
                response.responseCode = packet.protocolVersion < LemmeSayConstants.PROTOCOL_VERSION
                        ? LemmeSayConstants.RESPONSE_SERVER_OUTDATED
                        : LemmeSayConstants.RESPONSE_CLIENT_OUTDATED;
                Vars.net.send(response, true);
                Log.warn("@ Protocol mismatch, voice chat disabled", TAG);
                return;
            }

            response.responseCode = LemmeSayConstants.RESPONSE_ACCEPTED;
            Vars.net.send(response, true);
            status = VoiceStatus.READY; // Handshake successful

            // Start voice capture if enabled
            if (enabled && !muted) {
                startCapture();
            }
        });

        // Handle incoming audio on SERVER (Forwarding logic)
        Vars.net.handleServer(MicPacket.class, (con, packet) -> {
            if (con.player == null)
                return;

            // Enforce sender ID from connection to prevent spoofing
            packet.playerid = con.player.id;

            // Log removed to prevent lag - was running every packet

            // Forward to all other clients (TCP for testing reliability)
            Vars.net.sendExcept(con, packet, true);

            // If I am the Host Player (not headless), I also need to hear this!
            if (!Vars.headless) {
                processIncomingVoice(packet);
            }
        });

        // Handle incoming audio from other players (Client-side)
        Vars.net.handleClient(MicPacket.class, packet -> {
            processIncomingVoice(packet);
        });
    }

    private void processIncomingVoice(MicPacket packet) {
        if (!enabled)
            return;

        // Ignore own packets to prevent echo
        if (Vars.player != null && packet.playerid == Vars.player.id)
            return;

        // Log trace (Debug Mode: forceMock=true)
        if (forceMock) {
            Log.info("@ Packet received from @, size: @", TAG, packet.playerid, packet.audioData.length);
        }

        mindustry.gen.Player sender = mindustry.gen.Groups.player.find(p -> p.id == packet.playerid);
        if (sender == null) {
            if (forceMock)
                Log.warn("@ Unknown sender ID: @", TAG, packet.playerid);
            return;
        }

        // Check per-player mute
        if (playerMuted.get(sender.uuid(), false))
            return;

        // Check Speaker Mode (Receive Filter)
        if (speakerMode == VoiceMode.TEAM) {
            if (Vars.player.team() != sender.team())
                return;
        }

        ensureAudioInitialized();
        if (speaker != null && processor != null) {
            try {
                short[] decoded = processor.decode(packet.audioData);

                // Apply volume
                float volume = playerVolume.get(sender.uuid(), 1f);
                if (volume != 1f) {
                    for (int i = 0; i < decoded.length; i++) {
                        decoded[i] = (short) Math.max(Short.MIN_VALUE,
                                Math.min(Short.MAX_VALUE, decoded[i] * volume));
                    }
                }

                speaker.play(decoded);
            } catch (Throwable e) {
                // Log.err("@ Failed to play audio: @", TAG, e.getMessage()); // Avoid spam
            }
        }
    }

    /**
     * Start voice capture thread.
     */
    public void startCapture() {
        if (captureThread != null && captureThread.isAlive()) {
            return;
        }

        ensureAudioInitialized();

        if (microphone == null) {
            try {
                microphone = new VoiceMicrophone();
            } catch (Throwable e) {
                Log.warn("@ Failed to create microphone: @", TAG, e.getMessage());
            }
        }

        // Only use mock mic when EXPLICITLY enabled via Debug Log
        final boolean useMock = forceMock;

        // If mic unavailable and not using mock, abort capture
        if (microphone == null && !useMock) {
            Log.warn("@ No microphone available and Mock Mic is disabled. Voice capture aborted.", TAG);
            status = VoiceStatus.MIC_ERROR;
            return;
        }

        captureThread = new Thread(() -> {
            try {
                if (!useMock) {
                    microphone.open();
                    microphone.start();
                    Log.info("@ Microphone capture started", TAG);
                } else {
                    Log.info("@ Mock Microphone started (Debug Log enabled)", TAG);
                }

                // For mock mic: generating sine wave
                long mockTime = 0;

                while (enabled && !muted && Vars.net.active()) {
                    short[] rawAudio;

                    if (!useMock) {
                        // Real Mic Logic
                        if (microphone.available() >= VoiceConstants.BUFFER_SIZE) {
                            rawAudio = microphone.read();
                        } else {
                            Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS);
                            continue;
                        }
                    } else {
                        // Mock Mic Logic: Generate 440Hz Sine Wave beep
                        // Beep for 500ms, silence for 500ms
                        boolean beep = (System.currentTimeMillis() / 1000) % 2 == 0;
                        rawAudio = new short[VoiceConstants.BUFFER_SIZE];
                        if (beep) {
                            for (int i = 0; i < rawAudio.length; i++) {
                                // 48000Hz sample rate, 440Hz tone
                                rawAudio[i] = (short) (Math.sin((mockTime + i) * 2.0 * Math.PI * 440.0 / 48000.0)
                                        * 10000);
                            }
                        }
                        mockTime += rawAudio.length;
                        Thread.sleep(20); // Simulate buffer fill time
                    }

                    if (processor != null) {
                        // Denoise (skip if mock to preserve pure tone for testing)
                        short[] processed = useMock ? rawAudio : processor.denoise(rawAudio);
                        byte[] encoded = processor.encode(processed);

                        MicPacket packet = new MicPacket();
                        packet.audioData = encoded;
                        packet.playerid = Vars.player.id;

                        // Log removed to prevent lag - was running every 20ms
                        Vars.net.send(packet, true); // TCP for reliability
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.err("@ Capture error: @", TAG, e.getMessage());
                e.printStackTrace();
            } finally {
                if (microphone != null && !useMock) {
                    microphone.close();
                }
                Log.info("@ Capture stopped", TAG);
            }
        }, "VoiceChat-Capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stop voice capture.
     */
    public void stopCapture() {
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopCapture();
            status = VoiceStatus.DISABLED;
        } else {
            // If already connected (past handshake), start capture immediately
            if (status == VoiceStatus.READY && !muted && Vars.net.client()) {
                startCapture();
                status = VoiceStatus.CONNECTED;
            } else if (status == VoiceStatus.DISABLED) {
                status = VoiceStatus.WAITING_HANDSHAKE;

                // If already in a server, request handshake now
                if (Vars.net.client()) {
                    Log.info("@ Client requesting voice handshake from server...", TAG);
                    mindustry.gen.PingResponseCallPacket ping = new mindustry.gen.PingResponseCallPacket();
                    ping.time = -291104L; // MAGIC_PING_ID
                    Vars.net.send(ping, true);
                }

                // If hosting, set status to READY immediately (no handshake needed)
                if (Vars.net.server() && !Vars.headless) {
                    status = VoiceStatus.READY;
                    Log.info("@ Host mode: Status set to READY", TAG);
                }
            }
        }
        Log.info("@ Enabled: @, Status: @", TAG, enabled, status);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopCapture();
        } else if (enabled && (status == VoiceStatus.READY || status == VoiceStatus.CONNECTED) && Vars.net.active()) {
            startCapture();
            status = VoiceStatus.CONNECTED;
        }
        Log.info("@ Muted: @", TAG, muted);
    }

    public void toggleMute() {
        setMuted(!muted);
    }

    public VoiceMode getSpeakerMode() {
        return speakerMode;
    }

    public void setSpeakerMode(VoiceMode mode) {
        this.speakerMode = mode;
        Log.info("@ Speaker mode: @", TAG, mode);
    }

    public VoiceMode getMicMode() {
        return micMode;
    }

    public boolean isForceMock() {
        return forceMock;
    }

    public void setForceMock(boolean forceMock) {
        this.forceMock = forceMock;
        Log.info("@ Force Mock Mic: @", TAG, forceMock);

        // Always stop current capture first
        stopCapture();

        // Only restart if conditions are met
        if (enabled && !muted && Vars.net.active()
                && (status == VoiceStatus.READY || status == VoiceStatus.CONNECTED)) {
            startCapture();
            status = VoiceStatus.CONNECTED;
        }
    }

    public void setMicMode(VoiceMode mode) {
        this.micMode = mode;
        Log.info("@ Mic mode: @", TAG, mode);
    }

    // Per-player settings

    public void setPlayerMuted(String playerId, boolean muted) {
        playerMuted.put(playerId, muted);
        Log.info("@ Player @ muted: @", TAG, playerId, muted);
    }

    public boolean isPlayerMuted(String playerId) {
        return playerMuted.get(playerId, false);
    }

    public void setPlayerVolume(String playerId, float volume) {
        playerVolume.put(playerId, volume);
        Log.info("@ Player @ volume: @", TAG, playerId, volume);
    }

    public float getPlayerVolume(String playerId) {
        return playerVolume.get(playerId, 1f);
    }

    public VoiceStatus getStatus() {
        return status;
    }

    public String getStatusText() {
        switch (status) {
            case DISABLED:
                return "[gray]Disabled";
            case WAITING_HANDSHAKE:
                return "[yellow]Connecting...";
            case READY:
                return "[lime]Ready";
            case MIC_ERROR:
                return "[red]Mic Error";
            case SPEAKER_ERROR:
                return "[red]Speaker Error";
            case CONNECTED:
                return "[green]Connected";
            default:
                return "[gray]Unknown";
        }
    }

    /**
     * Dispose all resources.
     */
    public void dispose() {
        stopCapture();
        if (microphone != null) {
            microphone.close();
            microphone = null;
        }
        if (speaker != null) {
            speaker.close();
            speaker = null;
        }
        processor = null;
        Log.info("@ Disposed", TAG);
    }

    /**
     * Show settings dialog.
     */
    public void showSettings() {
        new VoiceChatSettingsDialog(this).show();
    }
}
