package mindustrytool.features.prettychat;

import arc.Core;
import arc.util.Nullable;

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

    String transform(String input);

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
