package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desktop speaker implementation using javax.sound.
 * This class should only be loaded on Desktop platforms.
 */
public class DesktopSpeaker {

    private static final String TAG = "[DesktopSpk]";

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;
    private final java.util.concurrent.BlockingQueue<byte[]> audioQueue = new java.util.concurrent.LinkedBlockingQueue<>(20);
    private Thread playbackThread;
    private volatile boolean running = false;

    public DesktopSpeaker(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void open() {
        if (isOpen())
            return;

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();
            
            running = true;
            playbackThread = new Thread(this::playbackLoop, "VoiceChat-Playback");
            playbackThread.setDaemon(true);
            playbackThread.start();
            
            Log.info("@ Speaker opened", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    private void playbackLoop() {
        while (running) {
            try {
                byte[] data = audioQueue.take();
                if (speaker != null && speaker.isOpen()) {
                    speaker.write(data, 0, data.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Playback error: @", TAG, e.getMessage());
            }
        }
    }

    public void play(short[] audioData) {
        if (speaker == null || !speaker.isOpen())
            return;

        byte[] bytes = shortsToBytes(audioData);
        audioQueue.offer(bytes);
    }

    public boolean isOpen() {
        return speaker != null && speaker.isOpen();
    }

    public void close() {
        running = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        
        if (speaker == null)
            return;
        speaker.stop();
        speaker.flush();
        speaker.close();
        speaker = null;
        Log.info("@ Speaker closed", TAG);
    }

    private static byte[] shortsToBytes(short[] shorts) {
        ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            bb.putShort(s);
        }
        return bb.array();
    }
}
