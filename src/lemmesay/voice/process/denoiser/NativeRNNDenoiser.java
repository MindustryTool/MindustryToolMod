package lemmesay.voice.process.denoiser;

import de.maxhenkel.rnnoise4j.UnknownPlatformException;

import java.io.IOException;
import java.lang.ref.Cleaner;

public class NativeRNNDenoiser implements Denoiser, AutoCloseable{

    private static final Cleaner CLEANER = Cleaner.create();
    private final ResourceState state;
    private final Cleaner.Cleanable cleanable;

    public NativeRNNDenoiser() throws UnknownPlatformException, IOException{
        var model = new de.maxhenkel.rnnoise4j.Denoiser();
        this.state = new ResourceState(model);
        this.cleanable = CLEANER.register(this, state);
    }

    @Override
    public short[] denoise(short[] rawAudio){
        return this.state.denoiser.denoise(rawAudio);
    }

    @Override
    public float denoiseInPlace(short[] rawAudio){
        return this.state.denoiser.denoiseInPlace(rawAudio);
    }

    @Override
    public int getFrameSize(){
        return this.state.denoiser.getFrameSize();
    }

    @Override
    public float getSpeechProbability(short[] audio){
        return this.state.denoiser.getSpeechProbability(audio);
    }

    @Override
    public void close(){
        this.cleanable.clean();
    }

    @Override
    public boolean isClosed(){
        return state.denoiser.isClosed();
    }

    private static class ResourceState implements Runnable{

        de.maxhenkel.rnnoise4j.Denoiser denoiser;

        public ResourceState(de.maxhenkel.rnnoise4j.Denoiser denoiser){
            this.denoiser = denoiser;
        }

        @Override
        public void run() {
            this.denoiser.close();
        }
    }
}
