package mindustrytool.plugins.browser;

import arc.files.Fi;
import mindustry.game.Rules;

public class MapUtils {
    public static Rules readRules(byte[] data) {
        try {
            // Write directly to custom maps dir so importMap works
            String tempName = "preview_" + System.nanoTime() + ".msav";
            Fi tempFi = mindustry.Vars.customMapDirectory.child(tempName);
            tempFi.writeBytes(data); // This overwrites if exists

            try {
                // Import map (parses and adds to list)
                // importMap returns void
                mindustry.Vars.maps.importMap(tempFi);

                // Find the map instance we just imported
                mindustry.maps.Map map = mindustry.Vars.maps.customMaps().find(m -> m.file.equals(tempFi));

                if (map != null) {
                    Rules r = map.rules();
                    // Cleanup from list
                    // removeMap removes it from the list and deletes the file for custom maps
                    mindustry.Vars.maps.removeMap(map);
                    return r;
                } else {
                    // Map not found? Cleanup file manually just in case
                    if (tempFi.exists())
                        tempFi.delete();
                }
            } catch (Exception ex) {
                // If import failed, manually delete the temp file
                if (tempFi.exists())
                    tempFi.delete();
                throw ex;
            }
        } catch (Exception e) {
            arc.util.Log.err("Failed to parse map rules", e);
            return null;
        }
        return null;
    }
}
