package mindustrytool.features.autoplay.tasks;

import java.util.Optional;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import mindustry.entities.units.AIController;
import mindustry.gen.Unit;

public interface AutoplayTask {
    default String getId() {
        return getClass().getSimpleName();
    }

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    default void init() {
        setEnabled(Core.settings.getBool("autoplay.task." + getId() + ".enabled", true));
    }

    default void save() {
        Core.settings.put("autoplay.task." + getId() + ".enabled", isEnabled());
    }

    default TextureRegionDrawable getIcon() {
        return null;
    }

    boolean shouldRun(Unit unit);

    AIController getAI();

    default void update(Unit unit) {
    }

    String getStatus();

    default Optional<Table> settings() {
        return Optional.empty();
    }
}
