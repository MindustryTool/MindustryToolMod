package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.features.playerconnect.PlayerConnectRoom;
import mindustrytool.features.playerconnect.PlayerConnectProvider;

public class PlayerConnectService {

    @SuppressWarnings("unchecked")
    public void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> c) {
        Http.get(Config.API_v4_URL + "player-connect/rooms?q=" + q)
                .timeout(5000)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Core.app.post(
                            () -> c.get(JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, data)));
                });
    }

    @SuppressWarnings("unchecked")
    public void findPlayerConnectProvider(
            Cons<Seq<PlayerConnectProvider>> providers,
            Cons<Throwable> onFailed) {

        Http.get(Config.API_v4_URL + "player-connect/providers")
                .timeout(5000)
                .error(onFailed)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Core.app.post(
                            () -> providers.get(JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, data)));
                });
    }
}
