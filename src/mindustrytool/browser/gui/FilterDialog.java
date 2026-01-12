package mindustrytool.browser.gui; // Khai báo package chứa các GUI components

import arc.Core; // Import Core để truy cập graphics, bundle
import arc.func.Cons; // Import interface callback function
import arc.scene.ui.ButtonGroup; // Import ButtonGroup để group radio buttons
import arc.scene.ui.TextButton.TextButtonStyle; // Import TextButtonStyle
import arc.scene.ui.layout.Table; // Import Table - container layout
import arc.struct.Seq; // Import Seq - mảng động của Arc
import arc.util.Align; // Import Align constants
import arc.util.Log; // Import Log để ghi log lỗi

import mindustry.Vars; // Import Vars để check mobile
import mindustry.ui.Styles; // Import Styles cho UI styles
import mindustry.ui.dialogs.BaseDialog; // Import BaseDialog base class

import mindustrytool.browser.BrowserConfig; // Import BrowserConfig cho SORTS
import mindustrytool.browser.data.DataMod; // Import DataMod data class
import mindustrytool.browser.data.ModService; // Import ModService
import mindustrytool.browser.data.SearchConfig; // Import SearchConfig
import mindustrytool.browser.data.TagCategory; // Import TagCategory data class
import mindustrytool.browser.data.TagService; // Import TagService

/**
 * Dialog cho việc chọn tags, mod filters, và sorting options.
 * Lắng nghe TagService và ModService updates để rebuild khi cần.
 */
public class FilterDialog extends BaseDialog { // Extends BaseDialog

    // Style cho các toggle buttons
    private TextButtonStyle style = Styles.togglet;
    // Provider để lấy tag categories
    private final Cons<Cons<Seq<TagCategory>>> tagProvider;
    // Tỷ lệ scale (nhỏ hơn trên mobile)
    private float scale = 1;
    // Số cột hiển thị
    private int cols = 1;
    // Kích thước mỗi card
    private int cardSize = 0;
    // Khoảng cách giữa các card
    private final int CARD_GAP = 4;
    // Danh sách mod IDs đã chọn để filter
    private Seq<String> modIds = new Seq<>();

    // Service để load mods
    private ModService modService = new ModService();
    // Service để load tags
    private final TagService tagService;

    /**
     * Constructor tạo FilterDialog.
     * @param tagService Service quản lý tags
     * @param searchConfig Config tìm kiếm hiện tại
     * @param tagProvider Provider để lấy tag categories
     */
    public FilterDialog(TagService tagService, SearchConfig searchConfig, Cons<Cons<Seq<TagCategory>>> tagProvider) {
        super(""); // Gọi constructor cha với title rỗng

        this.tagService = tagService; // Lưu tag service
        setFillParent(true); // Fill toàn màn hình
        addCloseListener(); // Thêm listener đóng dialog

        this.tagProvider = tagProvider; // Lưu tag provider

        // Handler khi resize màn hình
        onResize(() -> {
            if (searchConfig != null) {
                show(searchConfig); // Rebuild lại UI
            }
        });
    }

    /**
     * Hiển thị dialog với searchConfig hiện tại.
     * @param searchConfig Config tìm kiếm
     */
    public void show(SearchConfig searchConfig) {
        // Đăng ký callback khi mod service update
        modService.onUpdate(() -> {
            show(searchConfig); // Rebuild lại UI
        });

        // Đăng ký callback khi tag service update
        tagService.onUpdate(() -> show(searchConfig));

        try {
            // Tính scale dựa vào mobile hay không
            scale = Vars.mobile ? 0.8f : 1f;
            // Tính kích thước card
            cardSize = (int) (300 * scale);
            // Tính số cột dựa vào width màn hình
            cols = (int) Math.max(Math.floor(Core.graphics.getWidth() / (cardSize + CARD_GAP)), 1);

            // Xóa nội dung cũ
            cont.clear();
            // Tạo scroll pane chứa nội dung
            cont.pane(table -> {
                // Load và hiển thị mod selector
                modService.getMod(mods -> ModSelector(table, searchConfig, mods));

                table.row(); // Xuống hàng
                // Hiển thị sort selector
                SortSelector(table, searchConfig);
                table.row(); // Xuống hàng
                table.top(); // Căn top

                // Load và hiển thị tag selectors
                tagProvider.get(categories -> {
                    // Duyệt qua từng category đã sort theo position
                    for (var category : categories.sort((a, b) -> a.position() - b.position())) {
                        // Bỏ qua category rỗng
                        if (category.tags().isEmpty())
                            continue;

                        table.row(); // Xuống hàng
                        TagSelector(table, searchConfig, category); // Hiển thị tag selector
                    }
                });
            })//
                    .padLeft(20)// Padding trái 20
                    .padRight(20)// Padding phải 20
                    .scrollY(true)// Cho phép scroll dọc
                    .expand()// Mở rộng
                    .fill()// Fill container
                    .left()// Căn trái
                    .top(); // Căn top

            cont.row(); // Xuống hàng
            // Xóa và setup lại buttons footer
            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);

            addCloseButton(); // Thêm nút đóng
            show(); // Hiển thị dialog
        } catch (Exception e) {
            Log.err(e); // Log lỗi nếu có
        }
    }

    /**
     * Tạo UI cho mod selector.
     * @param table Table chứa nội dung
     * @param searchConfig Config tìm kiếm
     * @param mods Danh sách mods
     */
    public void ModSelector(Table table, SearchConfig searchConfig, Seq<DataMod> mods) {
        // Header "Mod"
        table.table(Styles.flatOver,
                text -> text.add(Core.bundle.format("messagemod"))// Lấy text từ bundle
                        .fontScale(scale)// Scale font
                        .left()// Căn trái
                        .labelAlign(Align.left))// Align text trái
                .top()// Căn top
                .left()// Căn trái
                .expandX() // Mở rộng theo X
                .padBottom(4); // Padding bottom 4

        table.row(); // Xuống hàng
        // Scroll pane chứa các mod buttons
        table.pane(card -> {
            card.defaults().size(cardSize, 50); // Size mặc định cho buttons
            int i = 0; // Counter cho columns
            // Duyệt qua các mods đã sort
            for (var mod : mods.sort((a, b) -> a.position() - b.position())) {
                // Tạo button cho mỗi mod
                card.button(btn -> {
                    btn.left(); // Căn trái
                    // Nếu có icon, hiển thị
                    if (mod.icon() != null && !mod.icon().isEmpty()) {
                        btn.add(new NetworkImage(mod.icon()))
                                .size(40 * scale) // Size theo scale
                                .padRight(4)
                                .marginRight(4);
                    }
                    btn.add(mod.name()).fontScale(scale); // Tên mod
                }, style, // Toggle style
                        () -> {
                            // Toggle mod selection
                            if (modIds.contains(mod.id())) {
                                modIds.remove(mod.id()); // Bỏ chọn
                            } else {
                                modIds.add(mod.id()); // Chọn
                            }
                            Core.app.post(() -> show(searchConfig)); // Rebuild UI
                        })//
                        .checked(modIds.contains(mod.id()))// Checked nếu đã chọn
                        .padRight(CARD_GAP)// Padding phải
                        .padBottom(CARD_GAP)// Padding dưới
                        .left()// Căn trái
                        .fillX()// Fill X
                        .margin(12); // Margin 12

                // Xuống hàng sau mỗi `cols` buttons
                if (++i % cols == 0) {
                    card.row();
                }
            }
        })//
                .top()// Căn top
                .left()// Căn trái
                .expandX() // Mở rộng X
                .scrollY(false)// Không scroll Y
                .padBottom(48); // Padding bottom 48
    }

    /**
     * Tạo UI cho sort selector.
     * @param table Table chứa nội dung
     * @param searchConfig Config tìm kiếm
     */
    public void SortSelector(Table table, SearchConfig searchConfig) {
        // Button group để chỉ cho chọn 1
        var buttonGroup = new ButtonGroup<>();

        // Header "Sort"
        table.table(Styles.flatOver,
                text -> text.add(Core.bundle.format("message.sort"))// Lấy text từ bundle
                        .fontScale(scale)// Scale font
                        .left()// Căn trái
                        .labelAlign(Align.left))// Align text trái
                .top()// Căn top
                .left()// Căn trái
                .expandX() // Mở rộng X
                .padBottom(4); // Padding bottom 4

        table.row(); // Xuống hàng
        // Scroll pane chứa các sort buttons
        table.pane(card -> {
            card.defaults().size(cardSize, 50); // Size mặc định
            int i = 0; // Counter cho columns
            // Duyệt qua các sort options
            for (var sort : BrowserConfig.SORTS) {
                // Tạo button cho mỗi sort
                card.button(btn -> btn.add(formatTag(sort.getName())).fontScale(scale)//
                        , style, () -> {
                            searchConfig.setSort(sort); // Set sort khi click
                        })//
                        .group(buttonGroup)// Add vào button group
                        .checked(sort.equals(searchConfig.getSort()))// Checked nếu đang active
                        .padRight(CARD_GAP)// Padding phải
                        .padBottom(CARD_GAP); // Padding dưới

                // Xuống hàng sau mỗi `cols` buttons
                if (++i % cols == 0) {
                    card.row();
                }
            }
        })//
                .top()// Căn top
                .left()// Căn trái
                .expandX() // Mở rộng X
                .scrollY(false)// Không scroll Y
                .padBottom(48); // Padding bottom 48
    }

    /**
     * Tạo UI cho tag selector của một category.
     * @param table Table chứa nội dung
     * @param searchConfig Config tìm kiếm
     * @param category TagCategory cần hiển thị
     */
    public void TagSelector(Table table, SearchConfig searchConfig, TagCategory category) {
        // Header với tên category
        table.table(Styles.flatOver,
                text -> text.add(category.name())// Tên category
                        .fontScale(scale)// Scale font
                        .left() // Căn trái
                        .labelAlign(Align.left))// Align text trái
                .top()// Căn top
                .left()// Căn trái
                .padBottom(4); // Padding bottom 4

        table.row(); // Xuống hàng

        // Scroll pane chứa các tag buttons
        table.pane(card -> {
            card.defaults().size(cardSize, 50); // Size mặc định
            int z = 0; // Counter cho columns

            // Duyệt qua các tags đã sort
            for (int i = 0; i < category.tags().sort((a, b) -> a.position() - b.position()).size; i++) {
                var value = category.tags().get(i);

                // Kiểm tra tag có thuộc mod đã chọn không
                if (value.planetIds() == null // Không có planetIds = hiển thị
                        || value.planetIds().size == 0 // Rỗng = hiển thị
                        || value.planetIds().find(t -> modIds.contains(t)) != null) { // Hoặc thuộc mod đã chọn

                    // Tạo button cho tag
                    card.button(btn -> {
                        btn.left(); // Căn trái
                        // Nếu có icon, hiển thị
                        if (value.icon() != null && !value.icon().isEmpty()) {
                            btn.add(new NetworkImage(value.icon()))
                                    .size(40 * scale) // Size theo scale
                                    .padRight(4)
                                    .marginRight(4);
                        }
                        btn.add(formatTag(value.name())).fontScale(scale); // Tên tag

                    }, style, () -> {
                        searchConfig.setTag(category, value); // Toggle tag khi click
                    })//
                            .checked(searchConfig.containTag(category, value))// Checked nếu đã chọn
                            .padRight(CARD_GAP)// Padding phải
                            .padBottom(CARD_GAP)// Padding dưới
                            .left()// Căn trái
                            .expandX() // Mở rộng X
                            .margin(12); // Margin 12

                    // Xuống hàng sau mỗi `cols` buttons
                    if (++z % cols == 0) {
                        card.row();
                    }
                }
            }
        })//
                .growY()// Mở rộng Y
                .wrap()// Wrap content
                .top()// Căn top
                .left()// Căn trái
                .expandX() // Mở rộng X
                .scrollX(true)// Cho phép scroll X
                .scrollY(false)// Không scroll Y
                .padBottom(48); // Padding bottom 48
    }

    /**
     * Format tag name để hiển thị.
     * @param name Tên tag
     * @return Tên đã format
     */
    private String formatTag(String name) {
        return name; // Trả về nguyên gốc (có thể customize)
    }
}
