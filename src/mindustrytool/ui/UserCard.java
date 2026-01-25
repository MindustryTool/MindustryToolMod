package mindustrytool.ui;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustrytool.dto.UserData;
import mindustrytool.services.UserService;

public class UserCard {

    public static void draw(Table parent, String id) {
        parent.pane(card -> {
            card.add("Loading...");
            UserService.findUserById(id).thenAccept(data -> Core.app.post(() -> draw(card, data)));
        })//
                .height(50);
    }

    public static void draw(Table card, UserData data) {
        card.clear();

        if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
            card.add(new NetworkImage(data.getImageUrl())).size(32);
        }

        card.add(data.getName());
    }
}
