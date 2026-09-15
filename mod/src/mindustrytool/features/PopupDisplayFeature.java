package mindustrytool.features;

import arc.scene.Element;
import arc.util.Nullable;

/**
 * Contract for features that can render an on-demand popup from QuickAccess
 * instead of toggling when in popup display mode.
 */
public interface PopupDisplayFeature {

    boolean isPopupMode();

    void togglePopup(@Nullable Element quickAccessBar);

    default void openPopup(@Nullable Element quickAccessBar) {
        togglePopup(quickAccessBar);
    }
}
