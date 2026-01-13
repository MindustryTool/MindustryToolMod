package mindustrytool.features;

import mindustry.gen.Iconc;

public record FeatureMetadata(String name, String description, char icon, int order, boolean enabledByDefault,
        boolean quickAccess) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private char icon = Iconc.settings;
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

        public Builder icon(char icon) {
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
            return new FeatureMetadata(name, description, icon, order, enabledByDefault, quickAccess);
        }
    }
}
