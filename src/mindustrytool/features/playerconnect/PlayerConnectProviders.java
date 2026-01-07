package mindustrytool.features.playerconnect;

import arc.Core;
import arc.struct.ArrayMap;
import mindustrytool.services.PlayerConnectService;

public class PlayerConnectProviders {
    public static final String PUBLIC_PROVIDER_URL = "";
    public static final String PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY = "player-connect-providers";
    public static final ArrayMap<String, String> online = new ArrayMap<>(),
            custom = new ArrayMap<>();
    private static final PlayerConnectService playerConnectService = new PlayerConnectService();

    public static synchronized void refreshOnline(Runnable onCompleted, arc.func.Cons<Throwable> onFailed) {
        playerConnectService.findPlayerConnectProvider(providers -> {
            online.clear();
            for (var provider : providers) {
                online.put(provider.name(), provider.address());
            }
            onCompleted.run();
        }, onFailed);
    }

    @SuppressWarnings("unchecked")
    public static void loadCustom() {
        custom.clear();
        custom.putAll(Core.settings.getJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, ArrayMap.class, String.class,
                ArrayMap::new));
    }

    public static void saveCustom() {
        Core.settings.putJson(PLAYER_CONNECT_PROVIDER_PERSISTENT_KEY, String.class, custom);
    }
}
