package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.dto.UserData;

public class UserService {

    public void findUserById(String id, Cons<UserData> c) {
        Http.get(Config.API_URL + "users/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(UserData.class, data)));
        });
    }
}
