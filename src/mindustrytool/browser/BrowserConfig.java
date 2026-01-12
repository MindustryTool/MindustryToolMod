package mindustrytool.browser; // Khai báo package chứa các class browser chính

import java.util.Arrays; // Import Arrays để tạo list từ array
import java.util.List; // Import List interface

import mindustrytool.browser.data.Sort; // Import Sort data class

/**
 * Các hằng số cấu hình cho Browser module.
 * Bao gồm API endpoints, image host, và các sort mặc định.
 */
public class BrowserConfig { // Class chứa các hằng số cấu hình

    // URL API cho môi trường development
    private static final String DEV_URL = "https://api.mindustry-tool.com/api/";
    // URL API cho môi trường production
    private static final String PROD_URL = "https://api.mindustry-tool.com/api/";
    // Lấy biến môi trường ENV
    private static final String ENV = System.getenv("ENV");
    // Flag kiểm tra có đang ở môi trường DEV không
    public static final boolean DEV = (ENV != null && ENV.equals("DEV"));

    // URL API cuối cùng, dựa vào môi trường DEV hay PROD + version v4
    public static final String API_URL = (DEV ? DEV_URL : PROD_URL) + "v4/";
    // URL host cho ảnh (thumbnails, avatars, etc.)
    public static final String IMAGE_URL = "https://image.mindustry-tool.com/";

    // URL trang web chính
    public static final String WEB_URL = "https://mindustry-tool.com";
    // URL trang upload schematic (mở dialog upload)
    public static final String UPLOAD_SCHEMATIC_URL = WEB_URL + "/schematics?upload=true";
    // URL trang upload map (mở dialog upload)
    public static final String UPLOAD_MAP_URL = WEB_URL + "/maps?upload=true";

    // Danh sách các loại sort có sẵn
    public static final List<Sort> SORTS = Arrays.asList(
            new Sort("newest", "time_desc"), // Mới nhất (theo thời gian giảm dần)
            new Sort("oldest", "time_asc"), // Cũ nhất (theo thời gian tăng dần)
            new Sort("most-download", "download-count_desc"), // Nhiều download nhất
            new Sort("most-like", "like_desc") // Nhiều like nhất
    );

    /**
     * Lấy sort mặc định.
     * @return Sort mặc định (newest)
     */
    public static Sort getDefaultSort() {
        return SORTS.get(0); // Trả về sort đầu tiên (newest)
    }
}
