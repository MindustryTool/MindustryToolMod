package mindustrytool.features.content.browser;

import java.util.HashMap;
import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.*;
import mindustry.io.JsonIO;

public class TagService {
    public enum TagCategoryEnum {
        schematics, maps
    }

    private Runnable onUpdate = () -> {
    };
    private static HashMap<String, Seq<TagCategory>> categories = new HashMap<>();

    public void getTag(TagCategoryEnum cat, Cons<Seq<TagCategory>> l) {
        Seq<TagCategory> item = categories.get(cat.name());
        if (item != null) {
            Core.app.post(() -> l.get(item));
            return;
        }
        fetch(cat, (tags) -> {
            categories.put(cat.name(), tags);
            Core.app.post(() -> l.get(tags));
        });
    }

    private void fetch(TagCategoryEnum cat, Cons<Seq<TagCategory>> l) {
        Http.get(Config.API_URL + "tags?group=" + cat)
                .error(e -> {
                    Log.err(Config.API_URL + "tags", e);
                    Core.app.post(() -> l.get(new Seq<>()));
                })
                .submit(r -> result(r, l));
    }

    @SuppressWarnings("unchecked")
    private void result(Http.HttpResponse r, Cons<Seq<TagCategory>> l) {
        String d = r.getResultAsString();
        Seq<TagCategory> tags = JsonIO.json.fromJson(Seq.class, TagCategory.class, d);
        Core.app.post(() -> {
            l.get(tags);
            onUpdate.run();
        });
    }

    public void onUpdate(Runnable cb) {
        onUpdate = cb;
    }

    public static String getCategoryName(String id) {
        if (id == null)
            return null;
        for (Seq<TagCategory> seq : categories.values()) {
            if (seq != null) {
                TagCategory cat = seq.find(c -> c.id() != null && c.id().equals(id));
                if (cat != null)
                    return cat.name();
            }
        }
        return null;
    }
}
