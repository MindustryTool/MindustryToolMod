package mindustrytool.services;

import java.util.concurrent.CompletableFuture;

import arc.Core;
import arc.util.Http;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.MapDetailData;

public class MapService {

    public static CompletableFuture<byte[]> downloadMap(String id) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "maps/" + id + "/data")
                .error(future::completeExceptionally)
                .timeout(10000)
                .submit(result -> {
                    future.complete(result.getResult());
                });

        return future;
    }

    public static CompletableFuture<MapDetailData> findMapById(String id) {
        CompletableFuture<MapDetailData> future = new CompletableFuture<>();

        Http.get(Config.API_URL + "maps/" + id)
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
