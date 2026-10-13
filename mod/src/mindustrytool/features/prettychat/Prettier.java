package mindustrytool.features.prettychat;

import arc.Core;
import arc.util.Nullable;

/**
 * Contract for a chat text transformer.
 */
public interface Prettier {

    String id();

    String nameKey();

    String defaultName();

    String descriptionKey();

    String defaultDescription();

    default String name() {
        return Core.bundle.has(nameKey())
                ? Core.bundle.get(nameKey())
                : defaultName();
    }

    default String description() {
        return Core.bundle.has(descriptionKey())
                ? Core.bundle.get(descriptionKey())
                : defaultDescription();
    }

    default String category() {
        return "effect";
    }

    /**
     * Transforms the given message string.
     *
     * @param input raw message text
     * @return transformed text
     */
    String transform(String input);

    /**
     * Transforms the given message string with a target length limit.
     *
     * @param input     raw message text
     * @param maxLength target max length limit
     * @return transformed text
     */
    default String transform(String input, int maxLength) {
        return transform(input);
    }

    default boolean isEditable() {
        return false;
    }

    default @Nullable String getScript() {
        return null;
    }

    default void setScript(String script) {
    }

    default void resetScript() {
    }

    default @Nullable String getDefaultScript() {
        return null;
    }
}
