package mindustrytool.services;

import arc.Core;
import arc.func.Cons;
import arc.func.ConsT;
import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.SchematicDetailData;

public class SchematicService {

    public void downloadSchematic(String id, ConsT<byte[], Exception> c) {
        Http.get(Config.API_URL + "schematics/" + id + "/data").submit(result -> {
            c.get(result.getResult());
        });
    }

    public void findSchematicById(String id, Cons<SchematicDetailData> c) {
        Http.get(Config.API_URL + "schematics/" + id).submit(response -> {
            String data = response.getResultAsString();
            Core.app.post(() -> c.get(Utils.fromJson(SchematicDetailData.class, data)));
        });
    }
}
