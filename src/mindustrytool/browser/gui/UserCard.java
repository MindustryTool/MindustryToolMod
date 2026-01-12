package mindustrytool.browser.gui; // Khai báo package chứa các component UI của browser

import java.util.ArrayList; // Import ArrayList từ Java standard library
import java.util.List; // Import interface List

import arc.func.Cons; // Import interface callback function của Arc
import arc.scene.ui.layout.Table; // Import Table layout container
import arc.struct.ObjectMap; // Import ObjectMap - HashMap của Arc
import mindustrytool.browser.BrowserApi; // Import API calls cho browser
import mindustrytool.browser.data.DataUser; // Import data class cho User

/**
 * Hiển thị thông tin user thu gọn (avatar + tên).
 * <p>
 * Cache dữ liệu user đã fetch và hỗ trợ cập nhật nhiều listener
 * đang chờ cùng một user id khi request đang chạy.
 */
public class UserCard {

    // Cache lưu trữ dữ liệu user theo id, tránh fetch lại nhiều lần
    private static final ObjectMap<String, DataUser> cache = new ObjectMap<>();
    // Map lưu các listener đang chờ dữ liệu user (khi request đang in-flight)
    private static final ObjectMap<String, List<Cons<DataUser>>> listeners = new ObjectMap<>();

    /**
     * Vẽ user card vào table cha. UI sẽ hiển thị placeholder
     * trong khi đang fetch dữ liệu user từ API.
     * @param parent Table cha để chứa user card
     * @param id ID của user cần hiển thị
     */
    public static void draw(Table parent, String id) {
        // Tạo scroll pane chứa nội dung card
        parent.pane(card -> {

            // Lấy dữ liệu user từ cache
            DataUser userData = cache.get(id);

            // Nếu chưa có trong cache - cần fetch từ API
            if (userData == null) {
                // Đặt placeholder vào cache để đánh dấu đang loading
                cache.put(id, new DataUser());
                // Thêm listener để cập nhật UI khi có dữ liệu
                listeners.get(id, () -> new ArrayList<>()).add((data) -> draw(card, data));

                // Gọi API để lấy thông tin user
                BrowserApi.findUserById(id, data -> {
                    // Lưu kết quả vào cache
                    cache.put(id, data);

                    // Lấy danh sách listener đang chờ
                    var l = listeners.get(id);

                    // Thông báo cho tất cả listener
                    if (l != null) {
                        for (Cons<DataUser> listener : l) {
                            listener.get(data); // Gọi callback với dữ liệu mới
                        }
                        listeners.remove(id); // Xóa listeners sau khi đã thông báo
                    }
                });

                // Hiển thị text loading trong khi chờ
                card.add("Loading...");
                return;
            }

            // Nếu có trong cache nhưng chưa có id (đang loading)
            if (userData.id() == null) {
                // Thêm listener để chờ kết quả
                listeners.get(id, () -> new ArrayList<>()).add((data) -> draw(card, data));
                card.add("Loading...."); // Hiển thị loading
                return;
            }

            // Có dữ liệu rồi - vẽ UI
            draw(card, userData);
        })
                .height(50); // Chiều cao cố định 50px
    }

    /**
     * Vẽ nội dung user card (avatar + tên).
     * @param card Table container
     * @param data Dữ liệu user
     */
    private static void draw(Table card, DataUser data) {
        // Xóa nội dung cũ
        card.clear();
        // Nếu có avatar URL thì hiển thị
        if (data.imageUrl() != null && !data.imageUrl().isEmpty()) {
            // Thêm NetworkImage để load avatar, kích thước 24x24
            card.add(new NetworkImage(data.imageUrl())).size(24).padRight(4);
        }
        // Thêm tên user
        card.add(data.name());
    }
}

