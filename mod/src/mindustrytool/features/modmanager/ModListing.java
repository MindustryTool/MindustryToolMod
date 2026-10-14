package mindustrytool.features.modmanager;

import arc.util.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Remote mod metadata entry parsed from Anuken/MindustryMods {@code mods.json}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModListing {
    private String repo = "";
    private String name = "";
    private String internalName = "";
    private @Nullable String subtitle;
    private String author = "";
    private String lastUpdated = "";
    private int stars;
    private @Nullable String minGameVersion;
    private boolean hasScripts;
    private boolean hasJava;
    private @Nullable String description;

    public String getIconUrl() {
        if (repo == null || repo.trim().isEmpty()) {
            return "";
        }
        return "https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/"
                + repo.trim().replace('/', '_');
    }

    public String getVersion() {
        return lastUpdated != null ? lastUpdated : "";
    }

    public String getShortDescription() {
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            return subtitle.trim();
        }
        if (description != null && !description.trim().isEmpty()) {
            String desc = description.trim();
            int nl = desc.indexOf('\n');
            return nl >= 0 ? desc.substring(0, nl).trim() : desc;
        }
        return "";
    }
}
