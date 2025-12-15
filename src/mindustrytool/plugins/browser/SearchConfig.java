package mindustrytool.plugins.browser;

import arc.struct.Seq;

public class SearchConfig {
    private Seq<SelectedTag> selectedTags = new Seq<>();
    private Sort sort = Config.sorts.get(0);
    private boolean changed = false;

    public void update() { changed = false; }
    public boolean isChanged() { return changed; }
    public String getSelectedTagsString() { return selectedTags.isEmpty() ? "" : String.join(",", selectedTags.map(s -> s.categoryName + "_" + s.name)); }
    public Seq<SelectedTag> getSelectedTags() { return selectedTags; }

    public void setTag(TagCategory category, TagData value) {
        SelectedTag tag = new SelectedTag();
        tag.name = value.name(); 
        tag.categoryName = category.name(); 
        tag.icon = value.icon();
        if (selectedTags.contains(tag)) selectedTags.remove(tag);
        else selectedTags.add(tag);
        changed = true;
    }

    public boolean containTag(TagCategory category, TagData tag) {
        return selectedTags.contains(v -> v.name.equals(tag.name()) && category.name.equals(v.categoryName));
    }

    public Sort getSort() { return sort; }
    public void setSort(Sort sort) { this.sort = sort; changed = true; }

    public static class SelectedTag {
        private String name;
        private String categoryName;
        private String icon;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SelectedTag that = (SelectedTag) o;
            return name != null && name.equals(that.name) && categoryName != null && categoryName.equals(that.categoryName);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
            return result;
        }
    }
}
