package mindustrytool.features.savesync;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import arc.files.Fi;
import arc.util.Log;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthHttp;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.savesync.dto.*;

public class StorageService {
    private static final String BASE_URL = Config.API_v4_URL + "storage/";

    public static CompletableFuture<List<StorageSlotDto>> listSlots() {
        CompletableFuture<List<StorageSlotDto>> future = new CompletableFuture<>();
        AuthHttp.get(BASE_URL + "slots")
                .submit(res -> {
                    try {
                        future.complete(Utils.fromJsonArray(StorageSlotDto.class, res.getResultAsString()));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }, future::completeExceptionally);
        return future;
    }

    public static CompletableFuture<StorageSlotDto> createSlot(String name) {
        CompletableFuture<StorageSlotDto> future = new CompletableFuture<>();
        AuthHttp.post(BASE_URL + "slots", Utils.toJson(new CreateStorageSlotDto(name)))
                .header("Content-Type", "application/json")
                .submit(res -> {
                    try {
                        future.complete(Utils.fromJson(StorageSlotDto.class, res.getResultAsString()));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }, future::completeExceptionally);
        return future;
    }

    public static CompletableFuture<Void> deleteSlot(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AuthService.getInstance().refreshTokenIfNeeded().thenRun(() -> {
            AuthHttp.delete(BASE_URL + "slots/" + id)
                    .header("Authorization", "Bearer " + AuthService.getInstance().getAccessToken())
                    .error(future::completeExceptionally)
                    .submit(res -> future.complete(null));
        });
        return future;
    }

    public static CompletableFuture<Void> deleteFile(String slotId, String path) {
        Log.info("Delete: " + path);

        CompletableFuture<Void> future = new CompletableFuture<>();
        AuthService.getInstance().refreshTokenIfNeeded().thenRun(() -> {
            AuthHttp.delete(
                    BASE_URL + "slots/" + slotId + "/files?path=" + URLEncoder.encode(path))
                    .header("Authorization", "Bearer " + AuthService.getInstance().getAccessToken())
                    .error(future::completeExceptionally)
                    .submit(res -> future.complete(null));
        });
        return future;
    }

    public static CompletableFuture<SyncSlotResponseDto> syncSlot(String id, SyncSlotDto data) {
        CompletableFuture<SyncSlotResponseDto> future = new CompletableFuture<>();
        AuthHttp.post(BASE_URL + "slots/" + id + "/sync", Utils.toJson(data))
                .header("Content-Type", "application/json")
                .submit(res -> {
                    try {
                        future.complete(Utils.fromJson(SyncSlotResponseDto.class, res.getResultAsString()));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }, future::completeExceptionally);
        return future;
    }

    public static CompletableFuture<CheckHashesResponseDto> checkHashes(CheckHashesDto data) {
        CompletableFuture<CheckHashesResponseDto> future = new CompletableFuture<>();
        AuthHttp.post(BASE_URL + "check-hashes", Utils.toJson(data))
                .header("Content-Type", "application/json")
                .submit(res -> {
                    try {
                        future.complete(Utils.fromJson(CheckHashesResponseDto.class, res.getResultAsString()));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }, future::completeExceptionally);
        return future;
    }

    public static CompletableFuture<Void> uploadFile(Fi file) {
        Log.info("Upload: " + file.path());

        return AuthService.getInstance().refreshTokenIfNeeded().thenCompose(v -> CompletableFuture.runAsync(() -> {
            String boundary = "---" + System.currentTimeMillis();
            String LINE_FEED = "\r\n";
            HttpURLConnection httpConn = null;
            try {
                URL url = new URL(BASE_URL + "upload");
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setDoInput(true);
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                String token = AuthService.getInstance().getAccessToken();

                if (token == null) {
                    throw new RuntimeException("No access token available");
                }

                httpConn.setRequestProperty("Authorization", "Bearer " + token);

                OutputStream outputStream = httpConn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                        true);

                String hash = Utils.sha256(file.file());
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"hash\"").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(hash).append(LINE_FEED);

                String fileName = file.name();
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"")
                        .append(LINE_FEED);
                writer.append("Content-Type: application/octet-stream").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                FileInputStream inputStream = new FileInputStream(file.file());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();

                writer.append(LINE_FEED);
                writer.flush();
                writer.append("--" + boundary + "--").append(LINE_FEED);
                writer.close();

                int status = httpConn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_CREATED) {
                    // Read error stream
                    InputStream errorStream = httpConn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        throw new IOException("Server returned non-OK status: " + status + " " + response.toString());
                    }
                    throw new IOException("Server returned non-OK status: " + status);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (httpConn != null)
                    httpConn.disconnect();
            }
        }));
    }
}
