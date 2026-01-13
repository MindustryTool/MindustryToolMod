package mindustrytool.services;

import java.util.HashMap;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Http.HttpResponse;
import arc.util.Log;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.dto.TagCategory;

public class TagService {

    public enum TagCategoryEnum {
        schematics,
        maps
    }

    private Runnable onUpdate = () -> {
    };
    private static HashMap<String, Seq<TagCategory>> categories = new HashMap<>();

    public void getTag(TagCategoryEnum category, Cons<Seq<TagCategory>> listener) {
        var item = categories.get(category.name());

        if (item != null) {
            Core.app.post(() -> listener.get(item));
            return;
        }

        getTagData(category, (tags) -> {
            categories.put(category.name(), tags);
            Core.app.post(() -> listener.get(tags));
        });

    }

    private void getTagData(TagCategoryEnum category, Cons<Seq<TagCategory>> listener) {
        Http.get(Config.API_URL + "tags" + "?group=" + category)
                .error(error -> handleError(listener, error, Config.API_URL + "tags"))
                .submit(response -> handleResult(response, listener));
    }

    public void handleError(Cons<Seq<TagCategory>> listener, Throwable error, String url) {
        Log.err(url, error);
        Core.app.post(() -> listener.get(new Seq<>()));
    }

    @SuppressWarnings("unchecked")
    private void handleResult(HttpResponse response, Cons<Seq<TagCategory>> listener) {
        String data = response.getResultAsString();
        Seq<TagCategory> tags = JsonIO.json.fromJson(Seq.class, TagCategory.class, data);
        Core.app.post(() -> {
            listener.get(tags);
            onUpdate.run();
        });
    }

    public void onUpdate(Runnable callback) {
        onUpdate = callback;
    }
}
