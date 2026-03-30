package mindustrytool.features.autoplay.tasks;

import java.util.Optional;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import mindustry.gen.Unit;
import arc.math.geom.Vec2;

public interface AutoplayTask {
    default String getId() {
        return getClass().getSimpleName();
    }

    String getName();

    default boolean isEnabled() {
        return Core.settings.getBool("mindustrytool.autoplay.task." + getId() + ".enabled", true);
    }

    default void setEnabled(boolean enabled) {
        Core.settings.put("mindustrytool.autoplay.task." + getId() + ".enabled", enabled);
    }

    default void init() {
    }

    default TextureRegionDrawable getIcon() {
        return null;
    }

    boolean update(Unit unit);

    BaseAutoplayAI getAI();

    String getStatus();

    default Optional<Table> settings() {
        return Optional.empty();
    }

    default Vec2 getTargetPos() {
        BaseAutoplayAI ai = getAI();
        if (ai != null) {
            return ai.targetPos;
        }
        return null;
    }
}
