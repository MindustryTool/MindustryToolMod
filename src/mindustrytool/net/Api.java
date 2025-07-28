package mindustrytool.net;

import arc.Core;
import arc.func.Cons;
import arc.func.ConsT;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.config.Config;
import mindustrytool.data.MapDetailData;
import mindustrytool.data.SchematicDetailData;
import mindustrytool.data.UserData;

public class Api {

    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) {
        Http.get(Config.API_URL + "schematics/" + id + "/download").submit(result -> {
            c.get(result.getResult());
        });
    }

    public static void downloadMap(String id, ConsT<byte[], Exception> c) {
        Http.get(Config.API_URL + "maps/" + id + "/download").submit(result -> {
            c.get(result.getResult());
        });
    }

    public static void findSchematicById(String id, Cons<SchematicDetailData> c) {
        Http.get(Config.API_URL + "schematics/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(SchematicDetailData.class, data)));
        });
    }

    public static void findMapById(String id, Cons<MapDetailData> c) {
        Http.get(Config.API_URL + "maps/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(MapDetailData.class, data)));
        });
    }
    public static void findUSerById(String id, Cons<UserData> c) {
        Http.get(Config.API_URL + "users/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(UserData.class, data)));
        });
    }
}
