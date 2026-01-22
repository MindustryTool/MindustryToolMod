package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.dto.UserData;

public class UserService {

    private static final ObjectMap<String, UserData> cache = new ObjectMap<String, UserData>();
    private static final ObjectMap<String, Seq<Cons<UserData>>> listeners = new ObjectMap<>();

    public static void findUserById(String id, Cons<UserData> c) {

        UserData cached = cache.get(id);

        if (cached != null) {
            c.get(cached);
            return;
        }

        var current = listeners.get(id);

        if (current == null) {
            final Seq<Cons<UserData>> callbacks = Seq.withArrays(c);
            listeners.put(id, callbacks);

            Http.get(Config.API_URL + "users/" + id)
                    .timeout(5000)
                    .submit(response -> {
                        String data = response.getResultAsString();
                        UserData userData = JsonIO.json.fromJson(UserData.class, data);

                        if (userData == null) {
                            throw new IllegalArgumentException("User data is null");
                        }

                        cache.put(id, userData);

                        Core.app.post(() -> {
                            callbacks.each(listener -> listener.get(userData));
                            listeners.remove(id);
                        });
                    });
        } else {
            current.add(c);
        }

    }
}
