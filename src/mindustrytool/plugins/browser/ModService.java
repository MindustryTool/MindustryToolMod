package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.*;
import mindustry.io.JsonIO;

public class ModService {
    private Runnable onUpdate = () -> {
    };
    private static Seq<ModData> mods = new Seq<>();

    public void getMod(Cons<Seq<ModData>> l) {
        if (mods.isEmpty()) {
            fetch(d -> {
                mods = d;
                Core.app.post(() -> l.get(mods));
            });
        } else {
            Core.app.post(() -> l.get(mods));
        }
    }

    private void fetch(Cons<Seq<ModData>> l) {
        Http.get(Config.API_URL + "planets")
                .error(e -> {
                    Log.err(Config.API_URL + "planets", e);
                    Core.app.post(() -> l.get(new Seq<>()));
                })
                .submit(r -> result(r, l));
    }

    @SuppressWarnings("unchecked")
    private void result(Http.HttpResponse r, Cons<Seq<ModData>> l) {
        String d = r.getResultAsString();
        Seq<ModData> m = JsonIO.json.fromJson(Seq.class, ModData.class, d);
        Core.app.post(() -> {
            l.get(m);
            onUpdate.run();
        });
    }

    public void onUpdate(Runnable cb) {
        onUpdate = cb;
    }
}
