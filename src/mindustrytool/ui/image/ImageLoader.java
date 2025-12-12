package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.*;
import arc.util.*;
import mindustry.Vars;
import mindustrytool.core.config.Config;

public class ImageLoader {
    public static void load(String id, ImageHandler.ImageType type, arc.files.Fi localFile, arc.func.Cons<Pixmap> onSuccess) {
        // Load async to not block main thread
        Vars.mainExecutor.execute(() -> {
            if (localFile.exists()) loadLocal(id, localFile, onSuccess);
            else loadHttp(id, type, localFile, onSuccess);
        });
    }

    private static void loadLocal(String id, arc.files.Fi file, arc.func.Cons<Pixmap> onSuccess) {
        try { 
            byte[] data = file.readBytes();
            Pixmap pix = new Pixmap(data);
            Core.app.post(() -> onSuccess.get(pix)); 
        } catch (Exception e) { Log.err("Load error: " + id, e); }
    }

    private static void loadHttp(String id, ImageHandler.ImageType type, arc.files.Fi localFile, arc.func.Cons<Pixmap> onSuccess) {
        Http.get(Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview", res -> {
            try {
                byte[] data = res.getResult();
                Pixmap pix = new Pixmap(data);
                // Write file async, don't block
                Vars.mainExecutor.execute(() -> { 
                    try { localFile.writeBytes(data); } 
                    catch (Exception e) { Log.err("Write error: " + id, e); } 
                });
                Core.app.post(() -> onSuccess.get(pix));
            } catch (Exception e) { Log.err("HTTP parse error: " + id, e); }
        }, e -> Log.err("HTTP error: " + id, e));
    }
}
