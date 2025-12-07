package mindustrytool.presentation.builder;

import arc.Core;
import arc.graphics.*;
import arc.util.*;
import mindustry.Vars;
import mindustrytool.core.config.Config;

public class ImageLoader {
    public static void load(String id, ImageHandler.ImageType type, arc.files.Fi localFile, arc.func.Cons<Pixmap> onSuccess) {
        if (localFile.exists()) loadLocal(id, localFile, onSuccess);
        else loadHttp(id, type, localFile, onSuccess);
    }

    private static void loadLocal(String id, arc.files.Fi file, arc.func.Cons<Pixmap> onSuccess) {
        try { onSuccess.get(new Pixmap(file.readBytes())); }
        catch (Exception e) { Log.err("Load error: " + id, e); }
    }

    private static void loadHttp(String id, ImageHandler.ImageType type, arc.files.Fi localFile, arc.func.Cons<Pixmap> onSuccess) {
        Http.get(Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview", res -> {
            try {
                Pixmap pix = new Pixmap(res.getResult());
                Vars.mainExecutor.execute(() -> { try { localFile.writeBytes(res.getResult()); } catch (Exception e) { Log.err("Write error: " + id, e); } });
                Core.app.post(() -> onSuccess.get(pix));
            } catch (Exception e) { Log.err("HTTP parse error: " + id, e); }
        }, e -> Log.err("HTTP error: " + id, e));
    }
}
