package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

import arc.graphics.Color; // Import Color để xử lý màu sắc
import arc.struct.Seq; // Import Seq - mảng động của Arc
import lombok.Data; // Import annotation @Data để tự tạo getter/setter/equals/hashCode
import mindustrytool.browser.BrowserConfig; // Import BrowserConfig để lấy sort mặc định

/**
 * Class chứa cấu hình search/filter hiện tại cho browser UI.
 * Theo dõi các tag đã chọn và sort option đang active.
 */
public class SearchConfig { // Class quản lý cấu hình tìm kiếm

    // Danh sách các tag đã được chọn
    private Seq<SelectedTag> selectedTags = new Seq<>();
    // Sort option hiện tại
    private Sort sort;
    // Flag đánh dấu có thay đổi không (cần refresh)
    private boolean changed = false;

    /**
     * Constructor mặc định.
     * Sử dụng sort đầu tiên từ BrowserConfig.
     */
    public SearchConfig() {
        this.sort = BrowserConfig.SORTS.get(0); // Lấy sort mặc định (newest)
    }

    /**
     * Constructor với sort tùy chọn.
     * @param defaultSort Sort mặc định
     */
    public SearchConfig(Sort defaultSort) {
        this.sort = defaultSort; // Gán sort được truyền vào
    }

    /**
     * Reset flag changed sau khi đã xử lý thay đổi.
     */
    public void update() {
        changed = false; // Đặt lại flag
    }

    /**
     * Kiểm tra có thay đổi không.
     * @return true nếu có thay đổi cần refresh
     */
    public boolean isChanged() {
        return changed; // Trả về flag changed
    }

    /**
     * Lấy chuỗi các tag đã chọn để gửi lên API.
     * Format: "categoryName_tagName,categoryName_tagName,..."
     * @return Chuỗi tags hoặc rỗng nếu không có
     */
    public String getSelectedTagsString() {
        // Nếu không có tag nào, trả về rỗng
        if (selectedTags.isEmpty()) {
            return "";
        }
        // Join các tag với format "category_name"
        return String.join(",", selectedTags.map(s -> s.categoryName + "_" + s.name));
    }

    /**
     * Lấy danh sách tag đã chọn.
     * @return Seq các SelectedTag
     */
    public Seq<SelectedTag> getSelectedTags() {
        return selectedTags; // Trả về list tags
    }

    /**
     * Toggle tag: nếu đã chọn thì bỏ, nếu chưa thì thêm.
     * @param category Category của tag
     * @param value DataTag cần toggle
     */
    public void setTag(TagCategory category, DataTag value) {
        // Tạo SelectedTag từ DataTag
        SelectedTag tag = new SelectedTag();

        tag.name = value.name(); // Gán tên tag
        tag.categoryName = category.name(); // Gán tên category
        tag.icon = value.icon(); // Gán icon
        tag.color = value.color(); // Gán màu

        // Toggle: nếu có thì xóa, không có thì thêm
        if (selectedTags.contains(tag)) {
            this.selectedTags.remove(tag); // Xóa tag
        } else {
            this.selectedTags.add(tag); // Thêm tag
        }
        changed = true; // Đánh dấu có thay đổi
    }

    /**
     * Xóa một tag đã chọn.
     * @param tag Tag cần xóa
     */
    public void removeTag(SelectedTag tag) {
        selectedTags.remove(tag); // Xóa khỏi list
        changed = true; // Đánh dấu có thay đổi
    }

    /**
     * Kiểm tra tag có được chọn không.
     * @param category Category của tag
     * @param tag DataTag cần kiểm tra
     * @return true nếu đã được chọn
     */
    public boolean containTag(TagCategory category, DataTag tag) {
        // Tìm trong list theo name và categoryName
        return selectedTags.contains(v -> v.name.equals(tag.name()) && category.name.equals(v.categoryName));
    }

    /**
     * Lấy sort option hiện tại.
     * @return Sort đang active
     */
    public Sort getSort() {
        return sort; // Trả về sort
    }

    /**
     * Đặt sort option mới.
     * @param sort Sort mới
     */
    public void setSort(Sort sort) {
        this.sort = sort; // Gán sort mới
        changed = true; // Đánh dấu có thay đổi
    }

    /**
     * Nested class đại diện cho một tag đã được chọn.
     * Chứa đầy đủ thông tin để hiển thị và gửi API.
     */
    @Data // Lombok: tự sinh getter, setter, equals, hashCode
    public static class SelectedTag { // Class chứa thông tin tag đã chọn

        // Tên của tag
        private String name;
        // Tên của category chứa tag
        private String categoryName;
        // Icon của tag
        private String icon;
        // Mã màu hex của tag
        private String color;

        /**
         * Chuyển đổi mã màu hex sang Color object.
         * @return Color object hoặc gray nếu null/invalid
         */
        public Color color() {
            // Nếu color null, trả về màu xám
            if (color == null) return Color.gray;
            try {
                return Color.valueOf(color); // Parse hex color
            } catch (Exception e) {
                return Color.gray; // Nếu lỗi, trả về màu xám
            }
        }
    }
}
