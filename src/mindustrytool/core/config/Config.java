package mindustrytool.core.config;

import java.util.Arrays;
import java.util.List;
import mindustrytool.core.model.Sort;

public final class Config {
    private Config() {}

    private static final String BASE = "https://api.mindustry-tool.com/api/";

    public static final String API_URL = BASE + "v4/";
    public static final String IMAGE_URL = "https://image.mindustry-tool.com/";
    public static final String API_REPO_URL = "https://raw.githubusercontent.com/MindustryVN/MindustryToolMod/v8/mod.hjson";
    public static final String REPO_URL = "MindustryVN/MindustryToolMod";
    public static final String WEB_URL = "https://mindustry-tool.com";
    public static final String UPLOAD_SCHEMATIC_URL = WEB_URL + "/schematics?upload=true";
    public static final String UPLOAD_MAP_URL = WEB_URL + "/maps?upload=true";

    public static final List<Sort> sorts = Arrays.asList(
        new Sort("newest", "time_desc"),
        new Sort("oldest", "time_asc"),
        new Sort("most-download", "download-count_desc"),
        new Sort("most-like", "like_desc")
    );
}
