package mindustrytool.browser.gui.browser; // Khai báo package chứa các class browser UI

import mindustrytool.browser.BrowserConfig; // Import BrowserConfig để lấy upload URLs
import mindustrytool.browser.data.TagService.TagCategoryEnum; // Import TagCategoryEnum để xác định loại tags

/**
 * Enum chứa cấu hình cho từng loại browser.
 * Định nghĩa các thông số khác nhau giữa Map browser và Schematic browser.
 */
public enum BrowserType { // Enum định nghĩa các loại browser

    // Browser cho Maps - title, endpoint, tag category, search hint, upload URL
    MAP("Map Browser", "maps", TagCategoryEnum.maps, "@map.search", BrowserConfig.UPLOAD_MAP_URL),
    // Browser cho Schematics - title, endpoint, tag category, search hint, upload URL
    SCHEMATIC("Schematic Browser", "schematics", TagCategoryEnum.schematics, "@schematic.search", BrowserConfig.UPLOAD_SCHEMATIC_URL);

    // Tiêu đề hiển thị của browser
    public final String title;
    // Endpoint API (vd: "maps" hoặc "schematics")
    public final String endpoint;
    // Loại tag category để lọc
    public final TagCategoryEnum tagCategory;
    // Gợi ý tìm kiếm (localized key)
    public final String searchHint;
    // URL trang upload trên web
    public final String uploadUrl;

    /**
     * Constructor tạo BrowserType.
     * @param title Tiêu đề browser
     * @param endpoint Endpoint API
     * @param tagCategory Loại tag category
     * @param searchHint Key localization cho gợi ý tìm kiếm
     * @param uploadUrl URL trang upload
     */
    BrowserType(String title, String endpoint, TagCategoryEnum tagCategory, String searchHint, String uploadUrl) {
        this.title = title; // Gán tiêu đề
        this.endpoint = endpoint; // Gán endpoint
        this.tagCategory = tagCategory; // Gán tag category
        this.searchHint = searchHint; // Gán search hint
        this.uploadUrl = uploadUrl; // Gán upload URL
    }
}
