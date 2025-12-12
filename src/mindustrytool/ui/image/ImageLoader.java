package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.Pixmap;
import arc.util.Http;
import mindustry.Vars;
import mindustrytool.core.config.Config;

public class ImageLoader {
    public static void load(String id, ImageHandler.ImageType type, arc.files.Fi file, arc.func.Cons<Pixmap> ok) {
        Vars.mainExecutor.execute(() -> {
            if (file.exists()) loadFile(file, ok, () -> { file.delete(); loadHttp(id, type, file, ok); });
            else loadHttp(id, type, file, ok);
        });
    }

    private static void loadFile(arc.files.Fi file, arc.func.Cons<Pixmap> ok, Runnable onError) {
        try {
            byte[] d = file.readBytes();
            Core.app.post(() -> { try { ok.get(new Pixmap(d)); } catch (Exception e) { onError.run(); } });
        } catch (Exception e) { onError.run(); }
    }

    private static void loadHttp(String id, ImageHandler.ImageType type, arc.files.Fi file, arc.func.Cons<Pixmap> ok) {
        Http.get(Config.IMAGE_URL + type.urlSegment + "/" + id + ".png?variant=preview", res -> {
            byte[] d = res.getResult();
            if (d == null || d.length < 8) return;
            Core.app.post(() -> { try { ok.get(new Pixmap(d)); file.writeBytes(d); } catch (Exception e) { file.delete(); } });
        }, e -> {});
    }
}
