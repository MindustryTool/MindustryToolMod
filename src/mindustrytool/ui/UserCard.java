package mindustrytool.ui;

import arc.scene.ui.layout.Table;
import mindustrytool.dto.UserData;
import mindustrytool.services.UserService;

public class UserCard {

    public static void draw(Table parent, String id) {
        parent.pane(card -> {
            card.add("Loading...");
            UserService.findUserById(id, data -> draw(card, data));
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
