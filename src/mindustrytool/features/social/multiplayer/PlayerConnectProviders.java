package mindustrytool.features.social.multiplayer;

import arc.Core;
import arc.struct.ArrayMap;

/** Manages Player Connect providers (servers). */
public class PlayerConnectProviders {
    public static final String PUBLIC_PROVIDER_URL = "";
    public static final String PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY = "player-connect-providers";
    public static final ArrayMap<String, String> online = new ArrayMap<>(),
            custom = new ArrayMap<>();

    public static synchronized void refreshOnline(Runnable onCompleted, arc.func.Cons<Throwable> onFailed) {
        Api.findPlayerConnectProvider(providers -> {
            online.clear();
            for (PlayerConnectProvider provider : providers) {
                online.put(provider.name(), provider.address());
            }
            onCompleted.run();
        }, onFailed);
    }

    @SuppressWarnings("unchecked")
    public static void loadCustom() {
        custom.clear();
        ArrayMap<String, String> saved = Core.settings.getJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, ArrayMap.class, ArrayMap::new);
        if (saved != null) custom.putAll(saved);
    }

    public static void saveCustom() {
        Core.settings.putJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, String.class, custom);
    }
}
