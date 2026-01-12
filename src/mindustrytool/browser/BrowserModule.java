package mindustrytool.browser; // Khai báo package browser

import arc.Core; // Import Core để truy cập bundle
import arc.files.Fi; // Import Fi để làm việc với files
import arc.struct.Seq; // Import Seq cho danh sách
import arc.util.Log; // Import Log để ghi log

import mindustry.Vars; // Import Vars để truy cập game variables
import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.ui.fragments.MenuFragment.MenuButton; // Import MenuButton cho menu

import mindustrytool.Module; // Import Module interface
import mindustrytool.browser.data.DataMap; // Import DataMap data class
import mindustrytool.browser.data.DataSchematic; // Import DataSchematic data class
import mindustrytool.browser.gui.BrowserDialog; // Import BrowserDialog
import mindustrytool.browser.gui.browser.BrowserType; // Import BrowserType enum

/**
 * Browser Module - Module để browse maps và schematics từ server.
 * Implements Module interface để được load động bởi ModuleLoader.
 * 
 * Module này có thể được xóa mà không ảnh hưởng đến compile của mod.
 */
public class BrowserModule implements Module { // Class module browser, implement Module interface

    // Tên module
    public static final String NAME = "Browser"; // Hằng số tên module

    // Dialog browse schematics - static để dùng global
    public static BrowserDialog<DataSchematic> schematicDialog; // Dialog browse schematics
    // Dialog browse maps - static để dùng global
    public static BrowserDialog<DataMap> mapDialog; // Dialog browse maps

    // Thư mục cache cho images
    public static Fi imageDir; // Thư mục cache images
    // Thư mục cache cho maps
    public static Fi mapsDir; // Thư mục cache maps
    // Thư mục cache cho schematics
    public static Fi schematicDir; // Thư mục cache schematics

    /**
     * Lấy tên module.
     * 
     * @return Tên module "Browser"
     */
    @Override
    public String getName() { // Override method getName
        return NAME; // Trả về tên module
    }

    /**
     * Khởi tạo module.
     * Tạo các thư mục cache và dialogs.
     */
    @Override
    public void init() { // Override method init
        Log.info("[BrowserModule] Initializing..."); // Log bắt đầu init

        // Tạo các thư mục cache
        imageDir = Vars.dataDirectory.child("mindustry-tool-caches"); // Thư mục cache images
        mapsDir = Vars.dataDirectory.child("mindustry-tool-maps"); // Thư mục cache maps
        schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics"); // Thư mục cache schematics

        // Tạo thư mục nếu chưa tồn tại
        imageDir.mkdirs(); // Tạo thư mục images
        mapsDir.mkdirs(); // Tạo thư mục maps
        schematicDir.mkdirs(); // Tạo thư mục schematics

        // Tạo các dialogs
        schematicDialog = new BrowserDialog<DataSchematic>(BrowserType.SCHEMATIC); // Tạo schematic dialog
        mapDialog = new BrowserDialog<DataMap>(BrowserType.MAP); // Tạo map dialog

        Log.info("[BrowserModule] Initialized"); // Log hoàn thành init
    }

    /**
     * Được gọi khi client đã load xong.
     * Thêm nút Browse vào schematic dialog.
     */
    @Override
    public void onClientLoad() { // Override method onClientLoad
        Log.info("[BrowserModule] Client loaded, adding UI elements..."); // Log bắt đầu

        // Thêm nút Browse vào schematic dialog của game
        Vars.ui.schematics.buttons.button("Browse", Icon.menu, () -> { // Tạo button Browse
            Vars.ui.schematics.hide(); // Ẩn schematic dialog gốc
            schematicDialog.show(); // Hiển thị browse dialog
        });

        Log.info("[BrowserModule] UI elements added"); // Log hoàn thành
    }

    /**
     * Lấy danh sách menu buttons cho PC.
     * Trả về button Map Browser nằm trong dropdown.
     * 
     * @return Seq chứa MenuButton
     */
    @Override
    public Seq<MenuButton> getMenuButtons() { // Override method getMenuButtons
        Seq<MenuButton> buttons = new Seq<>(); // Tạo Seq mới

        // Tạo button Map Browser
        MenuButton mapBrowserButton = new MenuButton( // Tạo MenuButton
                Core.bundle.format("message.map-browser.title"), // Tiêu đề từ bundle
                Icon.map, // Icon map
                () -> mapDialog.show() // Action hiển thị map dialog
        );

        buttons.add(mapBrowserButton); // Thêm vào danh sách

        return buttons; // Trả về danh sách buttons
    }

    /**
     * Lấy danh sách menu buttons cho Mobile.
     * Mobile hiển thị button trực tiếp, không dropdown.
     * 
     * @return Seq chứa MenuButton cho mobile
     */
    @Override
    public Seq<MenuButton> getMobileMenuButtons() { // Override method getMobileMenuButtons
        // Mobile dùng cùng buttons với PC
        return getMenuButtons(); // Trả về buttons giống PC
    }

    /**
     * Cleanup khi module bị unload.
     */
    @Override
    public void dispose() { // Override method dispose
        Log.info("[BrowserModule] Disposing..."); // Log bắt đầu dispose
        // Có thể cleanup resources ở đây nếu cần
        schematicDialog = null; // Clear reference
        mapDialog = null; // Clear reference
        Log.info("[BrowserModule] Disposed"); // Log hoàn thành
    }
}
