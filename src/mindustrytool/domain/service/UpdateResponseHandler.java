package mindustrytool.domain.service;

import arc.Core;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustrytool.core.config.Config;

public class UpdateResponseHandler {
    public static void handle(Http.HttpResponse res, String current) {
        if (res == null) { Log.warn("Update check response is null"); return; }
        String response = res.getResultAsString();
        if (response == null || response.isEmpty()) { Log.warn("Update check response is empty"); return; }
        try {
            String latest = Jval.read(response).getString("version");
            if (latest != null && !latest.equals(current)) {
                Log.info("Mod requires update: @ -> @", current, latest);
                showUpdatePrompt(current, latest);
            } else Log.info("Mod is up to date");
        } catch (Exception e) { Log.err("Error parsing update response", e); }
    }

    private static void showUpdatePrompt(String current, String latest) {
        Vars.ui.showConfirm(
            Core.bundle.format("message.new-version", current, latest) + "\nDiscord: https://discord.gg/72324gpuCd",
            () -> { if (!current.endsWith("v8")) Core.app.post(() -> Vars.ui.mods.githubImportMod(Config.REPO_URL, true, null)); });
    }
}
