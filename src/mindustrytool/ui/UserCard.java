package mindustrytool.ui;

import mindustrytool.features.browser.BrowserApi;
import java.util.ArrayList;
import java.util.List;

import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustrytool.dto.UserData;

public class UserCard {

    private static final ObjectMap<String, UserData> cache = new ObjectMap<String, UserData>();
    private static final ObjectMap<String, List<Cons<UserData>>> listeners = new ObjectMap<>();

    public static void draw(Table parent, String id) {
        parent.pane(card -> {

            UserData userData = cache.get(id);

            if (userData == null) {
                cache.put(id, new UserData());
                listeners.get(id, () -> new ArrayList<>()).add((data) -> draw(card, data));

                BrowserApi.findUserById(id, data -> {
                    cache.put(id, data);

                    var l = listeners.get(id);

                    if (l != null) {
                        for (Cons<UserData> listener : l) {
                            listener.get(data);
                        }
                        listeners.remove(id);
                    }
                });

                card.add("Loading...");
                return;
            }

            if (userData.id() == null) {
                listeners.get(id, () -> new ArrayList<>()).add((data) -> draw(card, data));
                card.add("Loading....");
                return;
            }

            draw(card, userData);
        })//
                .height(50);
    }

    private static void draw(Table card, UserData data) {
        card.clear();
        if (data.imageUrl() != null && !data.imageUrl().isEmpty()) {
            card.add(new NetworkImage(data.imageUrl())).size(24).padRight(4);
        }
        card.add(data.name());
    }
}
