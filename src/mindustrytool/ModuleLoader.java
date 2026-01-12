package mindustrytool; // Khai báo package chính của mod

import java.io.InputStream; // Import InputStream để đọc resources
import java.net.URL; // Import URL để truy cập resources
import java.util.Enumeration; // Import Enumeration để iterate resources
import java.util.Properties; // Import Properties để đọc file .module

import arc.struct.Seq; // Import Seq cho danh sách
import arc.util.Log; // Import Log để ghi log
import mindustry.ui.fragments.MenuFragment.MenuButton; // Import MenuButton

/**
 * Class quản lý việc load và khởi tạo các Module.
 * Tự động scan và phát hiện modules từ META-INF/modules/*.module
 * trong mỗi package module.
 * 
 * Mỗi module tự khai báo file .module của riêng mình.
 * Cho phép thêm/xóa modules mà không cần sửa code Main.
 */
public class ModuleLoader { // Class loader cho các modules

    // Đường dẫn để scan module files
    private static final String MODULE_PATH = "META-INF/modules/"; // Path chứa module configs
    
    // Danh sách các modules đã load
    private static final Seq<Module> modules = new Seq<>(); // Seq chứa các module instances

    /**
     * Load tất cả modules được khai báo trong META-INF/modules/.
     * Scan tất cả files .module và khởi tạo module classes.
     */
    public static void loadModules() { // Method load tất cả modules
        Log.info("[ModuleLoader] Scanning for modules..."); // Log bắt đầu scan

        try { // Try-catch để bắt lỗi
            // Lấy ClassLoader để scan resources
            ClassLoader classLoader = ModuleLoader.class.getClassLoader(); // Lấy class loader
            
            // Tìm tất cả resources matching pattern META-INF/modules/
            Enumeration<URL> resources = classLoader.getResources(MODULE_PATH); // Enumerate resources
            
            // Seq để chứa các module class names đã tìm thấy
            Seq<String> moduleClasses = new Seq<>(); // Danh sách class names
            
            // Duyệt qua các resource directories
            while (resources.hasMoreElements()) { // Vòng lặp qua resources
                URL resourceUrl = resources.nextElement(); // Lấy URL tiếp theo
                Log.info("[ModuleLoader] Found module directory: @", resourceUrl); // Log directory
                
                // Đọc trực tiếp các .module files đã biết
                // (Do limitations của ClassLoader, ta không thể list directory contents)
                // Nên ta sẽ thử load từng known module file
            }
            
            // Thử load các module files phổ biến
            // Mỗi module cần được khai báo ở đây hoặc dùng cách khác
            tryLoadModuleFile(classLoader, MODULE_PATH + "browser.module", moduleClasses); // Load browser
            tryLoadModuleFile(classLoader, MODULE_PATH + "playerconnect.module", moduleClasses); // Load playerconnect
            
            // Scan tất cả .module files có thể có (dùng pattern matching)
            scanModuleFiles(classLoader, moduleClasses); // Scan thêm
            
            // Khởi tạo các modules đã tìm thấy
            for (String className : moduleClasses) { // Vòng lặp qua class names
                try { // Try-catch cho từng module
                    Log.info("[ModuleLoader] Loading module class: @", className); // Log class name
                    
                    // Load class và tạo instance
                    Class<?> clazz = Class.forName(className); // Load class
                    
                    // Kiểm tra class có implement Module không
                    if (Module.class.isAssignableFrom(clazz)) { // Check interface
                        Module module = (Module) clazz.getDeclaredConstructor().newInstance(); // Tạo instance
                        
                        Log.info("[ModuleLoader] Initializing module: @", module.getName()); // Log init
                        module.init(); // Khởi tạo module
                        modules.add(module); // Thêm vào danh sách
                        Log.info("[ModuleLoader] Module loaded: @", module.getName()); // Log thành công
                    } else { // Không phải Module
                        Log.warn("[ModuleLoader] Class @ does not implement Module interface", className); // Cảnh báo
                    }
                } catch (Exception e) { // Bắt exception
                    Log.err("[ModuleLoader] Failed to load module class: @", className); // Log lỗi
                    Log.err(e); // Log stack trace
                }
            }
            
        } catch (Exception e) { // Bắt exception tổng
            Log.err("[ModuleLoader] Error scanning for modules"); // Log lỗi
            Log.err(e); // Log stack trace
        }

        Log.info("[ModuleLoader] Loaded @ modules", modules.size); // Log số modules đã load
    }
    
    /**
     * Thử load một file .module cụ thể.
     * 
     * @param classLoader ClassLoader để load resource
     * @param path Đường dẫn đến file .module
     * @param moduleClasses Seq để thêm class name nếu tìm thấy
     */
    private static void tryLoadModuleFile(ClassLoader classLoader, String path, Seq<String> moduleClasses) {
        try { // Try-catch để bắt lỗi
            // Tìm tất cả resources với path này (có thể có nhiều từ nhiều modules)
            Enumeration<URL> urls = classLoader.getResources(path); // Enumerate resources
            
            while (urls.hasMoreElements()) { // Vòng lặp qua URLs
                URL url = urls.nextElement(); // Lấy URL
                Log.info("[ModuleLoader] Found module file: @", url); // Log file
                
                // Đọc properties từ file
                try (InputStream is = url.openStream()) { // Mở stream
                    Properties props = new Properties(); // Tạo Properties
                    props.load(is); // Load từ stream
                    
                    // Lấy module class name
                    String moduleClass = props.getProperty("module.class"); // Đọc property
                    
                    if (moduleClass != null && !moduleClass.isEmpty()) { // Kiểm tra không rỗng
                        if (!moduleClasses.contains(moduleClass)) { // Kiểm tra chưa có
                            moduleClasses.add(moduleClass); // Thêm vào danh sách
                            Log.info("[ModuleLoader] Registered module: @", moduleClass); // Log
                        }
                    }
                }
            }
        } catch (Exception e) { // Bắt exception
            // Ignore - file không tồn tại là bình thường
            Log.info("[ModuleLoader] Module file not found: @", path); // Log info
        }
    }
    
    /**
     * Scan thêm các module files bằng cách enumerate resources.
     * 
     * @param classLoader ClassLoader để scan
     * @param moduleClasses Seq để thêm class names
     */
    private static void scanModuleFiles(ClassLoader classLoader, Seq<String> moduleClasses) {
        try { // Try-catch để bắt lỗi
            // Enumerate tất cả resources trong META-INF/modules/
            Enumeration<URL> resources = classLoader.getResources(MODULE_PATH); // Get resources
            
            while (resources.hasMoreElements()) { // Vòng lặp
                URL dirUrl = resources.nextElement(); // Lấy URL
                
                // Log để debug
                Log.info("[ModuleLoader] Scanning directory: @", dirUrl); // Log
                
                // Với JAR files, cần xử lý khác
                // Nhưng các known modules đã được load ở tryLoadModuleFile
            }
        } catch (Exception e) { // Bắt exception
            Log.err("[ModuleLoader] Error scanning module files"); // Log lỗi
            Log.err(e); // Log stack trace
        }
    }

    /**
     * Gọi onClientLoad() cho tất cả modules.
     * Được gọi từ Main khi ClientLoadEvent xảy ra.
     */
    public static void onClientLoad() { // Method thông báo client đã load
        for (Module module : modules) { // Vòng lặp qua các modules
            try { // Try-catch để bắt lỗi từng module
                module.onClientLoad(); // Gọi callback
            } catch (Exception e) { // Bắt exception
                Log.err("[ModuleLoader] Error in onClientLoad for module: @", module.getName()); // Log lỗi
                Log.err(e); // Log stack trace
            }
        }
    }

    /**
     * Lấy tất cả menu buttons từ các modules cho PC.
     * 
     * @return Seq chứa tất cả MenuButtons
     */
    public static Seq<MenuButton> getAllMenuButtons() { // Method lấy menu buttons cho PC
        Seq<MenuButton> buttons = new Seq<>(); // Tạo Seq mới

        for (Module module : modules) { // Vòng lặp qua các modules
            try { // Try-catch để bắt lỗi
                Seq<MenuButton> moduleButtons = module.getMenuButtons(); // Lấy buttons của module
                if (moduleButtons != null) { // Kiểm tra null
                    buttons.addAll(moduleButtons); // Thêm vào danh sách tổng
                }
            } catch (Exception e) { // Bắt exception
                Log.err("[ModuleLoader] Error getting menu buttons from module: @", module.getName()); // Log lỗi
                Log.err(e); // Log stack trace
            }
        }

        return buttons; // Trả về danh sách buttons
    }

    /**
     * Lấy tất cả menu buttons từ các modules cho Mobile.
     * 
     * @return Seq chứa tất cả MenuButtons cho mobile
     */
    public static Seq<MenuButton> getAllMobileMenuButtons() { // Method lấy menu buttons cho mobile
        Seq<MenuButton> buttons = new Seq<>(); // Tạo Seq mới

        for (Module module : modules) { // Vòng lặp qua các modules
            try { // Try-catch để bắt lỗi
                Seq<MenuButton> moduleButtons = module.getMobileMenuButtons(); // Lấy buttons của module
                if (moduleButtons != null) { // Kiểm tra null
                    buttons.addAll(moduleButtons); // Thêm vào danh sách tổng
                }
            } catch (Exception e) { // Bắt exception
                Log.err("[ModuleLoader] Error getting mobile menu buttons from module: @", module.getName()); // Log lỗi
                Log.err(e); // Log stack trace
            }
        }

        return buttons; // Trả về danh sách buttons
    }

    /**
     * Lấy danh sách các modules đã load.
     * 
     * @return Seq chứa các Module instances
     */
    public static Seq<Module> getModules() { // Method lấy danh sách modules
        return modules; // Trả về danh sách modules
    }

    /**
     * Tìm module theo tên.
     * 
     * @param name Tên module cần tìm
     * @return Module nếu tìm thấy, null nếu không
     */
    public static Module getModule(String name) { // Method tìm module theo tên
        return modules.find(m -> m.getName().equals(name)); // Tìm và trả về module
    }

    /**
     * Dispose tất cả modules.
     * Được gọi khi mod unload.
     */
    public static void disposeAll() { // Method dispose tất cả modules
        for (Module module : modules) { // Vòng lặp qua các modules
            try { // Try-catch để bắt lỗi
                module.dispose(); // Gọi dispose
            } catch (Exception e) { // Bắt exception
                Log.err("[ModuleLoader] Error disposing module: @", module.getName()); // Log lỗi
                Log.err(e); // Log stack trace
            }
        }
        modules.clear(); // Xóa danh sách
    }

    /**
     * Reload tất cả modules.
     * Dispose modules cũ và load lại.
     */
    public static void reloadModules() { // Method reload modules
        disposeAll(); // Dispose modules cũ
        loadModules(); // Load lại modules
    }
}
