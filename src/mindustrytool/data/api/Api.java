package mindustrytool.data.api;

import arc.Core;
import arc.func.*;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Http.HttpRequest;
import mindustry.io.JsonIO;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.*;
import mindustrytool.domain.service.AuthService;
import java.net.URLEncoder;

public class Api {
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) { 
        authGet(Config.API_URL + "schematics/" + id + "/data").submit(r -> c.get(r.getResult())); 
    }
    public static void downloadMap(String id, ConsT<byte[], Exception> c) { 
        authGet(Config.API_URL + "maps/" + id + "/data").submit(r -> c.get(r.getResult())); 
    }
    public static void findSchematicById(String id, Cons<SchematicDetailData> c) { get(Config.API_URL + "schematics/" + id, SchematicDetailData.class, c); }
    public static void findMapById(String id, Cons<MapDetailData> c) { get(Config.API_URL + "maps/" + id, MapDetailData.class, c); }
    public static void findUserById(String id, Cons<UserData> c) { get(Config.API_URL + "users/" + id, UserData.class, c); }
    
    @SuppressWarnings("unchecked")
    public static void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> c) {
        String encodedQ;
        try {
            encodedQ = URLEncoder.encode(q, "UTF-8");
        } catch (Exception e) {
            encodedQ = q; // fallback
        }
        authGet(Config.API_v4_URL + "player-connect/rooms?q=" + encodedQ).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, d)));
        });
    }
    
    @SuppressWarnings("unchecked")
    public static void findPlayerConnectProvider(Cons<Seq<PlayerConnectProvider>> p, Cons<Throwable> f) {
        authGet(Config.API_v4_URL + "player-connect/providers").error(f).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> p.get(JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, d)));
        });
    }
    
    private static <T> void get(String url, Class<T> cls, Cons<T> cb) {
        authGet(url).submit(r -> {
            if (r == null) return;
            String d = r.getResultAsString();
            if (d != null && !d.isEmpty()) Core.app.post(() -> cb.get(JsonIO.json.fromJson(cls, d)));
        });
    }
    
    /** Create HTTP GET request with auth header if logged in */
    private static HttpRequest authGet(String url) {
        HttpRequest req = Http.get(url);
        String token = AuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return req;
    }
    
    /** Create HTTP POST request with auth header if logged in */
    public static HttpRequest authPost(String url, String body) {
        HttpRequest req = Http.post(url, body);
        String token = AuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null && !body.isEmpty()) req.header("Content-Type", "application/json");
        return req;
    }
}
