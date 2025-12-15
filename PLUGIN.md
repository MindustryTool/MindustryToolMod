# MindustryTool Plugin Development Guide

Hướng dẫn tạo plugin cho MindustryTool Mod.

## Mục lục

- [Tổng quan](#tổng-quan)
- [Cấu trúc Plugin](#cấu-trúc-plugin)
- [Tạo Plugin mới](#tạo-plugin-mới)
- [Đăng ký Plugin](#đăng-ký-plugin)
- [Hệ thống Priority](#hệ-thống-priority)
- [Soft Dependencies](#soft-dependencies)
- [Các Pattern phổ biến](#các-pattern-phổ-biến)
- [Ví dụ Plugin hoàn chỉnh](#ví-dụ-plugin-hoàn-chỉnh)

---

## Tổng quan

MindustryTool sử dụng hệ thống plugin động (dynamic plugin system):

- **Độc lập**: Mỗi plugin hoạt động riêng biệt, xóa plugin này không ảnh hưởng plugin khác
- **Tự động load**: Chỉ cần thêm class name vào `plugins.txt`, không cần sửa code
- **Reflection-based**: Plugins được load bằng `Class.forName()` nên không có compile-time dependency

---

## Cấu trúc Plugin

### Plugin Interface

Mọi plugin phải implement interface `mindustrytool.Plugin`:

```java
public interface Plugin {
    /** Tên plugin (dùng cho logging). */
    String getName();
    
    /** Priority để xác định thứ tự load (cao hơn = load trước). Mặc định 0. */
    default int getPriority() { return 0; }
    
    /** Khởi tạo plugin. Được gọi khi mod init(). */
    void init();
    
    /** Giải phóng tài nguyên. Được gọi khi mod dispose(). */
    default void dispose() {}
}
```

### Cấu trúc thư mục

```
src/mindustrytool/plugins/
├── yourplugin/
│   ├── YourPlugin.java          # Main plugin class (implements Plugin)
│   ├── YourService.java         # Business logic
│   ├── YourDialog.java          # UI dialogs
│   └── ...                      # Các file khác
```

---

## Tạo Plugin mới

### Bước 1: Tạo package

Tạo package mới trong `src/mindustrytool/plugins/yourplugin/`

### Bước 2: Tạo Plugin class

```java
package mindustrytool.plugins.yourplugin;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.ClientLoadEvent;
import mindustrytool.Plugin;

public class YourPlugin implements Plugin {
    
    // Singleton pattern (tùy chọn, nhưng recommended)
    private static YourPlugin instance;
    private boolean initialized = false;
    
    public static YourPlugin getInstance() {
        if (instance == null) instance = new YourPlugin();
        return instance;
    }
    
    // REQUIRED: Tên plugin
    @Override 
    public String getName() { 
        return "YourPluginName"; 
    }
    
    // OPTIONAL: Priority (mặc định 0)
    @Override 
    public int getPriority() { 
        return 50; // Cao hơn = load trước
    }
    
    // REQUIRED: Khởi tạo plugin
    @Override 
    public void init() {
        if (initialized) return;
        
        Log.info("[YourPlugin] Initializing...");
        
        // Đăng ký UI khi game load xong
        Events.on(ClientLoadEvent.class, e -> {
            // Tạo UI, buttons, v.v.
        });
        
        initialized = true;
        Log.info("[YourPlugin] Ready!");
    }
    
    // OPTIONAL: Cleanup
    @Override 
    public void dispose() {
        Log.info("[YourPlugin] Disposing...");
        // Cleanup resources
    }
}
```

### Bước 3: Đăng ký plugin

Thêm class name vào file `assets/plugins.txt`:

```
mindustrytool.plugins.yourplugin.YourPlugin
```

Done! Plugin sẽ được tự động load khi mod khởi động.

---

## Đăng ký Plugin

File `assets/plugins.txt` chứa danh sách các plugin:

```properties
# Comment bắt đầu bằng #
# Mỗi dòng là 1 class name đầy đủ

mindustrytool.plugins.auth.AuthPlugin
mindustrytool.plugins.browser.BrowserPlugin
mindustrytool.plugins.yourplugin.YourPlugin
```

**Lưu ý:**
- Một class name mỗi dòng
- Dòng trống và dòng bắt đầu bằng `#` bị bỏ qua
- Plugin không tồn tại sẽ được skip (không crash)

---

## Hệ thống Priority

Priority quyết định thứ tự load plugin:

| Priority | Load Order | Ví dụ |
|----------|------------|-------|
| 80+ | Đầu tiên | Auth, Core services |
| 50-79 | Giữa | PlayerConnect, Browser |
| 0-49 | Cuối | Utilities, Optional features |

**Ví dụ:**
```java
@Override public int getPriority() { return 80; } // Load đầu tiên
@Override public int getPriority() { return 50; } // Load sau
@Override public int getPriority() { return 0; }  // Load cuối (mặc định)
```

**Sử dụng priority khi:**
- Plugin A cần được load trước Plugin B
- Plugin cung cấp service cho plugin khác nên có priority cao

---

## Soft Dependencies

### Vấn đề

Nếu Plugin B import trực tiếp Plugin A, khi xóa A thì B sẽ crash.

### Giải pháp: Dùng `Core.settings`

**Plugin A (provider)** - Lưu dữ liệu:
```java
// Trong AuthPlugin - khi login thành công
Core.settings.put("mindustrytool.auth.accessToken", token);

// Khi logout
Core.settings.remove("mindustrytool.auth.accessToken");
```

**Plugin B (consumer)** - Đọc dữ liệu:
```java
// Trong BrowserAuthService - tự tạo, không import AuthPlugin
public class BrowserAuthService {
    private static final String ACCESS_KEY = "mindustrytool.auth.accessToken";
    
    public static String getAccessToken() {
        // Trả về null nếu không có (AuthPlugin không tồn tại)
        return Core.settings.getString(ACCESS_KEY, null);
    }
    
    public static boolean hasAuth() {
        return getAccessToken() != null;
    }
}
```

**Kết quả:**
- Nếu AuthPlugin tồn tại → BrowserPlugin lấy được token
- Nếu AuthPlugin không tồn tại → `getAccessToken()` trả về `null`, Browser vẫn hoạt động với default API

### Kiểm tra Plugin tồn tại

```java
import mindustrytool.Main;

// Kiểm tra bằng class name (an toàn, không cần import)
if (Main.hasPlugin("mindustrytool.plugins.auth.AuthPlugin")) {
    // AuthPlugin đang hoạt động
}

// Hoặc lấy plugin instance (cần import, không khuyến khích)
AuthPlugin auth = Main.getPlugin(AuthPlugin.class);
if (auth != null) {
    // Sử dụng auth
}
```

---

## Các Pattern phổ biến

### 1. Tạo Dialog

```java
import mindustry.ui.dialogs.BaseDialog;
import mindustry.gen.Icon;

public class MyDialog extends BaseDialog {
    public MyDialog() {
        super("Dialog Title");
        
        cont.table(main -> {
            main.button("Click me", Icon.ok, () -> {
                // Action
            }).size(200, 50);
        });
        
        addCloseButton();
    }
}
```

### 2. Thêm Button vào Menu

```java
Events.on(ClientLoadEvent.class, e -> {
    Table menuTable = Vars.ui.menuGroup.find("menu");
    if (menuTable != null) {
        menuTable.row();
        menuTable.button("My Plugin", Icon.star, () -> {
            new MyDialog().show();
        }).width(200);
    }
});
```

### 3. HTTP Request

```java
import arc.util.Http;
import arc.util.serialization.Jval;

Http.get("https://api.example.com/data", response -> {
    Jval json = Jval.read(response.getResultAsString());
    String value = json.getString("key");
    // Xử lý dữ liệu
}, error -> {
    Log.err("Request failed: @", error.getMessage());
});
```

### 4. Inject vào JoinDialog

```java
public class JoinDialogInjector {
    public static void inject() {
        Events.on(ClientLoadEvent.class, e -> {
            // Thêm button vào Join dialog
            Vars.ui.join.buttons.row();
            Vars.ui.join.buttons.button("My Feature", Icon.star, () -> {
                // Action
            }).width(200);
        });
    }
}
```

---

## Ví dụ Plugin hoàn chỉnh

### HelloWorldPlugin.java

```java
package mindustrytool.plugins.helloworld;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Plugin;

public class HelloWorldPlugin implements Plugin {
    
    private static HelloWorldPlugin instance;
    private HelloDialog dialog;
    
    public static HelloWorldPlugin getInstance() {
        if (instance == null) instance = new HelloWorldPlugin();
        return instance;
    }
    
    @Override public String getName() { return "HelloWorld"; }
    @Override public int getPriority() { return 30; }
    
    @Override
    public void init() {
        dialog = new HelloDialog();
        
        Events.on(ClientLoadEvent.class, e -> {
            // Thêm button vào menu
            Vars.ui.menuGroup.fill(t -> {
                t.bottom().left();
                t.button("Hello", Icon.star, () -> dialog.show())
                    .size(100, 50).pad(10);
            });
        });
        
        Log.info("[HelloWorld] Plugin loaded!");
    }
    
    public void showDialog() {
        if (dialog != null) dialog.show();
    }
    
    // Inner Dialog class
    static class HelloDialog extends BaseDialog {
        HelloDialog() {
            super("Hello World");
            cont.add("Chào mừng đến với MindustryTool!").pad(20);
            addCloseButton();
        }
    }
}
```

### Đăng ký

Thêm vào `assets/plugins.txt`:
```
mindustrytool.plugins.helloworld.HelloWorldPlugin
```

---

## API Reference

### Main class

```java
// Lấy tất cả plugins đã load
Seq<Plugin> plugins = Main.getPlugins();

// Lấy plugin theo type (cần import class)
AuthPlugin auth = Main.getPlugin(AuthPlugin.class);

// Kiểm tra plugin tồn tại (không cần import)
boolean hasAuth = Main.hasPlugin("mindustrytool.plugins.auth.AuthPlugin");
```

### Directories

```java
// Thư mục cache của mod
Fi imageDir = Main.imageDir;    // mindustry-tool-caches
Fi mapsDir = Main.mapsDir;      // mindustry-tool-maps  
Fi schematicDir = Main.schematicDir; // mindustry-tool-schematics
```

---

## Tips & Best Practices

1. **Luôn dùng Soft Dependencies** - Không import plugin khác trực tiếp
2. **Singleton Pattern** - Giúp truy cập plugin từ mọi nơi
3. **Đợi ClientLoadEvent** - UI chỉ khả dụng sau khi game load xong
4. **Priority hợp lý** - Services/Auth nên có priority cao
5. **Defensive coding** - Luôn kiểm tra null khi dùng soft dependencies
6. **Log đầy đủ** - Giúp debug dễ dàng

---

## Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| Plugin không load | Kiểm tra class name trong plugins.txt |
| ClassNotFoundException | Đảm bảo package path đúng |
| NullPointerException | Kiểm tra soft dependency trả về null |
| UI không hiện | Đảm bảo dùng `Events.on(ClientLoadEvent.class, ...)` |

---

## License

MIT License - Tự do sử dụng và phát triển plugin.
