package mindustrytool.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.Timer;
import arc.util.Http.HttpStatusException;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.UserData;

public class UserService {

    private static final ConcurrentHashMap<String, UserData> cache = new ConcurrentHashMap<String, UserData>();
    private static final ConcurrentHashMap<String, Seq<CompletableFuture<UserData>>> listeners = new ConcurrentHashMap<>();

    static {
        Timer.schedule(UserService::batch, 0, 0.2f);
    }

    private static synchronized void batch() {
        if (listeners.isEmpty()) {
            return;
        }

        List<String> ids = new ArrayList<>();
        var copy = new ConcurrentHashMap<>(listeners);
        listeners.clear();

        copy.forEach((id, callbacks) -> {
            ids.add(id);
        });

        HashMap<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        Http.post(Config.API_URL + "users/batches", Utils.toJson(body))
                .header("Content-Type", "application/json")
                .timeout(10000)
                .error(error -> {
                    if (error instanceof HttpStatusException http) {
                        copy.forEach((id, callbacks) -> {
                            for (var callback : callbacks) {
                                callback.completeExceptionally(
                                        new Exception("Failed to get user: " + http.response.getResultAsString()));
                            }
                        });
                    } else {
                        copy.forEach((id, callbacks) -> {
                            for (var callback : callbacks) {
                                callback.completeExceptionally(error);
                            }
                        });
                    }
                })
                .submit(res -> {
                    String data = res.getResultAsString();
                    List<UserData> users = Utils.fromJsonArray(UserData.class, data);

                    copy.forEach((id, callbacks) -> {
                        var user = users.stream().filter(u -> u.getId().equals(id)).findFirst();
                        if (user.isPresent()) {
                            for (var callback : callbacks) {
                                callback.complete(user.get());
                            }
                        } else {
                            for (var callback : callbacks) {
                                callback.completeExceptionally(new Exception("User not found"));
                            }
                        }
                    });
                });
    }

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
        } else {
            current.add(future);
        }

        return future;
    }

}
