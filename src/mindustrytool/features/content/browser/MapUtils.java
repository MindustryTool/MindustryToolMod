package mindustrytool.features.content.browser;

import arc.files.Fi;
import mindustry.game.Rules;

public class MapUtils {
    public static Rules readRules(byte[] data) {
        if (data == null || data.length == 0)
            return null;

        try {
            // Write directly to custom maps dir so importMap works
            // Use unique name to prevent collisions
            String tempName = "preview_" + System.nanoTime() + ".msav";
            Fi tempFi = mindustry.Vars.customMapDirectory.child(tempName);
            tempFi.writeBytes(data); // This overwrites if exists

            try {
                // Import map (parses and adds to list)
                mindustry.Vars.maps.importMap(tempFi);

                // Find the map instance we just imported
                // If duplicates exist, this might pick another one, but checking file path is
                // safest
                mindustry.maps.Map map = mindustry.Vars.maps.customMaps().find(m -> m.file.equals(tempFi));

                if (map != null) {
                    Rules r = map.rules();
                    // Cleanup from list
                    mindustry.Vars.maps.removeMap(map);
                    return r;
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
