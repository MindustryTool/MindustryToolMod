package mindustrytool.services;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.playerconnect.PlayerConnectRoom;
import mindustrytool.features.playerconnect.PlayerConnectProvider;

public class PlayerConnectService {

    private static final ConcurrentHashMap<String, PlayerConnectRoom> roomCache = new ConcurrentHashMap<>();

    public void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> cons) {
        Http.get(Config.API_v4_URL + "player-connect/rooms?q=" + q)
                .timeout(10000)
                .error(_err -> Core.app.post(() -> cons.get(new Seq<>())))
                .submit(response -> {
                    String data = response.getResultAsString();

                    Seq<PlayerConnectRoom> rooms = Seq.with(Utils.fromJsonArray(PlayerConnectRoom.class, data));

                    Core.app.post(() -> cons.get(rooms));
                });
    }

    public void findPlayerConnectProvider(
            Cons<Seq<PlayerConnectProvider>> cons,
            Cons<Throwable> onFailed) {

        Http.get(Config.API_v4_URL + "player-connect/providers")
                .timeout(10000)
                .error(onFailed)
                .submit(response -> {
                    String data = response.getResultAsString();
                    List<PlayerConnectProvider> providers = Utils.fromJsonArray(PlayerConnectProvider.class, data);

                    Core.app.post(() -> cons.get(Seq.with(providers)));
                });
    }

    public void getRoomWithCache(String link, Cons<PlayerConnectRoom> cons) {
        var cached = roomCache.get(link);

        if (cached != null) {
            Core.app.post(() -> cons.get(cached));
            return;
        }

        findPlayerConnectRooms("", rooms -> {
            PlayerConnectRoom found = rooms.find(r -> r.getLink().equals(link));

            if (found != null) {
                roomCache.put(link, found);
                Core.app.post(() -> cons.get(found));
            } else {
                Core.app.post(() -> cons.get(null));
            }
        });
    }

    public void invalidateRoom(String link) {
        roomCache.remove(link);
    }
}
