package mindustrytool;

import java.util.Arrays;
import java.util.List;

import mindustrytool.dto.Sort;

public class Config {

    private static final String DEV_URL = "https://api.mindustry-tool.com/api/";
    // private static final String DEV_URL = "http://localhost:8080/api/v3/";
    private static final String PROD_URL = "https://api.mindustry-tool.com/api/";
    // private static final String PROD_URL =
    // "https://api.mindustry-tool.com/api/v3/";
    private static final String ENV = System.getenv("ENV");
    public static final boolean DEV = (ENV != null && ENV.equals("DEV"));
    public static final String API_URL = (DEV ? DEV_URL : PROD_URL) + "v4/";
    public static final String API_v4_URL = (DEV ? DEV_URL : PROD_URL) + "v4/";
    public static final String IMAGE_URL = "https://image.mindustry-tool.com/";

    public static final String API_REPO_URL = "https://raw.githubusercontent.com/MindustryVN/MindustryToolMod/v8/mod.hjson";

    public static final String REPO_URL = "MindustryVN/MindustryToolMod";

    public static final String WEB_URL = "https://mindustry-tool.com";
    public static final String UPLOAD_SCHEMATIC_URL = WEB_URL + "/schematics?upload=true";
    public static final String UPLOAD_MAP_URL = WEB_URL + "/maps?upload=true";

    public static final List<Sort> sorts = Arrays.asList(//
            new Sort("newest", "time_desc"), //
            new Sort("oldest", "time_asc"), //
            new Sort("most-download", "download-count_desc"), //
            new Sort("most-like", "like_desc"));
}
