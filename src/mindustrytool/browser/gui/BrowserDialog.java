package mindustrytool.browser.gui; // Khai báo package chứa các GUI components

import arc.Core; // Import Core để truy cập graphics, bundle, app
import arc.scene.ui.Button; // Import Button widget
import arc.scene.ui.Label; // Import Label widget
import arc.scene.ui.ScrollPane; // Import ScrollPane widget
import arc.scene.ui.TextButton; // Import TextButton widget
import arc.scene.ui.TextField; // Import TextField widget
import arc.scene.ui.layout.Cell; // Import Cell để config layout
import arc.scene.ui.layout.Scl; // Import Scl để scale theo DPI
import arc.scene.ui.layout.Table; // Import Table - container layout
import arc.struct.ObjectMap; // Import ObjectMap - HashMap của Arc
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.util.Log; // Import Log để ghi log lỗi

import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.gen.Tex; // Import Tex cho textures
import mindustry.ui.Styles; // Import Styles cho UI styles
import mindustry.ui.dialogs.BaseDialog; // Import BaseDialog base class

import mindustrytool.browser.BrowserConfig; // Import BrowserConfig cho API URL
import mindustrytool.browser.data.DataMap; // Import DataMap data class
import mindustrytool.browser.data.DataSchematic; // Import DataSchematic data class
import mindustrytool.browser.data.PagingRequest; // Import PagingRequest helper
import mindustrytool.browser.data.SearchConfig; // Import SearchConfig
import mindustrytool.browser.data.TagService; // Import TagService
import mindustrytool.browser.gui.browser.BrowserItem; // Import BrowserItem interface
import mindustrytool.browser.gui.browser.BrowserItemHandler; // Import BrowserItemHandler interface
import mindustrytool.browser.gui.browser.BrowserType; // Import BrowserType enum
import mindustrytool.browser.gui.browser.ItemCard; // Import ItemCard UI component
import mindustrytool.browser.gui.browser.MapItemHandler; // Import MapItemHandler
import mindustrytool.browser.gui.browser.SchematicItemHandler; // Import SchematicItemHandler
import mindustrytool.config.Debouncer; // Import Debouncer utility

import static mindustry.Vars.*; // Static import các Vars thường dùng

import java.util.concurrent.TimeUnit; // Import TimeUnit cho debouncer

/**
 * Dialog dùng để browse Maps hoặc Schematics từ online API.
 * Chứa search bar, tag/filter controls, responsive results grid
 * và pagination/upload actions.
 * Dialog này được chia sẻ giữa map và schematic browsing
 * thông qua generic type parameter.
 * @param <T> Loại BrowserItem (DataMap hoặc DataSchematic)
 */
public class BrowserDialog<T extends BrowserItem> extends BaseDialog { // Dialog chính cho browser

    // --- Browser Dialog UI ---
    // Chịu trách nhiệm render search bar, tag/filter bar, results grid, và footer (pagination/upload)
    // Hỗ trợ cả Map và Schematic browsing (responsive)

    // Loại browser (MAP hoặc SCHEMATIC)
    private final BrowserType type;
    // Class của data items
    private final Class<T> dataClass;
    // Handler xử lý actions cho items
    private final BrowserItemHandler<T> itemHandler;

    // Dữ liệu items hiện tại
    private Seq<T> data = new Seq<>();
    // Debouncer để tránh gọi API quá nhiều khi search
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);

    // Kích thước image trong card
    private final float IMAGE_SIZE = Scl.scl(210f);
    // Chiều cao phần info table
    private final float INFO_TABLE_HEIGHT = Scl.scl(60f);

    // Config tìm kiếm (shared static để giữ state)
    private static SearchConfig searchConfig = new SearchConfig();
    // Service để load tags
    private final TagService tagService = new TagService();
    // Dialog filter/tag
    private final FilterDialog filterDialog;

    // Chuỗi tìm kiếm hiện tại
    private String search = "";

    // TextField tìm kiếm
    TextField searchField;

    // Helper để thực hiện paged requests
    private PagingRequest<T> request;
    // Options gửi lên API (sort, tags, verification, etc.)
    private ObjectMap<String, String> options = new ObjectMap<>();

    /**
     * Constructor tạo BrowserDialog.
     * @param type Loại browser (MAP hoặc SCHEMATIC)
     */
    @SuppressWarnings("unchecked") // Suppress warning cho cast generic
    public BrowserDialog(BrowserType type) {
        super(type.title); // Gọi constructor cha với title
        this.type = type; // Lưu type

        // Khởi tạo dataClass và itemHandler dựa vào type
        if (type == BrowserType.MAP) {
            this.dataClass = (Class<T>) DataMap.class; // Cast sang Class<T>
            this.itemHandler = (BrowserItemHandler<T>) new MapItemHandler(); // Cast handler
        } else {
            this.dataClass = (Class<T>) DataSchematic.class; // Cast sang Class<T>
            this.itemHandler = (BrowserItemHandler<T>) new SchematicItemHandler(); // Cast handler
        }

        // Tạo filter dialog với tag provider
        filterDialog = new FilterDialog(tagService, searchConfig,
                tag -> tagService.getTag(type.tagCategory, tag::get));

        // Tạo PagingRequest với class và endpoint
        request = new PagingRequest<>(dataClass, BrowserConfig.API_URL + type.endpoint);

        // Tính và set số items mỗi trang
        setItemPerPage();

        // Set options mặc định
        options.put("sort", searchConfig.getSort().getValue()); // Sort mặc định
        options.put("verification", "VERIFIED"); // Chỉ lấy verified items

        request.setOptions(options); // Apply options

        // Handler khi đóng filter dialog
        filterDialog.hidden(() -> {
            Core.app.post(() -> {
                // Nếu config đã thay đổi, refresh data
                if (searchConfig.isChanged()) {
                    searchConfig.update(); // Reset changed flag
                    options.put("tags", searchConfig.getSelectedTagsString()); // Update tags
                    options.put("sort", searchConfig.getSort().getValue()); // Update sort
                    request.setPage(0); // Reset về trang đầu
                    request.getPage(this::handleResult); // Fetch data mới
                }
            });
        });

        // Handler khi resize màn hình
        onResize(() -> {
            setItemPerPage(); // Tính lại số items/page
            buildBrowser(); // Rebuild UI
            // Nếu filter dialog đang show, rebuild nó
            if (filterDialog.isShown()) {
                filterDialog.show(searchConfig);
            }
        });

        // Fetch data lần đầu
        request.getPage(this::handleResult);
        // Build browser khi dialog shown
        shown(this::buildBrowser);
    }

    /**
     * Tính và set số items mỗi trang dựa vào kích thước màn hình.
     */
    private void setItemPerPage() {
        // Tính số cột dựa vào width
        int columns = (int) (Core.graphics.getWidth() / Scl.scl() * 0.9f / IMAGE_SIZE) - 1;
        // Tính số hàng dựa vào height
        int rows = (int) (Core.graphics.getHeight() / Scl.scl(IMAGE_SIZE + INFO_TABLE_HEIGHT * 2));
        // Tổng số items, tối thiểu 20
        int size = Math.max(columns * rows, 20);

        request.setItemPerPage(size); // Set cho request
    }

    // --- Layout Assembly ---
    // Build top-level dialog layout: close button, search bar, content container và footer
    /**
     * Build toàn bộ UI của browser.
     */
    private void buildBrowser() {
        clear(); // Xóa nội dung cũ

        try {
            addCloseButton(); // Thêm nút close
            row(); // Xuống hàng
            buildSearchBar(); // Build search bar
            row(); // Xuống hàng
            buildContainer(); // Build main container
            row(); // Xuống hàng
            buildFooter(); // Build footer
        } catch (Exception ex) {
            // Nếu có lỗi, hiển thị error UI
            clear();
            addCloseButton();
            table(c -> buildError(c, Core.bundle.format("message.error") + "\n Error: " + ex.getMessage()));
            Log.err(ex); // Log lỗi
        }
    }

    /**
     * Wrapper để kiểm tra loading trước khi thực hiện action.
     * @param action Action cần thực hiện
     */
    private void loadingWrapper(Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading())
                ui.showInfoFade("Loading"); // Hiển thị thông báo loading
            else
                action.run(); // Thực hiện action
        });
    }

    // --- Search Bar UI ---
    // Back button, search field, filter/refresh buttons và tag bar
    // Search input debounces requests để tránh gọi API quá nhiều
    /**
     * Build search bar UI.
     */
    private void buildSearchBar() {
        // Table chứa search bar
        table(searchBar -> {
            // Nút back để đóng dialog
            searchBar.button("@back", Icon.leftSmall, this::hide)
                    .width(150).padLeft(2).padRight(2);

            // Wrapper chứa text field
            searchBar.table(wrapper -> {
                wrapper.left(); // Căn trái
                // Text field tìm kiếm
                searchField = wrapper.field(search, result -> {
                    search = result; // Lưu giá trị search
                    options.put("name", result); // Update option name
                    request.setPage(0); // Reset về trang đầu
                    // Debounce để tránh gọi API quá nhiều
                    debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleResult)));
                }).growX().get();

                searchField.setMessageText(type.searchHint); // Set placeholder
            }).fillX().expandX().padBottom(2).padLeft(2).padRight(2);

            // Nút filter - mở filter dialog
            searchBar.button(Icon.filterSmall,
                    () -> loadingWrapper(() -> filterDialog.show(searchConfig)))
                    .padLeft(2).padRight(2).width(60);
            // Nút refresh - fetch lại data
            searchBar.button(Icon.zoomSmall,
                    () -> loadingWrapper(() -> request.getPage(this::handleResult)))
                    .padLeft(2).padRight(2).width(60);

        }).fillX().expandX(); // Fill width

        row(); // Xuống hàng

        // Scroll pane chứa tag bar
        pane(tagBar -> {
            // Draw tag bar với callback khi tag thay đổi
            TagBar.draw(tagBar, searchConfig, config -> {
                options.put("tags", config.getSelectedTagsString()); // Update tags
                request.setPage(0); // Reset về trang đầu
                // Debounce fetch data
                debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleResult)));
                buildBrowser(); // Rebuild UI
            });
        }).scrollY(false); // Không scroll dọc
    }

    // --- Error UI ---
    // Hiển thị button để retry loading data
    /**
     * Build error UI với message và retry button.
     * @param parent Table cha
     * @param message Thông báo lỗi
     * @return Cell của button
     */
    private Cell<TextButton> buildError(Table parent, String message) {
        return parent.button(message, Styles.nonet, () -> request.getPage(this::handleResult))
                .center().labelAlign(0).expand().fill();
    }

    // --- Loading UI ---
    // Label loading đơn giản khi request đang chạy
    /**
     * Build loading UI.
     * @param parent Table cha
     * @return Cell của label
     */
    private Cell<Label> buildLoading(Table parent) {
        return parent.labelWrap(Core.bundle.format("message.loading"))
                .center().labelAlign(0).expand().fill();
    }

    // --- Results Grid / Scroll Container ---
    // Tạo grid các ItemCard entries trong ScrollPane
    /**
     * Build scroll container chứa grid items.
     * @param parent Table cha
     * @return Cell của ScrollPane
     */
    private Cell<ScrollPane> buildScrollContainer(Table parent) {
        // Nếu không có data, hiển thị thông báo
        if (data.size == 0) {
            return parent.pane(c -> c.add("message.no-result"));
        }

        // Scroll pane chứa grid
        return parent.pane(container -> {
            float sum = 0; // Tổng width đã dùng trong row hiện tại

            // Duyệt qua từng item
            for (T item : data) {
                // Nếu sắp tràn width, xuống hàng mới
                if (sum + IMAGE_SIZE * 2 >= Core.graphics.getWidth()) {
                    container.row();
                    sum = 0;
                }

                try {
                    // Tạo ItemCard và thêm vào container
                    Button button = ItemCard.create(container, item, itemHandler, this::hide);
                    sum += button.getPrefWidth(); // Cộng width vào sum
                } catch (Exception e) {
                    e.printStackTrace(); // Log lỗi
                }
            }
            container.top(); // Căn top
        }).scrollY(true).expand().fill(); // Scroll dọc, expand và fill
    }

    // --- Footer UI (Pagination & Actions) ---
    // Previous, direct page select, next và upload buttons
    /**
     * Build footer UI với pagination và actions.
     */
    private void buildFooter() {
        // Table chứa footer
        table(footer -> {
            // Nút previous page
            footer.button(Icon.left, () -> request.previousPage(this::handleResult))
                    .margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.getPage() == 0 || request.isError()) // Disable khi không thể
                    .height(40);

            // Hiển thị số trang hiện tại
            footer.table(Tex.buttonDisabled, table -> {
                table.labelWrap(String.valueOf(request.getPage() + 1)) // Page + 1 (hiển thị 1-indexed)
                        .width(50)
                        .style(Styles.defaultLabel)
                        .labelAlign(0)
                        .center()
                        .fill();
            }).pad(4).height(40);

            // Nút chọn trang trực tiếp
            footer.button(Icon.edit, () -> {
                // Hiển thị input dialog
                ui.showTextInput("@select-page", "", "", input -> {
                    try {
                        request.setPage(Integer.parseInt(input)); // Parse và set page
                        shown(this::buildBrowser); // Rebuild
                    } catch (Exception e) {
                        ui.showInfo("Invalid input"); // Thông báo input không hợp lệ
                    }
                });
            })
                    .margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError())
                    .height(40);

            // Nút next page
            footer.button(Icon.right, () -> request.nextPage(this::handleResult))
                    .margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError())
                    .height(40);

            // Nút upload - mở URL upload trên web
            footer.button("@upload", () -> Core.app.openURI(type.uploadUrl))
                    .margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError())
                    .height(40);

            footer.bottom(); // Căn bottom
        }).expandX().fillX(); // Expand và fill X
    }

    // --- Main Container ---
    // Quyết định hiển thị loading, error hoặc results container
    /**
     * Build main container dựa vào state hiện tại.
     * @return Cell của Table container
     */
    private Cell<Table> buildContainer() {
        return table(container -> {
            // Nếu đang loading
            if (request.isLoading()) {
                buildLoading(container); // Hiển thị loading
                return;
            }

            // Nếu có lỗi
            if (request.isError()) {
                // Hiển thị error với message
                buildError(container, String.format("There is an error, reload? (%s)", request.getError()));
                return;
            }

            // Không loading và không error - hiển thị results
            buildScrollContainer(container);
        }).margin(0).expand().fill().top(); // Không margin, expand, fill, căn top
    }

    // --- Data Handling ---
    // Update local data reference và rebuild UI
    /**
     * Xử lý kết quả từ API.
     * @param result Kết quả từ PagingRequest
     */
    private void handleResult(Seq<T> result) {
        // Nếu result không null, lưu lại
        if (result != null)
            this.data = result;
        else
            this.data.clear(); // Nếu null, clear data

        buildBrowser(); // Rebuild UI
    }
}
