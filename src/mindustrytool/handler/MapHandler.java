package mindustrytool.handler;

import arc.files.Fi;
import mindustry.Vars;
import mindustrytool.data.ContentData;
import mindustrytool.net.Api;

import static mindustry.Vars.ui;

public class MapHandler {

    public static void downloadMap(ContentData map) {
        Api.downloadMap(map.id(), result -> {
            Fi mapFile = Vars.customMapDirectory.child(map.id().toString());
            mapFile.writeBytes(result);
            Vars.maps.importMap(mapFile);
            ui.showInfoFade("@map.saved");
        });
    }
}