package mindustrytool.services;

import java.util.concurrent.CompletableFuture;

import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.SchematicDetailData;

public class SchematicService {

    public CompletableFuture<byte[]> downloadSchematic(String id) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "schematics/" + id + "/data")
                .error(future::completeExceptionally)
                .submit(result -> {
                    future.complete(result.getResult());
                });

        return future;
    }

    public CompletableFuture<SchematicDetailData> findSchematicById(String id) {
        CompletableFuture<SchematicDetailData> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "schematics/" + id)
                .error(future::completeExceptionally)
                .submit(response -> {
                    try {
                        String data = response.getResultAsString();
                        future.complete(Utils.fromJson(SchematicDetailData.class, data));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }
}
