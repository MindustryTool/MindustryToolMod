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

    private boolean initialized = false;
    private boolean enabled = false; // Speaker enabled
    private boolean muted = false; // Mic muted (inverse of Mic enabled)

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

        // Initialize audio components (lazy - only when actually used)
        initialized = true;
        Log.info("@ Initialized successfully", TAG);
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
        } catch (Exception e) {
            Log.err("@ Failed to initialize audio components", TAG);
            Log.err(e);
        }
    }

    /**
     * Register LemmeSay shared packets with Mindustry's network system.
     */
    private void registerPackets() {
        try {
            Net.registerPacket(MicPacket::new);
            Net.registerPacket(VoiceRequestPacket::new);
            Net.registerPacket(VoiceResponsePacket::new);
            Log.info("@ Packets registered", TAG);
        } catch (Exception e) {
            Log.warn("@ Packets may already be registered: @", TAG, e.getMessage());
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

            // Start voice capture if enabled
            if (enabled && !muted) {
                startCapture();
            }
        });

        // Handle incoming audio from other players
        Vars.net.handleClient(MicPacket.class, packet -> {
            if (!enabled)
                return;

            mindustry.gen.Player sender = mindustry.gen.Groups.player.getByID(packet.playerid);
            if (sender == null)
                return;

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
                } catch (Exception e) {
                    Log.err("@ Failed to play audio: @", TAG, e.getMessage());
                }
            }
        });
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
            } catch (Exception e) {
                Log.err("@ Failed to create microphone: @", TAG, e.getMessage());
                return;
            }
        }

        captureThread = new Thread(() -> {
            try {
                microphone.open();
                microphone.start();
                Log.info("@ Microphone capture started", TAG);

                while (enabled && !muted && Vars.net.active()) {
                    if (microphone.available() >= VoiceConstants.BUFFER_SIZE) {
                        short[] audio = microphone.read();
                        if (processor != null) {
                            short[] denoised = processor.denoise(audio);
                            byte[] encoded = processor.encode(denoised);

                            MicPacket packet = new MicPacket();
                            packet.audioData = encoded;
                            packet.playerid = Vars.player.id; // Set sender ID (important!)
                            Vars.net.send(packet, false);
                        }
                    }
                    Thread.sleep(VoiceConstants.CAPTURE_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.err("@ Capture error: @", TAG, e.getMessage());
            } finally {
                if (microphone != null) {
                    microphone.close();
                }
                Log.info("@ Microphone capture stopped", TAG);
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
        }
        Log.info("@ Enabled: @", TAG, enabled);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopCapture();
        } else if (enabled && Vars.net.client()) {
            startCapture();
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
