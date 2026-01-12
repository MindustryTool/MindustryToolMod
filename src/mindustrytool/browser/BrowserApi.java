package mindustrytool.browser; // Khai báo package chứa các class browser chính

import arc.Core; // Import Core để post lên main thread
import arc.func.Cons; // Import interface callback function
import arc.func.ConsT; // Import interface callback có thể throw Exception
import arc.util.Http; // Import Http để gọi HTTP requests
import mindustry.io.JsonIO; // Import JsonIO để parse JSON response
import mindustrytool.browser.data.DataMap; // Import data class Map
import mindustrytool.browser.data.DataUser; // Import data class User
import mindustrytool.browser.data.DataSchematic; // Import data class chi tiết Schematic

/**
 * Wrapper đơn giản cho các HTTP API calls của browser.
 * Các method post kết quả về main thread khi cần thiết.
 */
public class BrowserApi { // Class chứa các static method gọi API

    /**
     * Download binary data của schematic theo id.
     * @param id ID của schematic trên server
     * @param c Callback nhận byte array data
     */
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) {
        // Gọi HTTP GET tới endpoint /schematics/{id}/data
        Http.get(BrowserConfig.API_URL + "schematics/" + id + "/data").submit(result -> {
            c.get(result.getResult()); // Trả về raw bytes cho callback
        });
    }

    /**
     * Download binary data của map theo id.
     * @param id ID của map trên server
     * @param c Callback nhận byte array data
     */
    public static void downloadMap(String id, ConsT<byte[], Exception> c) {
        // Gọi HTTP GET tới endpoint /maps/{id}/data
        Http.get(BrowserConfig.API_URL + "maps/" + id + "/data").submit(result -> {
            c.get(result.getResult()); // Trả về raw bytes cho callback
        });
    }

    /**
     * Lấy thông tin chi tiết schematic theo id.
     * @param id ID của schematic trên server
     * @param c Callback nhận DetailDataSchematic object
     */
    public static void findSchematicById(String id, Cons<DataSchematic> c) {
        // Gọi HTTP GET tới endpoint /schematics/{id}
        Http.get(BrowserConfig.API_URL + "schematics/" + id).submit(response -> {
            String data = response.getResultAsString(); // Lấy response body dạng String
            // Post về main thread để parse JSON và gọi callback
            Core.app.post(() -> c.get(JsonIO.json.fromJson(DataSchematic.class, data)));
        });
    }

    /**
     * Lấy thông tin chi tiết map theo id.
     * @param id ID của map trên server
     * @param c Callback nhận DataMap object
     */
    public static void findMapById(String id, Cons<DataMap> c) {
        // Gọi HTTP GET tới endpoint /maps/{id}
        Http.get(BrowserConfig.API_URL + "maps/" + id).submit(response -> {
            String data = response.getResultAsString(); // Lấy response body dạng String
            // Post về main thread để parse JSON và gọi callback
            Core.app.post(() -> c.get(JsonIO.json.fromJson(DataMap.class, data)));
        });
    }

    /**
     * Lấy thông tin user theo id.
     * @param id ID của user trên server
     * @param c Callback nhận DataUser object
     */
    public static void findUserById(String id, Cons<DataUser> c) {
        // Gọi HTTP GET tới endpoint /users/{id}
        Http.get(BrowserConfig.API_URL + "users/" + id).submit(response -> {
            String data = response.getResultAsString(); // Lấy response body dạng String
            // Post về main thread để parse JSON và gọi callback
            Core.app.post(() -> c.get(JsonIO.json.fromJson(DataUser.class, data)));
        });
    }
}
