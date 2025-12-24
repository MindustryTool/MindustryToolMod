package mindustrytool.plugins.voicechat;

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

    private boolean initialized = false;
    private boolean enabled = false;
    private boolean muted = false;

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
        try {
            processor = new VoiceProcessor();
            speaker = new VoiceSpeaker();
            Log.info("@ Audio components initialized", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to initialize audio components", TAG);
            Log.err(e);
        }

        initialized = true;
        Log.info("@ Initialized successfully", TAG);
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
            if (speaker != null && processor != null && enabled) {
                try {
                    short[] decoded = processor.decode(packet.audioData);
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

                while (enabled && !muted && Vars.net.client()) {
                    if (microphone.available() >= VoiceConstants.BUFFER_SIZE) {
                        short[] audio = microphone.read();
                        if (processor != null) {
                            short[] denoised = processor.denoise(audio);
                            byte[] encoded = processor.encode(denoised);

                            MicPacket packet = new MicPacket();
                            packet.audioData = encoded;
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
