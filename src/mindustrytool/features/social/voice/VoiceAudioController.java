package mindustrytool.features.social.voice;

import arc.util.Log;
import arc.util.Nullable;

/**
 * Controller for Voice System (Audio I/O).
 * Manages Microphone, Speaker, VAD, and Audio Processing.
 * <p>
 * This component is responsible for the "Physical" layer of voice chat:
 * <ul>
 * <li>Initializing audio subsystems (Microphone, Speaker, Processor).</li>
 * <li>Running the background thread for audio capture
 * ({@code captureLoop}).</li>
 * <li>Handling Voice Activity Detection (VAD).</li>
 * <li>Batching and queueing audio frames to/from the network service.</li>
 * </ul>
 */
public class VoiceAudioController {
    private static final String TAG = "[VoiceAudio]";
    private final VoiceChatManager manager;

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

    public VoiceAudioController(VoiceChatManager manager) {
        this.manager = manager;
    }

    public synchronized void ensureInitialized(boolean spatialAudioEnabled) {
        if (processor != null && speaker != null && mixer != null)
            return;

        try {
            if (processor == null) {
                processor = new VoiceProcessor();
            }
            if (speaker == null) {
                speaker = new VoiceSpeaker();
            }

            if (mixer == null) {
                mixer = new AudioMixer();
                mixer.setProcessor(processor);
                mixer.setSpatialEnabled(spatialAudioEnabled);

                // Inject mixer into speaker for pull-based playback
                speaker.setMixer(mixer);
            }
        } catch (Throwable e) {
            Log.err("@ Failed to init audio: @", TAG, e.getMessage());
        }
    }

    public void startCapture(boolean forceMock, int bufferSize, int captureIntervalMs, double vadThreshold) {
        if (captureThread != null && captureThread.isAlive()) {
            return;
        }

        ensureInitialized(manager.isSpatialAudioEnabled());

        if (microphone == null) {
            try {
                microphone = new VoiceMicrophone();
            } catch (Exception e) {
                Log.warn("@ Failed to create VoiceMicrophone: @", TAG, e.getMessage());
            }
        }

        if (microphone == null && !forceMock) {
            Log.err("@ Microphone init failed (null).", TAG);
            manager.setMicError();
            return;
        }

        captureThread = new Thread(() -> captureLoop(forceMock, bufferSize, captureIntervalMs, vadThreshold),
                "VoiceChat-Capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop(boolean forceMock, int bufferSize, int captureIntervalMs, double vadThreshold) {
        try {
            if (!forceMock) {
                microphone.open();
                microphone.start();
            }

            long lastSpeakingTimeVAD = 0;
            boolean wasSpeaking = false;
            long mockTime = 0;
            byte[] pendingFrame = null;
            int localSequence = 0;

            while (manager.isRecording()) {
                short[] rawAudio;
                if (!forceMock) {
                    if (microphone.available() >= bufferSize) {
                        rawAudio = microphone.read();
                    } else {
                        try {
                            Thread.sleep(captureIntervalMs / 2);
                        } catch (Exception e) {
                        }
                        continue;
                    }
                } else {
                    // Mock
                    rawAudio = new short[bufferSize];
                    boolean beep = (System.currentTimeMillis() / 1000) % 2 == 0;
                    if (beep) {
                        for (int i = 0; i < rawAudio.length; i++) {
                            rawAudio[i] = (short) (Math.sin((mockTime + i) * 2.0 * Math.PI * 440.0 / 48000.0)
                                    * 10000);
                        }
                    }
                    mockTime += rawAudio.length;
                    Thread.sleep(captureIntervalMs);
                }

                if (processor != null) {
                    // VAD Logic
                    if (!forceMock) {
                        double rms = processor.calculateRMS(rawAudio);
                        if (rms > vadThreshold) {
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

                    // Send via Manager/NetworkService
                    manager.sendAudioPacket(batchedData, localSequence);
                    localSequence += 2;
                }
            }
        } catch (Exception e) {
            Log.err("@ Capture Loop Error: @", TAG, e.getMessage());
        } finally {
            if (microphone != null && !forceMock)
                microphone.close();
        }
    }

    public void stopCapture() {
        if (microphone != null) {
            microphone.close();
            microphone = null;
        }

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }

    public synchronized void close() {
        stopCapture();
        if (speaker != null) {
            speaker.close();
            speaker = null;
        }
        if (mixer != null) {
            mixer.removePlayer("all");
            mixer = null;
        }
        processor = null;
    }

    public void updateMixerPosition(String id, float x, float y) {
        if (mixer != null)
            mixer.updatePosition(id, x, y);
    }

    public void updateListenerPosition(float x, float y) {
        if (mixer != null)
            mixer.updateListener(x, y);
    }

    public void queueAudio(String id, byte[] data, int sequence) {
        ensureInitialized(manager.isSpatialAudioEnabled());
        if (mixer == null)
            return;

        // Logic to decode and queue is inside ProcessIncomingVoice in VoiceChatManager
        // currently,
        // but decoding happens in Mixer usually or before?
        // In original code:
        // System.arraycopy(data, offset + 2, frameData, 0, frameLen);
        // mixer.queueAudio(...)

        // This method will accept RAW frames extracted from the packet by the
        // NetworkService/Manager
        mixer.queueAudio(id, data, sequence);
    }

    public void setSpatialEnabled(boolean enabled) {
        if (mixer != null)
            mixer.setSpatialEnabled(enabled);
    }

    public void setMasterVolume(float vol) {
        if (speaker != null)
            speaker.setVolume(vol);
    }

    public void setPlayerVolume(String id, float vol) {
        if (mixer != null)
            mixer.setVolume(id, vol);
    }

    public void removePlayer(String id) {
        if (mixer != null)
            mixer.removePlayer(id);
    }

    public boolean isMicAvailable() {
        return microphone != null && microphone.available() > 0;
    }
}
