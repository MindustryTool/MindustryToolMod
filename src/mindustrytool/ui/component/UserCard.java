package mindustrytool.ui.component;

import java.util.*;
import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustrytool.core.model.UserData;
import mindustrytool.data.api.Api;

public final class UserCard {
    private static final ObjectMap<String, UserData> cache = new ObjectMap<>(32);
    private static final ObjectMap<String, List<Cons<UserData>>> listeners = new ObjectMap<>(16);

    private UserCard() {}

    public static void draw(Table parent, String id) {
        parent.pane(card -> {
            UserData user = cache.get(id);
            if (user == null) {
                cache.put(id, new UserData());
                listeners.get(id, ArrayList::new).add(data -> draw(card, data));
                Api.findUserById(id, data -> {
                    cache.put(id, data);
                    List<Cons<UserData>> l = listeners.get(id);
                    if (l != null) { l.forEach(c -> c.get(data)); listeners.remove(id); }
                });
                card.add("Loading...");
                return;
            }
            if (user.id() == null) {
                listeners.get(id, ArrayList::new).add(data -> draw(card, data));
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
