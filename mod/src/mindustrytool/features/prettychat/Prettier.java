package mindustrytool.features.prettychat;

import arc.Core;
import arc.util.Nullable;

/**
 * Contract for a chat text transformer.
 */
public interface Prettier {

    /** Unique identifier for this prettier (e.g., "rainbow", "uwu"). */
    String id();

    /** Translation key for display name. */
    String nameKey();

    /** Default display name if bundle key is missing. */
    String defaultName();

    /** Translation key for description. */
    String descriptionKey();

    /** Default description if bundle key is missing. */
    String defaultDescription();

    /** Returns the localized display name. */
    default String name() {
        return Core.bundle != null && Core.bundle.has(nameKey())
                ? Core.bundle.get(nameKey())
                : defaultName();
    }

    /** Returns the localized description. */
    default String description() {
        return Core.bundle != null && Core.bundle.has(descriptionKey())
                ? Core.bundle.get(descriptionKey())
                : defaultDescription();
    }

    /**
     * Transforms the given message string.
     *
     * @param input raw message text
     * @return transformed text
     */
    String transform(String input);

    /** Whether this prettier allows custom user editing (e.g. script / template). */
    default boolean isEditable() {
        return false;
    }

    /** Returns the current custom script/template, if applicable. */
    default @Nullable String getScript() {
        return null;
    }

    /** Sets the custom script/template, if applicable. */
    default void setScript(String script) {
    }

    /** Resets the custom script/template to default. */
    default void resetScript() {
    }

    /** Returns the default script/template, if applicable. */
    default @Nullable String getDefaultScript() {
        return null;
    }
}
