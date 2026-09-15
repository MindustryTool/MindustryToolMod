package old.mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Http.HttpResponse;
import old.mindustrytool.Config;
import old.mindustrytool.Utils;
import old.mindustrytool.dto.ModData;
import arc.util.Log;

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
        var mods = Utils.fromJsonArray(ModData.class, data);

        Core.app.post(() -> {
            listener.get(Seq.with(mods));
            onUpdate.run();
        });
    }

    public void onUpdate(Runnable callback) {
        onUpdate = callback;
    }

}
