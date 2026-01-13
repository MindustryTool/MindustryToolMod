package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.func.ConsT;
import arc.util.Http;
import mindustry.io.JsonIO;
import mindustrytool.Config;
import mindustrytool.dto.MapDetailData;

public class MapService {

    public void downloadMap(String id, ConsT<byte[], Exception> c) {
        Http.get(Config.API_URL + "maps/" + id + "/data").submit(result -> {
            c.get(result.getResult());
        });
    }

    public void findMapById(String id, Cons<MapDetailData> c) {
        Http.get(Config.API_URL + "maps/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(JsonIO.json.fromJson(MapDetailData.class, data)));
        });
    }
}
