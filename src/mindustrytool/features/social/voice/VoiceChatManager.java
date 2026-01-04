package mindustrytool.features.social.voice;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.net.NetConnection;
import mindustrytool.features.social.auth.AuthService;
import mindustrytool.features.social.voice.protocol.shared.packet.MicPacket;
import mindustrytool.features.social.voice.protocol.shared.packet.VoiceResponsePacket;

/**
 * VoiceChatManager - Core voice chat logic.
 * <p>
 * REFACTORED:
 * - Delegates Audio to {@link VoiceAudioController}
 * - Delegates Network to {@link VoiceNetworkService}
 * - Acts as central coordinator and State Manager.
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
    private volatile float masterVolume = 0.8f; // Master volume for output (0.0 - 2.0)
    private volatile boolean spatialAudioEnabled = true; // Enable 3D audio positioning

    // Modes
    private VoiceMode speakerMode = VoiceMode.ALL;
    private VoiceMode micMode = VoiceMode.ALL;

    // Per-player settings
    private final ObjectMap<String, Boolean> playerMuted = new ObjectMap<>();
    private final ObjectMap<String, Float> playerVolume = new ObjectMap<>(); // 0.0 - 1.0
    private final ObjectMap<String, Long> lastSpeakingTime = new ObjectMap<>();
    private static final long SPEAKING_THRESHOLD_MS = 300;

    private final ObjectSet<NetConnection> moddedClients = new ObjectSet<>();

    // Sub-Controllers
    private final VoiceAudioController audioController;
    private final VoiceNetworkService networkService;

    // --- Configuration Constants ---
    private static final int BUFFER_SIZE = 1920; // 40ms at 48kHz (960 samples * 2 bytes)
    // private static final int READ_SIZE = 960; // Samples to read
    private static final int CAPTURE_INTERVAL = 20; // ms
    private static final double VAD_THRESHOLD = 50.0;

    public VoiceChatManager() {
        Log.info("@ Manager created", TAG);
        this.audioController = new VoiceAudioController(this);
        this.networkService = new VoiceNetworkService(this);
    }

    /**
     * Initialize voice chat system.
     */
    public void init() {
        if (initialized)
            return;

        networkService.registerPackets();

        // Cleanup: Player Leave
        arc.Events.on(mindustry.game.EventType.PlayerLeave.class, e -> {
            if (Vars.net.server() && e.player.con != null) {
                moddedClients.remove(e.player.con);
                audioController.removePlayer(String.valueOf(e.player.id));
            }
        });

        // SERVER: Send voice chat request to newly connected players
        arc.Events.on(mindustry.game.EventType.PlayerJoin.class, e -> {
            if (Vars.net.server() && !Vars.headless && e.player.con != null) {
                networkService.sendRequest(e.player.con);
            }
        });

        // Sync status on World Load
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            syncStatus();
        });

        // Cleanup on Menu return
        arc.Events.on(mindustry.game.EventType.StateChangeEvent.class, e -> {
            if (e.to == mindustry.core.GameState.State.menu) {
                audioController.close();
                status = VoiceStatus.DISABLED;
                moddedClients.clear();
            }
        });

        // Retry Handshake Loop
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
        // Login check
        if (!AuthService.isLoggedIn()) {
            status = VoiceStatus.DISABLED;
            return;
        }

        if (!enabled || !Vars.net.active()) {
            return;
        }

        // Client: wait for server handshake
        if (Vars.net.client()) {
            if (status == VoiceStatus.READY || status == VoiceStatus.CONNECTED) {
                return;
            }
            status = VoiceStatus.WAITING_HANDSHAKE;
        } else if (Vars.net.server() && !Vars.headless) {
            status = VoiceStatus.CONNECTED; // Host is always CONNECTED
            if (!muted) {
                startCapture();
            }
        }
    }

    // --- Network Events Handlers (Called by NetworkService) ---

    public void onServerHandshakeRequest() {
        networkService.sendHandshakeResponse(); // NOW safe to send
        status = VoiceStatus.CONNECTED;
        if (enabled && !muted)
            startCapture();
    }

    public void onClientHandshakeResponse(NetConnection con, VoiceResponsePacket packet) {
        if (con.player != null) {
            if (!moddedClients.contains(con)) {
                moddedClients.add(con);
            }
            // Ack
            networkService.sendRequest(con);
        }
    }

    public void onServerReceiveAudio(NetConnection con, MicPacket packet) {
        if (con.player == null)
            return;
        if (!moddedClients.contains(con))
            return;

        int senderId = con.player.id;
        packet.playerid = senderId;

        // Forward
        for (NetConnection other : moddedClients) {
            if (!other.isConnected() || other.player == null)
                continue;
            if (other.player.id == senderId)
                continue;
            networkService.forwardAudioPacket(other, packet);
        }

        // Play local if host
        if (!Vars.headless) {
            processIncomingVoice(packet);
        }
    }

    // --- Audio Logic ---

    public void processIncomingVoice(MicPacket packet) {
        if (!enabled)
            return;

        // Self-echo prevention
        if (Vars.player != null && packet.playerid == Vars.player.id)
            return;

        mindustry.gen.Player sender = mindustry.gen.Groups.player.find(p -> p.id == packet.playerid);

        if (sender != null) {
            lastSpeakingTime.put(String.valueOf(sender.id), arc.util.Time.millis());

            if (playerMuted.get(String.valueOf(sender.id), false))
                return;
            if (speakerMode == VoiceMode.TEAM && Vars.player.team() != sender.team())
                return;

            // Spatial Updates
            audioController.updateMixerPosition(String.valueOf(sender.id), sender.x, sender.y);
        }

        // Listener Update
        if (Vars.player != null) {
            audioController.updateListenerPosition(Vars.player.x, Vars.player.y);
        }

        audioController.ensureInitialized(spatialAudioEnabled);

        try {
            byte[] data = packet.audioData;
            if (data.length > 4) {
                int offset = 0;
                int frameCount = 0;
                while (offset < data.length - 2) {
                    int frameLen = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                    if (frameLen <= 0 || offset + 2 + frameLen > data.length)
                        break;

                    byte[] frameData = new byte[frameLen];
                    System.arraycopy(data, offset + 2, frameData, 0, frameLen);

                    audioController.queueAudio(String.valueOf(packet.playerid), frameData,
                            packet.sequence + frameCount);
                    frameCount++;

                    offset += 2 + frameLen;
                }
            }
        } catch (Throwable e) {
            // Log.warn
        }
    }

    public void sendAudioPacket(byte[] data, int sequence) {
        if (Vars.net.client() && status != VoiceStatus.CONNECTED)
            return;

        MicPacket packet = new MicPacket();
        packet.audioData = data;
        packet.playerid = Vars.player.id;
        packet.sequence = sequence;

        if (Vars.net.client()) {
            networkService.sendAudioPacket(packet, false);
        } else if (Vars.net.server() && !Vars.headless) {
            // Host Logic
            processIncomingVoice(packet); // Echo local
            // Forward to all clients
            for (NetConnection con : moddedClients) {
                if (con.player == null || con.player.id == Vars.player.id)
                    continue;
                networkService.forwardAudioPacket(con, packet);
            }
        }
    }

    public void startCapture() {
        if (muted)
            return;
        audioController.startCapture(forceMock, BUFFER_SIZE, CAPTURE_INTERVAL, VAD_THRESHOLD);
    }

    public void stopCapture() {
        audioController.stopCapture();
    }

    public void setMicError() {
        this.status = VoiceStatus.MIC_ERROR;
    }

    // --- State Getters/Setters ---

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
                } else if (Vars.net.server() && !Vars.headless) {
                    status = VoiceStatus.READY;
                }
            }
        }
    }

    public boolean isRecording() {
        return enabled && !muted && status == VoiceStatus.CONNECTED;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopCapture();
        } else {
            if (enabled) {
                if (Vars.net.active() && (status == VoiceStatus.DISABLED || status == VoiceStatus.MIC_ERROR)) {
                    stopCapture();
                    syncStatus();
                }

                if (status == VoiceStatus.CONNECTED || status == VoiceStatus.READY) {
                    startCapture();
                } else if (Vars.net.client() && status == VoiceStatus.WAITING_HANDSHAKE) {
                    Vars.ui.hudfrag.showToast("Server does not support Voice Chat");
                    this.muted = true;
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

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(2.0f, volume));
        audioController.setMasterVolume(masterVolume);
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setSpatialAudioEnabled(boolean enabled) {
        this.spatialAudioEnabled = enabled;
        audioController.setSpatialEnabled(enabled);
    }

    public boolean isSpatialAudioEnabled() {
        return spatialAudioEnabled;
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
        audioController.setPlayerVolume(id, v);
    }

    public float getPlayerVolume(String id) {
        return playerVolume.get(id, 1f);
    }

    public boolean isSpeaking(String id) {
        return Time.timeSinceMillis(lastSpeakingTime.get(id, 0L)) < SPEAKING_THRESHOLD_MS;
    }

    public boolean isSelfSpeaking() {
        return isRecording() && audioController.isMicAvailable();
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
        audioController.close();
    }

    public void update() {
        if (!enabled)
            return;
        if (Vars.net.client() && Vars.net.active()) {
            if (status == VoiceStatus.WAITING_HANDSHAKE) {
                // Auto-retry logic or timeout here
            }
        }
    }
}
