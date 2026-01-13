package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Http.HttpResponse;
import arc.util.Log;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.dto.ModData;

public class ModService {

    private Runnable onUpdate = () -> {
    };
    private static Seq<ModData> mods = new Seq<>();

    public void getMod(Cons<Seq<ModData>> listener) {
        if (mods.isEmpty()) {
            getModData((modsData) -> {
                mods = modsData;
                Core.app.post(() -> listener.get(mods));
            });
        } else {
            Core.app.post(() -> listener.get(mods));
        }
    }

    private void getModData(Cons<Seq<ModData>> listener) {
        Http.get(Config.API_URL + "planets")
                .error(error -> handleError(listener, error, Config.API_URL + "planets"))
                .submit(response -> handleResult(response, listener));
    }

    public void handleError(Cons<Seq<ModData>> listener, Throwable error, String url) {
        Log.err(url, error);
        Core.app.post(() -> listener.get(new Seq<>()));
    }

    private void handleResult(HttpResponse response, Cons<Seq<ModData>> listener) {
        String data = response.getResultAsString();
        @SuppressWarnings("unchecked")
        Seq<ModData> mods = JsonIO.json.fromJson(Seq.class, ModData.class, data);

        Core.app.post(() -> {
            listener.get(mods);
            onUpdate.run();
        });
    }

    public void onUpdate(Runnable callback) {
        onUpdate = callback;
    }

}
