package mindustrytool.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import arc.struct.Seq;
import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.playerconnect.PlayerConnectRoom;
import mindustrytool.features.playerconnect.PlayerConnectProvider;

public class PlayerConnectService {
    private static final ConcurrentHashMap<String, PlayerConnectRoom> roomCache = new ConcurrentHashMap<>();

    public CompletableFuture<Seq<PlayerConnectRoom>> findPlayerConnectRooms(String q) {
        CompletableFuture<Seq<PlayerConnectRoom>> future = new CompletableFuture<>();

        Http.get(Config.API_v4_URL + "player-connect/rooms?q=" + q)
                .timeout(60000)
                .error(future::completeExceptionally)
                .submit(response -> {
                    try {
                        String data = response.getResultAsString();
                        Seq<PlayerConnectRoom> rooms = Seq.with(Utils.fromJsonArray(PlayerConnectRoom.class, data));
                        future.complete(rooms);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    public CompletableFuture<Seq<PlayerConnectProvider>> findPlayerConnectProvider() {
        CompletableFuture<Seq<PlayerConnectProvider>> future = new CompletableFuture<>();

        Http.get(Config.API_v4_URL + "player-connect/providers")
                .timeout(60000)
                .error(future::completeExceptionally)
                .submit(response -> {
                    try {
                        String data = response.getResultAsString();
                        List<PlayerConnectProvider> providers = Utils.fromJsonArray(PlayerConnectProvider.class, data);
                        future.complete(Seq.with(providers));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    public CompletableFuture<PlayerConnectRoom> getRoomWithCache(String link) {
        CompletableFuture<PlayerConnectRoom> future = new CompletableFuture<>();
        var cached = roomCache.get(link);

        if (cached != null) {
            future.complete(cached);
            return future;
        }

        findPlayerConnectRooms("").thenAccept(rooms -> {
            PlayerConnectRoom found = rooms.find(r -> r.getLink().equals(link));

            if (found != null) {
                roomCache.put(link, found);
                future.complete(found);
            } else {
                future.complete(null);
            }
        });

        return future;
    }

    public void invalidateRoom(String link) {
        roomCache.remove(link);
    }
}
