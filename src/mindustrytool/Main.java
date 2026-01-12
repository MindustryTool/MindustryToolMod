package mindustrytool; // Khai báo package chính của mod

import arc.Core; // Import Core để truy cập bundle, app
import arc.Events; // Import Events để đăng ký event listeners
import arc.struct.Seq; // Import Seq cho danh sách
import arc.util.Http; // Import Http để gọi HTTP requests
import arc.util.Log; // Import Log để ghi log
import arc.util.serialization.Jval; // Import Jval để parse JSON

import mindustry.Vars; // Import Vars để truy cập game variables
import mindustry.editor.MapResizeDialog; // Import MapResizeDialog để set maxSize
import mindustry.game.EventType.ClientLoadEvent; // Import event khi client load xong
import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.mod.*; // Import mod classes
import mindustry.mod.Mods.LoadedMod; // Import LoadedMod để lấy version
import mindustry.ui.fragments.MenuFragment.MenuButton; // Import MenuButton cho menu

import mindustrytool.config.Config; // Import Config cho API URLs
// Không còn import trực tiếp browser và playerconnect - chúng được load động qua ModuleLoader

/**
 * Main class của mod MindustryTool.
 * Extends Mod để được Mindustry load.
 * 
 * Các modules (browser, playerconnect) được load động thông qua ServiceLoader.
 * Xem META-INF/services/mindustrytool.Module để biết danh sách modules.
 */
public class Main extends Mod { // Class chính của mod

    // Không còn references trực tiếp đến module dialogs
    // Các dialogs được quản lý bởi từng module riêng biệt

    /**
     * Constructor của mod.
     * Được gọi khi mod load.
     */
    public Main() {
        // Tăng max schematic size lên 4000 tiles
        Vars.maxSchematicSize = 4000;
        // Tăng max map resize size
        MapResizeDialog.maxSize = 4000;
    }

    /**
     * Khởi tạo mod.
     * Được gọi sau khi game và content load xong.
     */
    @Override
    public void init() {
        Log.info("[Main] Initializing MindustryTool mod..."); // Log bắt đầu init

        // Kiểm tra cập nhật mod
        checkForUpdate();

        // Load tất cả modules thông qua ServiceLoader
        ModuleLoader.loadModules(); // Load các modules từ META-INF/services

        // Đăng ký listener khi client load xong
        Events.on(ClientLoadEvent.class, (event) -> {
            Log.info("[Main] Client loaded, setting up UI..."); // Log client đã load

            // Thông báo cho tất cả modules rằng client đã load
            ModuleLoader.onClientLoad(); // Gọi onClientLoad cho từng module

            // Lấy menu buttons từ tất cả modules
            Seq<MenuButton> menuButtons = ModuleLoader.getAllMenuButtons(); // Lấy buttons từ modules

            // Thêm buttons vào menu
            if (Vars.mobile) {
                // Mobile: Thêm từng button riêng biệt
                Seq<MenuButton> mobileButtons = ModuleLoader.getAllMobileMenuButtons(); // Lấy mobile buttons
                for (MenuButton button : mobileButtons) { // Vòng lặp qua các buttons
                    Vars.ui.menufrag.addButton(button.text, button.icon, button.runnable); // Thêm button
                }
            } else {
                // PC: Thêm dropdown menu "Tools" chứa tất cả module buttons
                if (menuButtons.size > 0) { // Kiểm tra có buttons không
                    // Tạo mảng sub-buttons từ Seq
                    MenuButton[] subButtons = menuButtons.toArray(MenuButton.class); // Convert sang array

                    // Tạo parent button với sub-buttons
                    Vars.ui.menufrag.addButton(new MenuButton( // Thêm MenuButton dropdown
                            "Tools", // Tiêu đề dropdown
                            Icon.wrench, // Icon
                            () -> {}, // Parent không có action
                            subButtons // Sub-buttons từ các modules
                    ));
                }
            }

            Log.info("[Main] UI setup complete"); // Log hoàn thành setup UI
        });

        Log.info("[Main] MindustryTool mod initialized"); // Log hoàn thành init
    }

    /**
     * Kiểm tra và thông báo cập nhật mod.
     * So sánh version hiện tại với version mới nhất trên server.
     */
    private void checkForUpdate() {
        // Lấy thông tin mod đang chạy
        LoadedMod mod = Vars.mods.getMod(Main.class);
        String currentVersion = mod.meta.version; // Version hiện tại

        // Gọi API để lấy version mới nhất
        Http.get(Config.API_REPO_URL, (res) -> {
            // Parse JSON response
            Jval json = Jval.read(res.getResultAsString());

            String latestVersion = json.getString("version"); // Version mới nhất

            // So sánh version
            if (!latestVersion.equals(currentVersion)) {
                // Có version mới
                Log.info("Mod require update, current version: @, latest version: @", currentVersion, latestVersion);

                // Hiển thị dialog confirm update
                Vars.ui.showConfirm(Core.bundle.format("message.new-version", currentVersion, latestVersion)
                        + "\nDiscord: https://discord.gg/72324gpuCd",
                        () -> {
                            // Nếu đang dùng v8, không tự update
                            if (currentVersion.endsWith("v8")) {
                                return;
                            }

                            // Import mod từ GitHub
                            Core.app.post(() -> {
                                Vars.ui.mods.githubImportMod(Config.REPO_URL, true, null);
                            });
                        });
            } else {
                // Mod đã up to date
                Log.info("Mod up to date");
            }
        });

        // Ping server để thống kê
        Http.get(Config.API_URL + "ping?client=mod-v8").submit(result -> {
            Log.info("Ping");
        });

    }
}
