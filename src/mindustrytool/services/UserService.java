package mindustrytool.services;

import java.util.concurrent.CompletableFuture;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.UserData;

public class UserService {

    private static final ObjectMap<String, UserData> cache = new ObjectMap<String, UserData>();
    private static final ObjectMap<String, Seq<CompletableFuture<UserData>>> listeners = new ObjectMap<>();

    public static synchronized CompletableFuture<UserData> findUserById(String id) {
        CompletableFuture<UserData> future = new CompletableFuture<>();

        UserData cached = cache.get(id);

        if (cached != null) {
            future.complete(cached);
            return future;
        }

        var current = listeners.get(id);

        if (current == null) {
            final Seq<CompletableFuture<UserData>> callbacks = Seq.withArrays(future);

            listeners.put(id, callbacks);

            Http.get(Config.API_URL + "users/" + id)
                    .timeout(10000)
                    .submit(response -> {
                        String data = response.getResultAsString();
                        UserData userData = Utils.fromJson(UserData.class, data);

                        if (userData == null) {
                            throw new IllegalArgumentException("User data is null");
                        }

                        cache.put(id, userData);

                        callbacks.each(listener -> listener.complete(userData));
                        listeners.remove(id);
                    });
        } else {
            current.add(future);
        }

        return future;
    }
}
