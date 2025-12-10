package mindustrytool.data.api;

import arc.Core;
import arc.func.*;
import arc.struct.Seq;
import mindustry.io.JsonIO;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.*;
import java.net.URLEncoder;

public class Api {
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) { AuthHttp.get(Config.API_URL + "schematics/" + id + "/data").submit(r -> c.get(r.getResult())); }
    public static void downloadMap(String id, ConsT<byte[], Exception> c) { AuthHttp.get(Config.API_URL + "maps/" + id + "/data").submit(r -> c.get(r.getResult())); }
    public static void findSchematicById(String id, Cons<SchematicDetailData> c) { get(Config.API_URL + "schematics/" + id, SchematicDetailData.class, c); }
    public static void findMapById(String id, Cons<MapDetailData> c) { get(Config.API_URL + "maps/" + id, MapDetailData.class, c); }
    public static void findUserById(String id, Cons<UserData> c) { get(Config.API_URL + "users/" + id, UserData.class, c); }

    @SuppressWarnings("unchecked")
    public static void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> c) {
        String enc = q; try { enc = URLEncoder.encode(q, "UTF-8"); } catch (Exception ignored) {}
        AuthHttp.get(Config.API_v4_URL + "player-connect/rooms?q=" + enc).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, d)));
        });
    }

    @SuppressWarnings("unchecked")
    public static void findPlayerConnectProvider(Cons<Seq<PlayerConnectProvider>> p, Cons<Throwable> f) {
        AuthHttp.get(Config.API_v4_URL + "player-connect/providers").error(f).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> p.get(JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, d)));
        });
    }

    private static <T> void get(String url, Class<T> cls, Cons<T> cb) {
        AuthHttp.get(url).submit(r -> {
            if (r == null) return;
            String d = r.getResultAsString();
            if (d != null && !d.isEmpty()) Core.app.post(() -> cb.get(JsonIO.json.fromJson(cls, d)));
        });
    }
}
