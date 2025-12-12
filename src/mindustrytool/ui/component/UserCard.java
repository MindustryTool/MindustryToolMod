package mindustrytool.ui.component;

import java.util.*;
import arc.Core;
import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustrytool.core.model.UserData;
import mindustrytool.data.api.Api;

public final class UserCard {
    private static final Object lock = new Object();
    private static final ObjectMap<String, UserData> cache = new ObjectMap<>(32);
    private static final ObjectMap<String, List<Cons<UserData>>> listeners = new ObjectMap<>(16);

    private UserCard() {}

    public static void draw(Table parent, String id) {
        parent.pane(card -> {
            UserData user;
            boolean needsLoad = false;
            
            synchronized (lock) {
                user = cache.get(id);
                if (user == null) {
                    cache.put(id, new UserData());
                    listeners.get(id, ArrayList::new).add(data -> Core.app.post(() -> draw(card, data)));
                    needsLoad = true;
                } else if (user.id() == null) {
                    listeners.get(id, ArrayList::new).add(data -> Core.app.post(() -> draw(card, data)));
                }
            }
            
            if (needsLoad) {
                Api.findUserById(id, data -> {
                    List<Cons<UserData>> pending;
                    synchronized (lock) {
                        cache.put(id, data);
                        pending = listeners.remove(id);
                    }
                    if (pending != null) {
                        for (Cons<UserData> c : pending) c.get(data);
                    }
                });
                card.add("Loading...");
                return;
            }
            
            if (user == null || user.id() == null) {
                card.add("Loading...");
                return;
            }
            draw(card, user);
        }).height(50);
    }

    private static void draw(Table card, UserData data) {
        card.clear();
        if (data.imageUrl() != null && !data.imageUrl().isEmpty()) {
            card.add(new NetworkImage(data.imageUrl())).size(24).padRight(4);
        }
        card.add(data.name());
    }
}
