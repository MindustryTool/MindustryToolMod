package mindustrytool.browser.data; // Khai báo package chứa các data class của browser

/**
 * Class đơn giản đại diện cho một tùy chọn sắp xếp.
 * Chứa tên hiển thị và giá trị gửi lên API.
 */
public class Sort { // Class chứa thông tin sort

    // Tên hiển thị của loại sort (vd: "newest", "oldest")
    private final String name;
    // Giá trị gửi lên API (vd: "time_desc", "time_asc")
    private final String value;

    /**
     * Constructor tạo Sort.
     * @param name Tên hiển thị
     * @param value Giá trị API
     */
    public Sort(String name, String value) {
        this.name = name; // Gán tên
        this.value = value; // Gán giá trị
    }

    /**
     * Lấy tên hiển thị.
     * @return Tên của sort
     */
    public String getName() {
        return name; // Trả về tên
    }

    /**
     * Lấy giá trị API.
     * @return Giá trị gửi lên API
     */
    public String getValue() {
        return value; // Trả về giá trị
    }
}
