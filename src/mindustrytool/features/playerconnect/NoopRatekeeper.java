package mindustrytool.features.playerconnect;

import arc.util.Ratekeeper;

public class NoopRatekeeper extends Ratekeeper {
    @Override
    public boolean allow(long spacing, int cap) {
        return true;
    }
}
