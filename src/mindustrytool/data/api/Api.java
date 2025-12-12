package mindustrytool.data.api;

import arc.func.*;
import arc.struct.Seq;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.*;
import java.net.URLEncoder;

public class Api {
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) { AuthHttp.get(Config.API_URL + "schematics/" + id + "/data").submit(r -> c.get(r.getResult())); }
    public static void downloadMap(String id, ConsT<byte[], Exception> c) { AuthHttp.get(Config.API_URL + "maps/" + id + "/data").submit(r -> c.get(r.getResult())); }
    public static void findSchematicById(String id, Cons<SchematicDetailData> c) { ApiRequest.get(Config.API_URL + "schematics/" + id, SchematicDetailData.class, c); }
    public static void findMapById(String id, Cons<MapDetailData> c) { ApiRequest.get(Config.API_URL + "maps/" + id, MapDetailData.class, c); }
    public static void findUserById(String id, Cons<UserData> c) { ApiRequest.get(Config.API_URL + "users/" + id, UserData.class, c); }
    public static void getSession(Cons<SessionData> ok, Cons<Throwable> err) { ApiRequest.getWithError(Config.API_v4_URL + "auth/session", SessionData.class, ok, err); }
    public static void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> c) { ApiRequest.getList(Config.API_v4_URL + "player-connect/rooms?q=" + encode(q), PlayerConnectRoom.class, c); }
    public static void findPlayerConnectProvider(Cons<Seq<PlayerConnectProvider>> ok, Cons<Throwable> err) { ApiRequest.getList(Config.API_v4_URL + "player-connect/providers", PlayerConnectProvider.class, ok, err); }
    private static String encode(String q) { try { return URLEncoder.encode(q, "UTF-8"); } catch (Exception e) { return q; } }
}
