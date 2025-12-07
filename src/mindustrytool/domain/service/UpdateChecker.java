package mindustrytool.domain.service;

import arc.util.Http;
import arc.util.Log;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.Main;
import mindustrytool.core.config.Config;

public class UpdateChecker {
    public static void check() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        if (mod == null || mod.meta == null || mod.meta.version == null || mod.meta.version.isEmpty()) {
            Log.warn("Failed to get mod metadata");
            return;
        }
        String current = mod.meta.version;
        Http.get(Config.API_REPO_URL)
            .error(e -> Log.err("Failed to check for updates", e))
            .submit(res -> UpdateResponseHandler.handle(res, current));
        Http.get(Config.API_URL + "ping?client=mod-v8")
            .error(e -> {})
            .submit(r -> Log.debug("Ping successful"));
    }
}
