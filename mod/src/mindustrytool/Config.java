package mindustrytool;

import java.util.Arrays;
import java.util.List;
import mindustrytool.models.response.Sort;

public class Config {
	private static final String DEV_URL = "https://api.mindustry-tool.com/api/v4";
	private static final String PROD_URL = "https://api.mindustry-tool.com/api/v4";
	private static final String ENV = System.getenv("ENV");

	public static final boolean DEV = ENV != null && ENV.equals("DEV");
	public static final String API_URL = DEV ? DEV_URL : PROD_URL;

	public static final String ORG_NAME = "MindustryTool";
	public static final String REPO_NAME = "MindustryToolMod";
	public static final String MOD_HJSON_URL =
			"https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/v8/mod.hjson";
	public static final String GITHUB_API_URL = "https://api.github.com/repos/MindustryTool/MindustryToolMod/releases";

	public static final String WEB_URL = "https://mindustry-tool.com";
	public static final String UPLOAD_SCHEMATIC_URL = WEB_URL + "/schematics?upload=true";
	public static final String UPLOAD_MAP_URL = WEB_URL + "/maps?upload=true";

	public static final String DISCORD_INVITE_URL = "https://mindustry-tool.com/links/mindustry-tool";

	public static final List<Sort> sorts = Arrays.asList( //
			new Sort("newest", "time_desc"), //
			new Sort("oldest", "time_asc"), //
			new Sort("most-download", "download-count_desc"), //
			new Sort("most-like", "like_desc"));

	public static final String PROJECT_URL = "https://your-choice-seven.vercel.app";
	public static final String PROJECT_ID = "cmm8tccmc0001vlucyj8bn6s1";
}
