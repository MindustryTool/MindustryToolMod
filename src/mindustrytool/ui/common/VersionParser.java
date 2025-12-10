package mindustrytool.ui.common;

import arc.Core;
import mindustry.core.Version;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionParser {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(.+?) build (\\d+)(?:\\.(\\d+))?$");
    private static final int CLIENT_BUILD = Version.build;
    private static final String CLIENT_TYPE = Version.type;

    public static String getVersionString(String s) {
        int build = -1;
        String type = "custom";
        
        if (!"custom build".equals(s)) {
            Matcher m = VERSION_PATTERN.matcher(s);
            if (m.matches()) { type = m.group(1); build = Integer.parseInt(m.group(2)); }
        }
        
        if (build == -1) return Core.bundle.format("server.custombuild");
        if (build == 0) return Core.bundle.get("server.outdated");
        if (CLIENT_BUILD != -1) {
            if (build < CLIENT_BUILD) return Core.bundle.get("server.outdated") + "\n" + Core.bundle.format("server.version", build, "");
            if (build > CLIENT_BUILD) return Core.bundle.get("server.outdated.client") + "\n" + Core.bundle.format("server.version", build, "");
        }
        return (build == CLIENT_BUILD && CLIENT_TYPE.equals(type)) ? "" : Core.bundle.format("server.version", build, type);
    }
}
