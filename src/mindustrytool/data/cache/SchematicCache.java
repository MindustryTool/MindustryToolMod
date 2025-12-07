package mindustrytool.data.cache;

import arc.struct.*;
import arc.func.Func;
import mindustry.game.Schematic;

public class SchematicCache {
    private static ObjectMap<String, Schematic> cache = new ObjectMap<>();

    public static Schematic get(String key, Func<String, Schematic> loader) {
        if (!cache.containsKey(key)) cache.put(key, loader.get(key));
        return cache.get(key);
    }
}
