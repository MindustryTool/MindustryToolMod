package old.mindustrytool.services;

import java.util.concurrent.CompletableFuture;

import arc.Core;
import arc.util.Http;
import old.mindustrytool.Config;
import old.mindustrytool.Utils;
import old.mindustrytool.dto.MapDetailData;

public class MapService {

    public static CompletableFuture<byte[]> downloadMap(String itemId) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "maps/" + itemId + "/data")
                .timeout(10000)
                .error(future::completeExceptionally)
                .submit(result -> {
                    future.complete(result.getResult());
                });

        return future;
    }

    public static CompletableFuture<MapDetailData> findMapById(String itemId) {
        return findMapByItemId(itemId);
    }

    public static CompletableFuture<MapDetailData> findMapByItemId(String itemId) {
        CompletableFuture<MapDetailData> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "maps/" + itemId)
                .error(future::completeExceptionally)
                .timeout(10000)
                .submit(response -> {
                    String data = response.getResultAsString();
                    Core.app.post(() -> {
                        try {
                            future.complete(Utils.fromJson(MapDetailData.class, data));
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                });

        return future;

    }
}
