package mindustrytool.features;

import arc.input.KeyBind;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import java.util.Optional;
import lombok.Getter;

@Getter
public class FeatureMetadata {
    private final String id;
    private final Drawable icon;
    private final int order;
    private final boolean enabledByDefault;
    private final boolean quickAccess;
    private final boolean development;
    private final Optional<KeyBind> keybind;

    private FeatureMetadata(
            String id,
            Drawable icon,
            int order,
            boolean enabledByDefault,
            boolean quickAccess,
            boolean development,
            @Nullable KeyBind keybind) {
        this.id = id;
        this.icon = icon;
        this.order = order;
        this.enabledByDefault = enabledByDefault;
        this.quickAccess = quickAccess;
        this.development = development;
        this.keybind = Optional.ofNullable(keybind);
    }

    public int getOrder() {
        if (development) {
            return Integer.MAX_VALUE;
        }
        return order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private Drawable icon = null;
        private int order = 0;
        private boolean enabledByDefault = true;
        private boolean quickAccess = false;
        private boolean development = false;
        private @Nullable KeyBind keybind = null;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder icon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder enabledByDefault(boolean enabledByDefault) {
            this.enabledByDefault = enabledByDefault;
            return this;
        }

        public Builder quickAccess(boolean quickAccess) {
            this.quickAccess = quickAccess;
            return this;
        }

        public Builder development(boolean development) {
            this.development = development;
            return this;
        }

        public Builder keybind(KeyBind keybind) {
            this.keybind = keybind;
            return this;
        }

        public FeatureMetadata build() {
            if (id == null)
                throw new IllegalStateException("ID is required");

            if (icon == null) {
                throw new IllegalStateException("Icon is required");
            }

            return new FeatureMetadata(id, icon, order, enabledByDefault, quickAccess, development, keybind);
        }
    }
}
