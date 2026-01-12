package mindustrytool.playerconnect; // Khai báo package playerconnect

import arc.Core; // Import Core để truy cập bundle
import arc.struct.Seq; // Import Seq cho danh sách
import arc.util.Log; // Import Log để ghi log

import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.ui.fragments.MenuFragment.MenuButton; // Import MenuButton cho menu

import mindustrytool.Module; // Import Module interface
import mindustrytool.playerconnect.gui.CreateRoomDialog; // Import CreateRoomDialog
import mindustrytool.playerconnect.gui.JoinRoomDialog; // Import JoinRoomDialog
import mindustrytool.playerconnect.gui.PlayerConnectRoomsDialog; // Import PlayerConnectRoomsDialog

/**
 * PlayerConnect Module - Module để kết nối và chơi multiplayer qua CLaJ.
 * Implements Module interface để được load động bởi ModuleLoader.
 * 
 * Module này có thể được xóa mà không ảnh hưởng đến compile của mod.
 */
public class PlayerConnectModule implements Module { // Class module playerconnect, implement Module interface

    // Tên module
    public static final String NAME = "PlayerConnect"; // Hằng số tên module

    // Dialog danh sách rooms player connect
    public static PlayerConnectRoomsDialog playerConnectRoomsDialog; // Dialog danh sách rooms
    // Dialog tạo room mới
    public static CreateRoomDialog createRoomDialog; // Dialog tạo room
    // Dialog join room
    public static JoinRoomDialog joinRoomDialog; // Dialog join room

    /**
     * Lấy tên module.
     * 
     * @return Tên module "PlayerConnect"
     */
    @Override
    public String getName() { // Override method getName
        return NAME; // Trả về tên module
    }

    /**
     * Khởi tạo module.
     * Tạo các dialogs.
     */
    @Override
    public void init() { // Override method init
        Log.info("[PlayerConnectModule] Initializing..."); // Log bắt đầu init

        // Tạo các dialogs
        playerConnectRoomsDialog = new PlayerConnectRoomsDialog(); // Tạo rooms dialog
        createRoomDialog = new CreateRoomDialog(); // Tạo create room dialog
        joinRoomDialog = new JoinRoomDialog(playerConnectRoomsDialog); // Tạo join room dialog

        Log.info("[PlayerConnectModule] Initialized"); // Log hoàn thành init
    }

    /**
     * Được gọi khi client đã load xong.
     * Module này không cần thêm UI elements trong onClientLoad.
     */
    @Override
    public void onClientLoad() { // Override method onClientLoad
        Log.info("[PlayerConnectModule] Client loaded"); // Log client đã load
        // Không cần thêm gì vào game UI vì menu buttons đã được xử lý bởi ModuleLoader
    }

    /**
     * Lấy danh sách menu buttons cho PC.
     * Trả về button Player Connect.
     * 
     * @return Seq chứa MenuButton
     */
    @Override
    public Seq<MenuButton> getMenuButtons() { // Override method getMenuButtons
        Seq<MenuButton> buttons = new Seq<>(); // Tạo Seq mới

        // Tạo button Player Connect
        MenuButton playerConnectButton = new MenuButton( // Tạo MenuButton
                Core.bundle.format("message.player-connect.title"), // Tiêu đề từ bundle
                Icon.menu, // Icon menu
                () -> playerConnectRoomsDialog.show() // Action hiển thị rooms dialog
        );

        buttons.add(playerConnectButton); // Thêm vào danh sách

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
        Log.info("[PlayerConnectModule] Disposing..."); // Log bắt đầu dispose
        // Có thể cleanup resources ở đây nếu cần
        playerConnectRoomsDialog = null; // Clear reference
        createRoomDialog = null; // Clear reference
        joinRoomDialog = null; // Clear reference
        Log.info("[PlayerConnectModule] Disposed"); // Log hoàn thành
    }
}
