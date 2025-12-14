package mindustrytool.service.content;

import arc.Core;
import arc.util.*;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.Main;
import mindustrytool.core.config.Config;

public class UpdateChecker {
    public static void check() {
        LoadedMod mod = Vars.mods.getMod(Main.class);
        if (mod == null || mod.meta == null || mod.meta.version == null) return;
        String current = mod.meta.version;
        Http.get(Config.API_REPO_URL).error(e -> {}).submit(res -> handleResponse(res, current));
        Http.get(Config.API_URL + "ping?client=mod-v8").error(e -> {}).submit(r -> {});
    }

    private static void handleResponse(Http.HttpResponse res, String current) {
        if (res == null) return;
        String response = res.getResultAsString();
        if (response == null || response.isEmpty()) return;
        try {
            String latest = Jval.read(response).getString("version");
            if (latest != null && !latest.equals(current)) showUpdatePrompt(current, latest);
        } catch (Exception e) { Log.err("Update check error", e); }
    }

    private static void showUpdatePrompt(String current, String latest) {
        Vars.ui.showConfirm(Core.bundle.format("message.new-version", current, latest) + "\nDiscord: https://discord.gg/72324gpuCd",
            () -> { if (!current.endsWith("v8")) Core.app.post(() -> Vars.ui.mods.githubImportMod(Config.REPO_URL, true, null)); });
    }
}
