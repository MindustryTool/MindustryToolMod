package mindustrytool.browser.gui; // Khai báo package chứa các GUI components

import java.security.InvalidParameterException; // Import exception cho invalid input

import arc.Core; // Import Core để truy cập graphics, bundle
import arc.scene.ui.layout.Scl; // Import Scl để scale theo DPI
import arc.scene.ui.layout.Table; // Import Table - container layout
import arc.struct.Seq; // Import Seq - mảng động của Arc

import mindustry.Vars; // Import Vars để truy cập content
import mindustry.gen.Building; // Import Building để check core items
import mindustry.gen.Icon; // Import Icon cho các icon UI
import mindustry.graphics.Pal; // Import Pal cho color palette
import mindustry.type.ItemSeq; // Import ItemSeq để quản lý items
import mindustry.type.ItemStack; // Import ItemStack cho item + amount
import mindustry.ui.Styles; // Import Styles cho UI styles
import mindustry.ui.dialogs.BaseDialog; // Import BaseDialog base class

import mindustrytool.browser.BrowserConfig; // Import BrowserConfig cho WEB_URL
import mindustrytool.browser.data.DataSchematic; // Import DataSchematic cho requirements
import mindustrytool.browser.data.DataSchematic.SchematicRequirement; // Import nested class
import mindustrytool.browser.gui.browser.BrowserItem; // Import interface BrowserItem
import mindustrytool.browser.gui.browser.BrowserType; // Import BrowserType enum
import mindustrytool.browser.gui.browser.ItemStats;
import arc.util.Align; // Import Align để căn text

import static mindustry.Vars.*; // Static import các Vars thường dùng

/**
 * Dialog thống nhất hiển thị thông tin chi tiết của Map hoặc Schematic.
 * Sử dụng BrowserType để xác định loại và render phù hợp.
 * Layout responsive: trên mobile xếp dọc (image trên, info dưới);
 * trên PC xếp ngang (image trái, info phải).
 */
public class BrowserInfoDialog extends BaseDialog { // Dialog thông tin browser item

    // Kích thước image trên mobile (px trước khi scale)
    private static final float MOBILE_IMAGE_SIZE = 280f;
    // Kích thước image trên PC (px trước khi scale)
    private static final float PC_IMAGE_SIZE = 400f;
    // Chiều cao tối đa của description pane
    private static final float DESC_MAX_HEIGHT = 150f;
    // Loại browser hiện tại (MAP hoặc SCHEMATIC)
    private BrowserType type;

    /**
     * Constructor tạo BrowserInfoDialog.
     */
    public BrowserInfoDialog() {
        super(""); // Gọi constructor cha với title rỗng
        setFillParent(true); // Fill toàn màn hình
        addCloseListener(); // Thêm listener đóng dialog
    }

    /**
     * Mở dialog và populate UI từ BrowserItem data.
     * Method này tính toán responsive sizes và rebuild content pane.
     * @param data BrowserItem chứa thông tin (DataMap hoặc DataSchematic)
     * @param type BrowserType xác định loại browser (MAP hoặc SCHEMATIC)
     */
    public void show(BrowserItem data, BrowserType type) {
        // Kiểm tra data không null
        if (data == null) {
            throw new InvalidParameterException("Data can not be null");
        }
        this.type = type;
        cont.clear(); // Xóa nội dung cũ

        // Kiểm tra layout mobile hay PC
        boolean mobile = Core.graphics.isPortrait();
        // Tính kích thước image theo layout
        float imageSize = Scl.scl(mobile ? MOBILE_IMAGE_SIZE : PC_IMAGE_SIZE);
        // Tính width của info panel (50% màn hình trên PC, 90% trên mobile)
        float infoWidth = mobile ? Core.graphics.getWidth() * 0.9f : Core.graphics.getWidth() * 0.5f;

        // Set title với tên item theo type
        String typeKey = type == BrowserType.MAP ? "map" : "schematic";
        title.setText("[[" + Core.bundle.get(typeKey) + "] " + data.name());

        // Scroll pane chứa main content
        cont.pane(main -> {
            main.defaults().pad(8).top(); // Padding mặc định 8, căn top
            buildLayout(main, data, imageSize, infoWidth, mobile); // Build layout theo device
        }).grow().scrollX(false); // Grow, không scroll ngang

        // Build stats bar ở footer
        buildStatsBar(data, mobile);
        show(); // Hiển thị dialog
    }

    /**
     * Build layout chính theo device type.
     * @param main Table chứa content
     * @param data BrowserItem data
     * @param imageSize Kích thước image
     * @param infoWidth Width của info panel
     * @param mobile true nếu layout mobile
     */
    private void buildLayout(Table main, BrowserItem data, float imageSize, float infoWidth, boolean mobile) {
        // Tạo image widget theo type
        arc.scene.Element imageWidget = type == BrowserType.MAP
                ? new MapImage(data.id())
                : new SchematicImage(data.id());

        if (mobile) {
            // Mobile: Layout xếp dọc (image trên, info dưới)
            main.add(imageWidget).size(imageSize).center().row(); // Image ở giữa
            main.table(info -> { // Table chứa info
                info.defaults().left().padTop(8); // Căn trái, padding top 8
                buildInfoContent(info, data, infoWidth, mobile); // Build nội dung info
            }).growX().row(); // Width cố định, grow X
        } else {
            // PC: Layout xếp ngang (image trái, info phải)
            // Reserve 50% screen for image table, image occupies 75% of that table and is centered
            float imageTableWidth = Core.graphics.getWidth() * 0.5f;
            float imageCellSize = imageTableWidth * 0.75f;

            main.table(imageTable -> {
                imageTable.add(imageWidget).size(imageCellSize).center();
            }).width(imageTableWidth).padRight(16).top(); // Image table on the left

            main.table(info -> { // Table chứa info
                info.defaults().left().padTop(8).row(); // Căn trái, padding top 8
                buildInfoContent(info, data, infoWidth, mobile); // Build nội dung info
            }).top().left(); // Width cố định, căn top, grow Y
        }
    }

    /**
     * Build stats bar với likes, comments, downloads và back button.
     * @param data BrowserItem data
     * @param mobile true nếu layout mobile
     */
    private void buildStatsBar(BrowserItem data, boolean mobile) {
        buttons.clearChildren(); // Xóa buttons cũ
        // Footer: only keep back button here; map stats are shown inside info panel
        buttons.table(statsBar -> {
            statsBar.defaults().pad(8);
            statsBar.add().growX();
            statsBar.button("@back", Icon.left, this::hide).size(mobile ? 120f : 140f, 50f);
        }).growX().pad(8);
    }

    /**
     * Build nội dung phần info: description, size, author, metadata link, tags, requirements (nếu schematic).
     * @param info Table chứa info
     * @param data BrowserItem data
     * @param infoWidth Width của info panel
     * @param mobile true nếu layout mobile
     */
    private void buildInfoContent(Table info, BrowserItem data, float infoWidth, boolean mobile) {
        // Hiển thị description nếu có
        String desc = data.description();
        if (desc != null && !desc.isEmpty()) {
            // Description as a wrapping label (replace pane to avoid nested indent)
            float descWidth = mobile ? infoWidth - 20f : Core.graphics.getWidth() * 0.5f;
            info.add(desc)
                .width(descWidth)
                .maxHeight(Scl.scl(DESC_MAX_HEIGHT))
                .wrap()
                .labelAlign(Align.left)
                .color(Pal.lightishGray)
                .left()
                .row();
        }

        // Hiển thị kích thước
        addLabelRow(info, "message.size", data.width() + "x" + data.height());

        // Hiển thị author
        info.add(Core.bundle.get("message.author")).color(Pal.gray).left().padTop(12).row();
        info.table(authorRow -> UserCard.draw(authorRow, data.createdBy())).left().padTop(4).row();

        // Button mở metadata trên web - sử dụng endpoint từ BrowserType
        info.button("Metadata", Icon.link, Styles.flatBordert,
                () -> Core.app.openURI(BrowserConfig.WEB_URL + "/" + type.endpoint + "/" + data.id()))
                .left().padTop(12).size(Scl.scl(120f), Scl.scl(40f)).row();

        // Hiển thị tags
        info.add(Core.bundle.get("message.tags")).color(Pal.gray).left().padTop(12).row();
        info.table(tags -> TagContainer.draw(tags, data.tags())).left().padTop(4).growX().row();

        // Hiển thị requirements nếu là SCHEMATIC
        if (type == BrowserType.SCHEMATIC && data instanceof DataSchematic schematic) {
            buildRequirements(info, schematic, mobile);
        }

            ItemStats.renderStatsRow(info, data, mobile);
    }

    /**
     * Build requirements grid (icons + counts) - chỉ dùng cho SCHEMATIC.
     * Trên mobile dùng ít cột hơn.
     * @param info Table chứa info
     * @param data DataSchematic data
     * @param mobile true nếu layout mobile
     */
    private void buildRequirements(Table info, DataSchematic data, boolean mobile) {
        // Lấy requirements từ metadata
        Seq<SchematicRequirement> requirements = data.meta() != null ? data.meta().requirements() : null;
        // Chuyển sang ItemSeq
        ItemSeq arr = toItemSeq(requirements);
        // Nếu không có requirements, return
        if (arr.total <= 0) return;

        // Header requirements
        info.add(Core.bundle.get("schematic.requirements")).color(Pal.gray).left().padTop(12).row();

        // Số cột tùy theo mobile/PC
        int cols = mobile ? 3 : 4;
        // Table chứa requirements grid
        info.table(r -> {
            int i = 0; // Counter cho columns
            // Duyệt qua từng item
            for (ItemStack s : arr) {
                r.image(s.item.uiIcon).left().size(iconMed); // Icon của item
                r.label(() -> getRequirementLabel(s)).padLeft(2).left().padRight(8); // Label với số lượng
                if (++i % cols == 0) r.row(); // Xuống hàng sau mỗi `cols` items
            }
        }).left().padTop(4).row();
    }

    /**
     * Lấy label hiển thị cho requirement.
     * Nếu đang trong game, so sánh với items trong core.
     * @param s ItemStack cần hiển thị
     * @return String formatted với màu
     */
    private String getRequirementLabel(ItemStack s) {
        Building core = player.core(); // Lấy core hiện tại
        // Nếu không có core, ở menu, hoặc infinite resources
        if (core == null || state.isMenu() || state.rules.infiniteResources) {
            return "[lightgray]" + s.amount; // Hiển thị màu xám
        }
        // Kiểm tra số lượng hiện có trong core
        int currentAmount = core.items.get(s.item);
        // Nếu đủ items, hiển thị màu xám; nếu không đủ, hiển thị màu đỏ cho phần thiếu
        if (currentAmount >= s.amount) {
            return "[lightgray]" + s.amount;
        }
        return "[scarlet]" + currentAmount + "[lightgray]/" + s.amount;
    }

    /**
     * Helper method để thêm một label row với title và value.
     * @param info Table chứa info
     * @param bundleKey Key trong bundle cho title
     * @param value Giá trị hiển thị
     */
    private void addLabelRow(Table info, String bundleKey, String value) {
        // Title màu xám
        info.add(Core.bundle.get(bundleKey)).color(Pal.gray).left().padTop(12).row();
        // Value
        info.add(value).left().padTop(4).row();
    }

    /**
     * Chuyển đổi Seq<SchematicRequirement> sang ItemSeq.
     * Tìm item trong Vars.content dựa vào tên.
     * @param requirements Seq requirements từ API
     * @return ItemSeq chứa các ItemStack
     */
    private ItemSeq toItemSeq(Seq<SchematicRequirement> requirements) {
        if (requirements == null) return new ItemSeq(); // Nếu null, trả về rỗng

        Seq<ItemStack> seq = new Seq<>(); // Seq để chứa kết quả
        // Duyệt qua từng requirement
        for (var req : requirements) {
            if (req.name() == null) continue; // Bỏ qua nếu name null

            // Tìm item trong content dựa vào tên (case-insensitive)
            var item = Vars.content.items().find(i -> i.name.equalsIgnoreCase(req.name()));
            if (item != null) {
                seq.add(new ItemStack(item, req.amount())); // Thêm vào seq
            }
        }
        return new ItemSeq(seq); // Trả về ItemSeq
    }
}
