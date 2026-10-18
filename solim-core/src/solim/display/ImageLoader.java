package solim.display;

import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;

@FunctionalInterface
public interface ImageLoader {

    void load(String url, int radius, float targetW, float targetH,
            Cons<TextureRegion> onSuccess, Cons<Throwable> onError);
}
