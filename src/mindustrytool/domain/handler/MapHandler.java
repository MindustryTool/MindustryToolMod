package mindustrytool.domain.handler;

import arc.files.Fi;
import mindustry.Vars;
import mindustrytool.core.model.ContentData;
import mindustrytool.data.api.Api;
import static mindustry.Vars.ui;

public final class MapHandler {
    private MapHandler() {}

    public static void Download(ContentData map) {
        Api.downloadMap(map.id(), result -> {
            Fi file = Vars.customMapDirectory.child(map.id());
            file.writeBytes(result);
            Vars.maps.importMap(file);
            ui.showInfoFade("@map.saved");
        });
    }
}
