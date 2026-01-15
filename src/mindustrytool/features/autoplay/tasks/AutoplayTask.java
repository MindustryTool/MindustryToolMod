package mindustrytool.features.autoplay.tasks;

import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import mindustry.entities.units.AIController;

public interface AutoplayTask {
    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Get the icon for this task
     */
    default Drawable getIcon() {
        return null;
    }

    /**
     * Check if this task should run in current situation
     */
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
