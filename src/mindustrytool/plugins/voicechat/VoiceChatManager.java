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
    // This prevents crashes when forwarding voice packets to vanilla clients
    private final ObjectSet<NetConnection> moddedClients = new ObjectSet<>();

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

        // Server-side Logic: Receive Handshake from Client (VoiceResponsePacket)
        Vars.net.handleServer(VoiceResponsePacket.class, (con, packet) -> {
            if (con.player != null) {
                if (packet.protocolVersion == LemmeSayConstants.PROTOCOL_VERSION) {
                    Log.info("@ Client @ verified modded (Protocol @). Adding to voice recipients.", TAG,
                            con.player.name, packet.protocolVersion);
                    moddedClients.add(con);

                    // Send Ack back to client
                    VoiceRequestPacket ack = new VoiceRequestPacket();
                    ack.protocolVersion = LemmeSayConstants.PROTOCOL_VERSION;
                    con.send(ack, true);
                } else {
                    Log.warn("@ Client @ protocol mismatch. Server: @, Client: @", TAG, con.player.name,
                            LemmeSayConstants.PROTOCOL_VERSION, packet.protocolVersion);
                }
            }
        });

        // Cleanup: Remove client from modded set when they disconnect
        arc.Events.on(mindustry.game.EventType.PlayerLeave.class, e -> {
            if (Vars.net.server() && e.player.con != null) {
                if (moddedClients.remove(e.player.con)) {
                    Log.info("@ Removed @ from modded clients list", TAG, e.player.name);
                }
            }
        });

        // Sync voice chat status when world is loaded (joining server)
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            Log.info("@ WorldLoadEvent fired, syncing status", TAG);
            syncStatusForCurrentConnection();
        });

        // Cleanup when returning to menu (Fixes Audio Loop on Disconnect)
        arc.Events.on(mindustry.game.EventType.StateChangeEvent.class, e -> {
            if (e.to == mindustry.core.GameState.State.menu) {
                Log.info("@ Returned to menu, cleaning up voice resources", TAG);
                stopCapture();
                if (speaker != null)
                    speaker.close();
                status = VoiceStatus.DISABLED;
                moddedClients.clear();
            }
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

        // Client: Send handshake to server immediately
        if (Vars.net.client()) {
            status = VoiceStatus.WAITING_HANDSHAKE;
            sendHandshake(); // <--- PROACTIVE HANDSHAKE
            Log.info("@ Client: Sent handshake to server", TAG);
        } else if (Vars.net.server() && !Vars.headless) {
            // Host: Ready immediately
            status = VoiceStatus.READY;
            // Auto-start capture if enabled
            if (enabled && !muted) {
                startCapture();
                status = VoiceStatus.CONNECTED;
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

        // Handle incoming voice request from server (ACK)
        Vars.net.handleClient(VoiceRequestPacket.class, packet -> {
            Log.info("@ Received voice ack from server (protocol: @)", TAG, packet.protocolVersion);

            if (packet.protocolVersion != LemmeSayConstants.PROTOCOL_VERSION) {
                Log.warn("@ Protocol mismatch, voice chat disabled", TAG);
                return;
            }

            status = VoiceStatus.READY; // Handshake successful

            // Start voice capture if enabled
            if (enabled && !muted) {
                startCapture();
            }
        });

        // Handle incoming audio on SERVER (Forwarding logic) is in init() now?
        // No, registerPackets() is called BY init().
        // Wait, handleServer(MicPacket) was in registerPackets in Step 3141 diff?
        // Step 3137 shows lines 230-253. These were NOT touched in Step 3141. So they
        // remain.
        // I need to ensure I don't delete them if I use write_to_file.
        // Using write_to_file replaces ENTIRE file.
        // I must be careful.

        // I will copy the logic:

        // Handle incoming audio on SERVER (Forwarding logic)
        Vars.net.handleServer(MicPacket.class, (con, packet) -> {
            if (con.player == null)
                return;

            // Only accept voice from verified modded clients
            if (!moddedClients.contains(con)) {
                return;
            }

            // Enforce sender ID from connection to prevent spoofing
            packet.playerid = con.player.id;

            // Forward ONLY to verified modded clients (prevents crash for vanilla clients)
            for (NetConnection other : moddedClients) {
                if (other != con && other.isConnected()) {
                    other.send(packet, false); // Unreliable (UDP) for lower latency
                }
            }

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
        if (sender != null) {
            lastSpeakingTime.put(sender.uuid(), arc.util.Time.millis());
        }

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
     * Send handshake packet to server to register as a modded client.
     */
    public void sendHandshake() {
        if (!Vars.net.client() || !Vars.net.active())
            return;

        VoiceResponsePacket packet = new VoiceResponsePacket();
        packet.responseCode = LemmeSayConstants.RESPONSE_ACCEPTED;
        packet.protocolVersion = LemmeSayConstants.PROTOCOL_VERSION;
        Vars.net.send(packet, true);
        Log.info("@ Sending proactive handshake (Protocol @)", TAG, packet.protocolVersion);
    }

    /**
     * Start voice capture thread.
     */
    public void startCapture() {
        Log.info("@ startCapture() called. muted=@, enabled=@, status=@", TAG, muted, enabled, status);
        if (captureThread != null && captureThread.isAlive()) {
            Log.info("@ Capture thread already running, skipping", TAG);
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
                    Log.info("@ Microphone capture started (isServer=@, moddedClients=@)", TAG, Vars.net.server(),
                            moddedClients.size);
                } else {
                    Log.info("@ Mock Microphone started (Debug Log enabled)", TAG);
                }

                // For mock mic: generating sine wave
                long mockTime = 0;
                long lastSpeakingTimeVAD = 0;
                boolean wasSpeaking = false;

                while (enabled && !muted && Vars.net.active()) {
                    short[] rawAudio;

                    if (!useMock) {
                        // Real Mic Logic
                        if (microphone.available() >= VoiceConstants.BUFFER_SIZE) {
                            rawAudio = microphone.read();
                        } else {
                            // Calculate sleep time properly to avoid busy wait
                            // But keeping logical simple
                            try {
                                Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS / 2);
                            } catch (Exception e) {
                            }
                            continue;
                        }
                    } else {
                        // Mock Mic Logic: Generate 440Hz Sine Wave beep
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
                        Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS);
                    }

                    if (processor != null) {
                        // VAD (Voice Activity Detection) - Skip silence to save bandwidth (Fix Lag)
                        if (!useMock) {
                            double rms = processor.calculateRMS(rawAudio);
                            if (rms > VoiceProcessor.VAD_THRESHOLD) {
                                lastSpeakingTimeVAD = System.currentTimeMillis();
                                wasSpeaking = true;
                            } else {
                                // 400ms hangover to prevent cut-off words
                                if (System.currentTimeMillis() - lastSpeakingTimeVAD > 400) {
                                    wasSpeaking = false;
                                }
                            }

                            if (!wasSpeaking) {
                                continue; // Skip silent packet -> Huge bandwidth saving!
                            }
                        }

                        // Denoise (skip if mock to preserve pure tone for testing)
                        short[] processed = useMock ? rawAudio : processor.denoise(rawAudio);
                        byte[] encoded = processor.encode(processed);

                        MicPacket packet = new MicPacket();
                        packet.audioData = encoded;
                        packet.playerid = Vars.player.id;

                        // Host: Send directly to all modded clients
                        // Client: Use normal net.send (will be handled by server)
                        if (Vars.net.server() && !Vars.headless) {
                            // Host sends directly to all verified modded clients
                            int sentCount = 0;
                            for (NetConnection con : moddedClients) {
                                if (con.isConnected()) {
                                    con.send(packet, false); // Unreliable (UDP) for lower latency
                                    sentCount++;
                                }
                            }
                            // Debug log every 50 packets (roughly every 1 second)
                            if (sentCount > 0 && System.currentTimeMillis() % 1000 < 25) {
                                Log.info("@ Host sent voice to @ modded clients", TAG, sentCount);
                            }
                        } else {
                            // Client sends to server (server will forward)
                            Vars.net.send(packet, false); // Unreliable (UDP) for lower latency
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Catch ALL exceptions to prevent crash from Encoder or Driver issues
                Log.err("@ Capture loop crashed: @", TAG, e.getMessage());
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
            if (status == VoiceStatus.READY && !muted && Vars.net.active()) {
                startCapture();
                status = VoiceStatus.CONNECTED;
            } else if (status == VoiceStatus.DISABLED) {
                status = VoiceStatus.WAITING_HANDSHAKE;

                // If already in a server, request handshake now
                if (Vars.net.client()) {
                    Log.info("@ Client requesting voice handshake from server...", TAG);
                    // Use new handshake
                    sendHandshake();
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
        } else {
            // Check if we can start capture
            boolean isReady = status == VoiceStatus.READY || status == VoiceStatus.CONNECTED;

            // Special case for Host: If we are server/host and active, we should be ready.
            // Force status to READY if it seems stuck.
            if (Vars.net.server() && Vars.net.active() && !Vars.headless && enabled) {
                isReady = true;
                if (status != VoiceStatus.CONNECTED)
                    status = VoiceStatus.READY;
            }

            if (enabled && isReady && Vars.net.active()) {
                startCapture();
                status = VoiceStatus.CONNECTED;
            } else {
                // Debug: why startCapture was NOT called
                Log.info("@ setMuted(false) but startCapture NOT called: enabled=@, status=@, net.active=@, isReady=@",
                        TAG, enabled, status, Vars.net.active(), isReady);
            }
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

    public boolean isSpeaking(String playerId) {
        // Check if player has sent audio recently
        long lastTime = lastSpeakingTime.get(playerId, 0L);
        return Time.timeSinceMillis(lastTime) < SPEAKING_THRESHOLD_MS;
    }

    public boolean isSelfSpeaking() {
        // Check if we are capturing audio
        return isRecording() && microphone != null && microphone.available() > 0;
    }

    public boolean isRecording() {
        return enabled && !muted && status == VoiceStatus.CONNECTED;
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
