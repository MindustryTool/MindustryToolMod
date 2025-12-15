package mindustrytool.plugins.playerconnect;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import mindustry.io.JsonIO;
import java.net.URLEncoder;

/** Local API class for Player Connect HTTP requests. */
public final class Api {
    private static final String API_URL = "https://api.mindustry-tool.com/api/v4/";
    
    private Api() {}

    /** Fetches player connect rooms with optional search query. */
    public static void findPlayerConnectRooms(String q, Cons<Seq<PlayerConnectRoom>> ok) {
        String url = API_URL + "player-connect/rooms?q=" + encode(q);
        Http.get(url).submit(r -> {
            try {
                String d = r.getResultAsString();
                @SuppressWarnings("unchecked")
                Seq<PlayerConnectRoom> rooms = JsonIO.json.fromJson(Seq.class, PlayerConnectRoom.class, d);
                Core.app.post(() -> ok.get(rooms));
            } catch (Exception e) {
                Log.err("Failed to parse rooms", e);
                Core.app.post(() -> ok.get(new Seq<>()));
            }
        });
    }

    /** Fetches player connect providers. */
    public static void findPlayerConnectProvider(Cons<Seq<PlayerConnectProvider>> ok, Cons<Throwable> err) {
        String url = API_URL + "player-connect/providers";
        Http.get(url).error(e -> { Log.err(url, e); if (err != null) err.get(e); })
            .submit(r -> {
                try {
                    String d = r.getResultAsString();
                    @SuppressWarnings("unchecked")
                    Seq<PlayerConnectProvider> providers = JsonIO.json.fromJson(Seq.class, PlayerConnectProvider.class, d);
                    Core.app.post(() -> ok.get(providers));
                } catch (Exception e) {
                    Log.err("Failed to parse providers", e);
                    if (err != null) Core.app.post(() -> err.get(e));
                }
            });
    }

    private static String encode(String q) {
        try {
            return URLEncoder.encode(q, "UTF-8");
        } catch (Exception e) {
            return q;
        }
    }
}
