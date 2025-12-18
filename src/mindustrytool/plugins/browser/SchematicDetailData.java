package mindustrytool.plugins.browser;

import arc.struct.Seq;

public class SchematicDetailData {
    private String id;
    private String itemId;
    private String createdBy;
    private String name;
    private String description;
    private int width;
    private int height;
    private Long likes;
    private Long downloads = 0L;
    private Long comments = 0L;
    private Seq<TagData> tags;
    public String createdAt;
    public String created_at;
    public String status;
    public String verifiedBy;
    public String verified_by;
    public String verificationStatus;
    public String verifierId;
    public String data; // Potential Base64 payload
    public SchematicMetadata meta;

    public String id() {
        return id;
    }

    public SchematicDetailData id(String id) {
        this.id = id;
        return this;
    }

    public String itemId() {
        return itemId;
    }

    public SchematicDetailData itemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public String createdBy() {
        return createdBy;
    }

    public SchematicDetailData createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String name() {
        return name;
    }

    public SchematicDetailData name(String name) {
        this.name = name;
        return this;
    }

    public String description() {
        return description;
    }

    public SchematicDetailData description(String description) {
        this.description = description;
        return this;
    }

    public int width() {
        return width;
    }

    public SchematicDetailData width(int width) {
        this.width = width;
        return this;
    }

    public int height() {
        return height;
    }

    public SchematicDetailData height(int height) {
        this.height = height;
        return this;
    }

    public Long likes() {
        return likes;
    }

    public SchematicDetailData likes(Long likes) {
        this.likes = likes;
        return this;
    }

    public Long downloads() {
        return downloads;
    }

    public SchematicDetailData downloads(Long downloads) {
        this.downloads = downloads;
        return this;
    }

    public Long comments() {
        return comments;
    }

    public SchematicDetailData comments(Long comments) {
        this.comments = comments;
        return this;
    }

    public Seq<TagData> tags() {
        return tags;
    }

    public SchematicDetailData tags(Seq<TagData> tags) {
        this.tags = tags;
        return this;
    }

    public String createdAt() {
        return createdAt != null ? createdAt : created_at;
    }

    public void createdAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String status() {
        return status != null ? status : verificationStatus;
    }

    public void status(String status) {
        this.status = status;
    }

    public String verifiedBy() {
        return verifiedBy != null ? verifiedBy : (verified_by != null ? verified_by : verifierId);
    }

    public void verifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public SchematicMetadata meta() {
        return meta;
    }

    public SchematicDetailData meta(SchematicMetadata meta) {
        this.meta = meta;
        return this;
    }

    public static class SchematicMetadata {
        private Seq<SchematicRequirement> requirements;
        public Float powerProduction;
        public Float powerConsumption;

        public Float powerProduction() {
            return powerProduction;
        }

        public SchematicMetadata powerProduction(Float p) {
            this.powerProduction = p;
            return this;
        }

        public Float powerConsumption() {
            return powerConsumption;
        }

        public SchematicMetadata powerConsumption(Float p) {
            this.powerConsumption = p;
            return this;
        }

        public Seq<SchematicRequirement> requirements() {
            return requirements;
        }

        public SchematicMetadata requirements(Seq<SchematicRequirement> requirements) {
            this.requirements = requirements;
            return this;
        }
    }

    public static class SchematicRequirement {
        private String name;
        private String color;
        private Integer amount;

        public String name() {
            return name;
        }

        public SchematicRequirement name(String name) {
            this.name = name;
            return this;
        }

        public String color() {
            return color;
        }

        public SchematicRequirement color(String color) {
            this.color = color;
            return this;
        }

        public Integer amount() {
            return amount;
        }

        public SchematicRequirement amount(Integer amount) {
            this.amount = amount;
            return this;
        }
    }
}
