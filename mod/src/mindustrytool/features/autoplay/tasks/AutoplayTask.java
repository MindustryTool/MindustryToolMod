package mindustrytool.features.autoplay.tasks;

import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import mindustry.gen.Unit;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.reactive.Readable;

/**
 * Common interface for all autonomous gameplay tasks.
 */
public interface AutoplayTask {

    String getId();

    String getName();

    TextureRegionDrawable getIcon();

    Readable<String> status();

    boolean update(Unit unit);

    BaseAutoplayAI getAI();

    default @Nullable Vec2 getTargetPos() {
        BaseAutoplayAI ai = getAI();
        return ai != null ? ai.targetPos : null;
    }

    default boolean hasSettings() {
        return false;
    }

    default void buildSettings(AutoplayFeature feature) {
    }
}
