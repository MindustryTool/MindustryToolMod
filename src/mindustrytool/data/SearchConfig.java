package mindustrytool.data;

import arc.struct.Seq;
import lombok.Data;
import mindustrytool.config.Config;

public class SearchConfig {
    private Seq<SelectedTag> selectedTags = new Seq<>();
    private Sort sort = Config.sorts.get(0);
    private boolean changed = false;

    public void update() {
        changed = false;
    }

    public boolean isChanged() {
        return changed;
    }

    public String getSelectedTagsString() {
        return String.join(",", selectedTags.map(s -> s.categoryName + "_" + s.name));
    }

    public Seq<SelectedTag> getSelectedTags() {
        return selectedTags;
    }

    public void setTag(TagCategory category, TagData value) {
        SelectedTag tag = new SelectedTag();

        tag.name = value.name();
        tag.categoryName = category.name();
        tag.icon = value.icon();

        if (selectedTags.contains(tag)) {
            this.selectedTags.remove(tag);
        } else {
            this.selectedTags.add(tag);
        }
        changed = true;
    }

    public boolean containTag(TagCategory category, TagData tag) {
        return selectedTags.contains(v -> v.name.equals(tag.name()) && category.name.equals(v.categoryName));
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
        changed = true;
    }

    @Data
    public static class SelectedTag {
        private String name;
        private String categoryName;
        private String icon;
    }
}
