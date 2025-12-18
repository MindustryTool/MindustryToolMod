package mindustrytool.plugins.playerconnect;

import arc.util.Ratekeeper;

/** A rate keeper that always allows operations. Used for proxy connections. */
public class NoopRatekeeper extends Ratekeeper {
    @Override
    public boolean allow(long spacing, int cap) {
        return true;
    }
}
