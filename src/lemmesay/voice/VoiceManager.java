package lemmesay.voice;

import arc.util.Log;
import lemmesay.voice.microphone.JavaxMicrophone;
import mindustry.Vars;

public class VoiceManager implements Runnable {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;
    private static final int BUFFER_SIZE = 960;

    @Override
    public void run() {
        JavaxMicrophone mic = new JavaxMicrophone(SAMPLE_RATE, BUFFER_SIZE, "");
        mic.open();
        mic.start();
        Log.info("Microphone capture started for voice chat.");
        while (true) {
            if (!Vars.net.client()) {
                Log.info("Voice chat disconnected, stopping microphone capture.");
                mic.close();
                break;
            }
            short[] audioData = mic.read();
            // I will never write code like this again
            VoiceProcessing.getInstance().handleAudio(audioData);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mic.close();
                Log.info("Voice chat thread interrupted, stopping microphone capture.");
                break;
            }
        }
    }
}
