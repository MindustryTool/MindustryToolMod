package mindustrytool.presentation.builder;

import mindustry.Vars;
import mindustrytool.data.cache.CacheManager;
import mindustrytool.data.api.Api;
import mindustrytool.core.model.*;
import mindustrytool.domain.service.*;
import mindustrytool.presentation.dialog.MapInfoDialog;
import mindustrytool.presentation.dialog.SchematicInfoDialog;
import mindustry.ui.dialogs.BaseDialog;

public class InfoOpener {
    private static final CacheManager<String, Object> mapCache = new CacheManager<>(128);
    private static final CacheManager<String, Object> schematicCache = new CacheManager<>(128);

    public static void open(ContentData data, ContentType type, BaseDialog infoDialog) {
        if (data == null) { Vars.ui.showInfo("Cannot open info: Data is null."); return; }
        String id = data.id();
        CacheManager<String, Object> cache = type == ContentType.MAP ? mapCache : schematicCache;
        if (cache.has(id)) { showCached(cache.get(id), type, infoDialog); return; }
        fetch(id, type, cache, infoDialog);
    }

    private static void showCached(Object cached, ContentType type, BaseDialog infoDialog) {
        if (type == ContentType.MAP && cached instanceof MapDetailData) ((MapInfoDialog) infoDialog).show((MapDetailData) cached);
        else if (cached instanceof SchematicDetailData) ((SchematicInfoDialog) infoDialog).show((SchematicDetailData) cached);
    }

    private static void fetch(String id, ContentType type, CacheManager<String, Object> cache, BaseDialog infoDialog) {
        if (type == ContentType.MAP) {
            Api.findMapById(id, m -> { if (m != null) { cache.put(id, m); ((MapInfoDialog) infoDialog).show(m); } else Vars.ui.showInfo("Failed to load map info."); });
        } else {
            Api.findSchematicById(id, s -> { if (s != null) { cache.put(id, s); ((SchematicInfoDialog) infoDialog).show(s); } else Vars.ui.showInfo("Failed to load schematic info."); });
        }
    }
}
