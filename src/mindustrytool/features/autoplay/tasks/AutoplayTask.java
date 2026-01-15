package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import mindustry.entities.units.AIController;

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

    default Drawable getIcon() {
        return null;
    }
    
    boolean shouldRun();

    /**
     * Get the AI controller for this task
     */
    AIController getAI();

    /**
     * Update task state (called every frame when active)
     */
    default void update() {
    }

    /**
     * Build settings UI for this task
     */
    default void buildSettings(Table table) {
    }

    default boolean hasSettings() {
        return false;
    }
}
