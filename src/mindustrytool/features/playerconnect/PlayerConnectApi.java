package mindustrytool.features.playerconnect;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.Config;

public class PlayerConnectApi {

    @SuppressWarnings("unchecked")
    public static void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> c) {
        Http.get(Config.API_v4_URL + "player-connect/rooms?q=" + q)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Core.app.post(
                            () -> c.get(JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, data)));
                });
    }

    @SuppressWarnings("unchecked")
    public static void findPlayerConnectProvider(
            Cons<Seq<PlayerConnectProvider>> providers,
            Cons<Throwable> onFailed//
    ) {
        Http.get(Config.API_v4_URL + "player-connect/providers")
                .error(onFailed)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Core.app.post(
                            () -> providers.get(JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, data)));
                });
    }
}
