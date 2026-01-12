package mindustrytool; // Khai báo package chính của mod

import arc.struct.Seq; // Import Seq cho danh sách menu buttons
import mindustry.ui.fragments.MenuFragment.MenuButton; // Import MenuButton cho menu

/**
 * Interface định nghĩa một Module có thể load động.
 * Các module implement interface này sẽ được tự động phát hiện và load
 * thông qua ServiceLoader từ META-INF/services.
 * 
 * Module có thể được thêm hoặc xóa mà không ảnh hưởng đến compile.
 */
public interface Module { // Interface cho các module độc lập

    /**
     * Lấy tên của module.
     * Dùng để hiển thị trong logs và debug.
     * 
     * @return Tên module
     */
    String getName(); // Method trả về tên module

    /**
     * Khởi tạo module.
     * Được gọi một lần khi mod load.
     * Dùng để khởi tạo resources, dialogs, etc.
     */
    void init(); // Method khởi tạo module

    /**
     * Được gọi khi client đã load xong.
     * Dùng để thêm UI elements vào game.
     */
    void onClientLoad(); // Method được gọi khi client load xong

    /**
     * Lấy danh sách menu buttons của module.
     * Các buttons này sẽ được thêm vào menu chính.
     * 
     * @return Danh sách MenuButton, có thể rỗng
     */
    Seq<MenuButton> getMenuButtons(); // Method trả về danh sách menu buttons

    /**
     * Lấy danh sách menu buttons cho mobile.
     * Mobile UI khác với PC nên cần buttons riêng.
     * 
     * @return Danh sách MenuButton cho mobile, có thể rỗng
     */
    default Seq<MenuButton> getMobileMenuButtons() { // Method mặc định trả về buttons cho mobile
        return getMenuButtons(); // Mặc định trả về cùng buttons với PC
    }

    /**
     * Cleanup khi module bị unload.
     * Dùng để giải phóng resources.
     */
    default void dispose() { // Method cleanup mặc định
        // Mặc định không làm gì
    }
}
