package lemmesay.voice.microphone;

import arc.util.Log;
import arc.util.Nullable;
import lemmesay.utils.AudioUtils;
import javax.sound.sampled.*;

public class JavaxMicrophone implements Microphone{

    private int sampleRate;
    private int bufferSize;
    @Nullable
    private String deviceName;
    @Nullable
    private TargetDataLine mic;

    public JavaxMicrophone(int sampleRate, int bufferSize, @Nullable String deviceName) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.deviceName = deviceName;
    }

    @Override
    public void open() throws IllegalStateException {
        if (this.isOpen()){
            throw new IllegalStateException("Microphone is already open");
        }
        var format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 1, 2, sampleRate, false);

        mic = findMicrophoneByName(this.deviceName, format);
        if (mic == null){
            Log.warn("Could not found microphone with name: " + this.deviceName + ", fallback to default microphone.");
            mic = getDefaultMicrophone(format);
        }
        if (mic == null){
            throw new IllegalStateException("No microphone found on this system with specified audio format.");
        }
        try {
            mic.open(format);
            var micName = mic.getLineInfo().toString();
            Log.info("Microphone opened: " + micName);
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Failed to open microphone line.", e);
        }
        // This fixes the accumulating audio issue on some Linux systems (Based on MC SimpleVoiceChat Mod).
        mic.start();
        mic.stop();
        mic.flush();
    }

    @Override
    public void start() {
        if (!isOpen() || mic == null) {
            return;
        }
        mic.start();
    }

    @Override
    public void stop() {
        if (!isOpen() || mic == null) {
            return;
        }
        mic.stop();
        mic.flush();
    }

    @Override
    public void close() {
        if (mic == null) {
            return;
        }
        mic.stop();
        mic.flush();
        mic.close();
    }

    @Override
    public boolean isOpen() {
        if (mic == null){
            return false;
        }
        return mic.isOpen();
    }

    @Override
    public boolean isStarted() {
        if (mic == null){
            return false;
        }
        return mic.isActive();
    }

    @Override
    public int available() {
        if (mic == null){
            return 0;
        }
        return mic.available() / 2;
    }

    @Override
    public short[] read() {
        if (mic == null){
            throw new IllegalStateException("Microphone is not opened.");
        }
        var available = available();
        if (bufferSize > available){
            throw new IllegalStateException("Failed to read from microphone, not enough data available. Required: " + bufferSize + ", available: " + available);
        }
        byte[] byteBuffer = new byte[bufferSize * 2];
        mic.read(byteBuffer, 0, byteBuffer.length);
        return AudioUtils.bytes2shorts(byteBuffer);
    }

    private static TargetDataLine findMicrophoneByName(String name, AudioFormat format) {
        var mixers = AudioSystem.getMixerInfo();
        for (var mixerInfo : mixers) {
            var mixer = AudioSystem.getMixer(mixerInfo);
            var lineInfo = new DataLine.Info(TargetDataLine.class, format);
            if (mixer.isLineSupported(lineInfo) && mixerInfo.getName().contains(name)) {
                try {
                    return (TargetDataLine) mixer.getLine(lineInfo);
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    private static TargetDataLine getDefaultMicrophone(AudioFormat format) {
        try {
            var lineInfo = new DataLine.Info(TargetDataLine.class, format);
            return (TargetDataLine) AudioSystem.getLine(lineInfo);
        } catch (Exception ignore) {
        }
        return null;
    }
}
