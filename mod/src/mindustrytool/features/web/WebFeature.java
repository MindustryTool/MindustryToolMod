package mindustrytool.features.web;

import arc.Core;
import arc.scene.style.Drawable;
import arc.struct.Seq;
import lombok.Getter;
import mindustry.gen.Icon;
import mindustrytool.Config;

@Getter
public class WebFeature {
    private final String id;
    private final String nameKey;
    private final String descriptionKey;
    private final String url;
    private final Drawable icon;

    public WebFeature(String id, String nameKey, String descriptionKey, String url, Drawable icon) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.url = url;
        this.icon = icon;
    }

    public String getName() {
        return Core.bundle.has(nameKey) ? Core.bundle.get(nameKey) : id;
    }

    public String getDescription() {
        return Core.bundle.has(descriptionKey) ? Core.bundle.get(descriptionKey) : "";
    }

    public static final Seq<WebFeature> defaults = Seq.with(
            new WebFeature(
                    "content-patches",
                    "web-feature.content-patches.name",
                    "web-feature.content-patches.description",
                    Config.WEB_URL + "/content-patches?size=100",
                    Icon.box),
            new WebFeature(
                    "logic-editor",
                    "web-feature.logic-editor.name",
                    "web-feature.logic-editor.description",
                    Config.WEB_URL + "/tools/logic",
                    Icon.pencil),
            new WebFeature(
                    "logic-display-generator",
                    "web-feature.logic-display-generator.name",
                    "web-feature.logic-display-generator.description",
                    Config.WEB_URL + "/tools/logic-display-generator",
                    Icon.image),
            new WebFeature(
                    "sorter-image-generator",
                    "web-feature.sorter-image-generator.name",
                    "web-feature.sorter-image-generator.description",
                    Config.WEB_URL + "/tools/sorter-generator",
                    Icon.distribution),
            new WebFeature(
                    "canvas-image-generator",
                    "web-feature.canvas-image-generator.name",
                    "web-feature.canvas-image-generator.description",
                    Config.WEB_URL + "/tools/canvas-generator",
                    Icon.crafting),
            new WebFeature(
                    "wiki",
                    "web-feature.wiki.name",
                    "web-feature.wiki.description",
                    Config.WEB_URL + "/wiki",
                    Icon.book),
            new WebFeature(
                    "posts",
                    "web-feature.posts.name",
                    "web-feature.posts.description",
                    Config.WEB_URL + "/posts",
                    Icon.chat),
            new WebFeature(
                    "free-mindustry-server",
                    "web-feature.free-mindustry-server.name",
                    "web-feature.free-mindustry-server.description",
                    Config.WEB_URL + "/@me/servers",
                    Icon.host));
}
