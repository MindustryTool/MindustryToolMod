package mindustrytool.features;

import arc.scene.style.Drawable;

public class FeatureMetadata {
    String name;
    String description;
    Drawable icon;
    int order;
    boolean enabledByDefault;
    boolean quickAccess;

    private FeatureMetadata(String name, String description, Drawable icon, int order, boolean enabledByDefault,
            boolean quickAccess) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.order = order;
        this.enabledByDefault = enabledByDefault;
        this.quickAccess = quickAccess;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Drawable icon() {
        return icon;
    }

    public int order() {
        return order;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    public boolean quickAccess() {
        return quickAccess;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Drawable icon = null;
        private int order = 0;
        private boolean enabledByDefault = true;
        private boolean quickAccess = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public FeatureMetadata build() {
            if (name == null)
                throw new IllegalStateException("Name is required");
            if (description == null)
                throw new IllegalStateException("Description is required");
            if (icon == null)
                throw new IllegalStateException("Icon is required");

            return new FeatureMetadata(name, description, icon, order, enabledByDefault, quickAccess);
        }
    }
}
