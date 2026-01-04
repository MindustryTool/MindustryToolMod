package mindustrytool.features.content.browser;

import java.util.*;
import arc.Core;
import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;

public final class UserCard {
    private static final Object lock = new Object();
    private static final ObjectMap<String, UserData> cache = new ObjectMap<>(32);
    private static final ObjectMap<String, List<Cons<UserData>>> listeners = new ObjectMap<>(16);

    private UserCard() {}

    public static void draw(Table parent, String id) {
        parent.pane(card -> {
            UserData user; 
            boolean load;
            synchronized (lock) {
                user = cache.get(id);
                load = user == null;
                if (user == null) cache.put(id, new UserData());
                if (user == null || user.id() == null) {
                    listeners.get(id, ArrayList::new).add(d -> Core.app.post(() -> draw(card, d)));
                }
            }
            if (load) Api.findUserById(id, data -> {
                List<Cons<UserData>> pending;
                synchronized (lock) { 
                    cache.put(id, data); 
                    pending = listeners.remove(id); 
                }
                if (pending != null) {
                    for (Cons<UserData> c : pending) c.get(data);
                }
            });
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
