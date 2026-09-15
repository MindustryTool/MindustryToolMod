package old.mindustrytool.services;

import java.util.concurrent.CompletableFuture;

import arc.util.Http;
import old.mindustrytool.Config;
import old.mindustrytool.Utils;
import old.mindustrytool.dto.SchematicDetailData;

public class SchematicService {

    public static CompletableFuture<byte[]> downloadSchematic(String itemId) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "schematics/" + itemId + "/data")
                .error(future::completeExceptionally)
                .submit(result -> {
                    future.complete(result.getResult());
                });

        return future;
    }

    public static CompletableFuture<SchematicDetailData> findSchematicById(String itemId) {
        return findSchematicByItemId(itemId);
    }

    public static CompletableFuture<SchematicDetailData> findSchematicByItemId(String itemId) {
        CompletableFuture<SchematicDetailData> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "schematics/" + itemId)
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
