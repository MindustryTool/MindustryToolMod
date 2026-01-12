package mindustrytool.config; // Khai báo package chứa các class config/utility

/**
 * Class chứa các hằng số cấu hình của mod.
 * Bao gồm API URLs, image host, và các URLs liên quan.
 */
public class Config { // Class Config

    // URL API cho môi trường development
    private static final String DEV_URL = "https://api.mindustry-tool.com/api/";
    // private static final String DEV_URL = "http://localhost:8080/api/v3/";
    // URL API cho môi trường production
    private static final String PROD_URL = "https://api.mindustry-tool.com/api/";
    // private static final String PROD_URL =
    // "https://api.mindustry-tool.com/api/v3/";
    // Lấy biến môi trường ENV
    private static final String ENV = System.getenv("ENV");
    // Flag kiểm tra có đang ở môi trường DEV không
    public static final boolean DEV = (ENV != null && ENV.equals("DEV"));
    // URL API cuối cùng + version v4
    public static final String API_URL = (DEV ? DEV_URL : PROD_URL) + "v4/";
    // URL API v4 (alias)
    public static final String API_v4_URL = (DEV ? DEV_URL : PROD_URL) + "v4/";
    // URL host cho ảnh
    public static final String IMAGE_URL = "https://image.mindustry-tool.com/";

    // URL raw file mod.hjson trên GitHub để check version
    public static final String API_REPO_URL = "https://raw.githubusercontent.com/MindustryVN/MindustryToolMod/v8/mod.hjson";

    // URL repo trên GitHub (format: owner/repo)
    public static final String REPO_URL = "MindustryVN/MindustryToolMod";

    // URL trang web chính
    public static final String WEB_URL = "https://mindustry-tool.com";
    // URL trang upload schematic
    public static final String UPLOAD_SCHEMATIC_URL = WEB_URL + "/schematics?upload=true";
    // URL trang upload map
    public static final String UPLOAD_MAP_URL = WEB_URL + "/maps?upload=true";
}
