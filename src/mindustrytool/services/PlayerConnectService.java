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
    public void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> cons) {
        Http.get(Config.API_v4_URL + "player-connect/rooms?q=" + q)
                .timeout(5000)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Seq<PlayerConnectRoom> rooms = JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, data);

                    if (rooms == null) {
                        throw new IllegalArgumentException("Player connect room data is null");
                    }

                    Core.app.post(() -> cons.get(rooms));
                });
    }

    @SuppressWarnings("unchecked")
    public void findPlayerConnectProvider(
            Cons<Seq<PlayerConnectProvider>> cons,
            Cons<Throwable> onFailed) {

        Http.get(Config.API_v4_URL + "player-connect/providers")
                .timeout(5000)
                .error(onFailed)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Seq<PlayerConnectProvider> providers = JsonIO.json.fromJson(Seq.class,
                            PlayerConnectProvider.class, data);

                    if (providers == null) {
                        throw new IllegalArgumentException("Player connect provider data is null");
                    }

                    Core.app.post(() -> cons.get(providers));
                });
    }
}
