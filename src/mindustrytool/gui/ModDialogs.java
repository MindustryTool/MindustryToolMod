package mindustrytool.gui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.config.Config;
import mindustrytool.config.Debouncer;
import mindustrytool.config.Utils;
// Import các lớp dữ liệu
import mindustrytool.data.ContentData;
import mindustrytool.data.SearchConfig;
import mindustrytool.data.TagService;
import mindustrytool.data.TagService.TagCategoryEnum;
import mindustrytool.net.Api;
import mindustrytool.net.PagingRequest;

import static mindustry.Vars.*;

import java.util.concurrent.TimeUnit;

import mindustrytool.handler.*;

class ModDialog extends BaseDialog {
    public ModDialog(String title) {
        super(title);
    }
}

public class ModDialogs {
    public static BaseDialog schematicDialog;
    public static BaseDialog mapDialog;

    public static void init() {
        // T không cần ép kiểu
        mapDialog = new BrowserDialog<>(BrowserDialog.BrowserType.MAP, ContentData.class, new MapInfoDialog());
        schematicDialog = new BrowserDialog<>(BrowserDialog.BrowserType.SCHEMATIC, ContentData.class, new SchematicInfoDialog());
    }
}

class BrowserDialog<T> extends ModDialog {

    public enum BrowserType {
        MAP, SCHEMATIC
    }

    private final BrowserType type;
    private final BaseDialog infoDialog;

    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);
    private Seq<T> dataList = new Seq<>();
    private final ObjectMap<String, String> options = new ObjectMap<>();
    private final PagingRequest<T> request;
    private final SearchConfig searchConfig = new SearchConfig();
    private final TagService tagService = new TagService();

    private final String searchMessageText;
    private final TagCategoryEnum tagCategory;
    private final String uploadUrl;
    private String search = "";
    TextField searchField;

    private final float IMAGE_SIZE = 210;
    private final float INFO_TABLE_HEIGHT = 60;

    private final FilterDialog filterDialog;


    public BrowserDialog(BrowserType type, Class<T> dataType, BaseDialog infoDialog) {
        super(type == BrowserType.MAP ? "Map Browser" : "Schematic Browser");

        this.type = type;
        this.infoDialog = infoDialog;

        String endpoint;
        if (type == BrowserType.MAP) {
            this.searchMessageText = "@map.search";
            this.tagCategory = TagCategoryEnum.maps;
            this.uploadUrl = Config.UPLOAD_MAP_URL;
            endpoint = "maps";
        } else { // SCHEMATIC
            this.searchMessageText = "@schematic.search";
            this.tagCategory = TagCategoryEnum.schematics;
            this.uploadUrl = Config.UPLOAD_SCHEMATIC_URL;
            endpoint = "schematics";
        }

        this.request = new PagingRequest<>(dataType, Config.API_URL + endpoint);

        this.filterDialog = new FilterDialog(tagService, searchConfig,
                (tag) -> tagService.getTag(tagCategory, group -> tag.get(group)));

        setItemPerPage();

        options.put("sort", searchConfig.getSort().getValue());
        options.put("verification", "VERIFIED");

        request.setOptions(options);

        filterDialog.hidden(() -> {
            Core.app.post(() -> {
                if (searchConfig.isChanged()) {
                    searchConfig.update();
                    options.put("tags", searchConfig.getSelectedTagsString());
                    options.put("sort", searchConfig.getSort().getValue());

                    request.setPage(0);
                    request.getPage(this::handleResult);
                }
            });
        });

        onResize(() -> {
            setItemPerPage();
            Browser();
            if (filterDialog.isShown()) {
                filterDialog.show(searchConfig);
            }
        });
        request.getPage(this::handleResult);
        shown(this::Browser);
    }

    private void setItemPerPage() {
        float itemSize = type == BrowserType.MAP ? IMAGE_SIZE : Scl.scl(IMAGE_SIZE);
        int columns = (int) (Core.graphics.getWidth() / Scl.scl(itemSize));
        int rows = (int) (Core.graphics.getHeight() / Scl.scl(IMAGE_SIZE + INFO_TABLE_HEIGHT * 2));
        int size = Math.max(columns * rows, 20);

        request.setItemPerPage(size);
    }

    private void Browser() {
        clear();

        try {
            addCloseButton();
            row();
            SearchBar();
            row();
            DataContainer();
            row();
            Footer();
        } catch (Exception ex) {
            clear();
            addCloseButton();
            table(container -> Error(container, Core.bundle.format("message.error") + "\n Error: " + ex.getMessage()));
            Log.err(ex);
        }
    }

    public void loadingWrapper(Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading())
                ui.showInfoFade("Loading");
            else
                action.run();
        });
    }

    private void SearchBar() {
        table(searchBar -> {
            searchBar.button("@back", Icon.leftSmall, this::hide)
                    .width(150).padLeft(2).padRight(2);

            searchBar.table(searchBarWrapper -> {
                searchBarWrapper.left();
                searchField = searchBarWrapper.field(search, (result) -> {
                    search = result;
                    options.put("name", result);
                    request.setPage(0);
                    debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleResult)));
                }).growX().get();

                searchField.setMessageText(searchMessageText);
            })
                    .fillX()
                    .expandX()
                    .padBottom(2)
                    .padLeft(2)
                    .padRight(2);

            searchBar.button(Icon.filterSmall, () -> loadingWrapper(() -> filterDialog.show(searchConfig))).padLeft(2)
                    .padRight(2).width(60);
            searchBar.button(Icon.zoomSmall, () -> loadingWrapper(() -> request.getPage(this::handleResult)))
                    .padLeft(2).padRight(2).width(60);

        }).fillX().expandX();

        row();
        pane(tagBar -> {
            TagBar.draw(tagBar, searchConfig, searchConfig -> {
                options.put("tags", searchConfig.getSelectedTagsString());
                request.setPage(0);
                debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleResult)));
                Browser();
            });
        }).scrollY(false);
    }

    private Cell<TextButton> Error(Table parent, String message) {
        Cell<TextButton> error = parent.button(message, Styles.nonet, () -> request.getPage(this::handleResult));

        return error.center().labelAlign(0).expand().fill();
    }

    private Cell<Label> Loading(Table parent) {
        return parent.labelWrap(Core.bundle.format("message.loading")).center().labelAlign(0).expand().fill();
    }

    private Cell<ScrollPane> DataScrollContainer(Table parent) {
        if (dataList.size == 0)
            return parent.pane(container -> container.add("message.no-result"));

        return parent.pane(container -> {
            float sum = 0;
            for (T data : dataList) {
                if (sum + IMAGE_SIZE * 1.25 >= Core.graphics.getWidth()) {
                    container.row();
                    sum = 0;
                }

                Button[] button = { null };
                // Ép kiểu T thành ContentData đã được tách ra
                button[0] = drawContentPreview(container, (ContentData) data, type);

                sum += button[0].getPrefWidth();
            }
            container.top();
        }).scrollY(true).expand().fill();

    }

    private void Footer() {
        table(footer -> {
            footer.button(Icon.left, () -> request.previousPage(this::handleResult)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.getPage() == 0 || request.isError()).height(40);

            footer.table(Tex.buttonDisabled, table -> {
                table.labelWrap(String.valueOf(request.getPage() + 1)).width(50).style(Styles.defaultLabel)
                        .labelAlign(0).center().fill();
            }).pad(4).height(40);

            footer.button(Icon.edit, () -> {
                ui.showTextInput("@select-page", "", "", input -> {
                    try {
                        request.setPage(Integer.parseInt(input));
                        shown(this::Browser);
                    } catch (Exception e) {
                        ui.showInfo("Invalid input");
                    }
                });
            })
                    .margin(4)
                    .pad(4)
                    .width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button(Icon.right, () -> request.nextPage(this::handleResult)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button("@upload", () -> Core.app.openURI(uploadUrl)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.bottom();
        }).expandX().fillX();
    }

    private Cell<Table> DataContainer() {
        return table(container -> {
            if (request.isLoading()) {
                Loading(container);
                return;
            }

            if (request.isError()) {
                Error(container, String.format("There is an error, reload? (%s)", request.getError()));
                return;
            }

            DataScrollContainer(container);
        }).margin(0).expand().fill().top();
    }

    private void handleResult(Seq<T> items) {
        if (items != null)
            this.dataList = items;
        else
            this.dataList.clear();

        Browser();
    }

    // --- LOGIC GỘP CHUNG CHO PREVIEW MAP/SCHEMATIC ---

    private Button drawContentPreview(Table container, ContentData data, BrowserType type) {
        Button[] button = { null };

        // Ép kiểu dữ liệu (cast)
        final ContentData mapData = (type == BrowserType.MAP) ? (ContentData) data : null;
        final ContentData schematicData = (type == BrowserType.SCHEMATIC) ? (ContentData) data : null;

        // Giả định infoDialog là một trường (field) và cần được ép kiểu
        final Object infoDialogCasted = (type == BrowserType.MAP) ? (MapInfoDialog) infoDialog : (SchematicInfoDialog) infoDialog;

        button[0] = container.button(preview -> {
            preview.top();
            preview.margin(0f);

            // Hàng 1: Các nút hành động
            preview.table(buttons -> {
                buttons.center();
                buttons.defaults().size(50f);

                // Nút Sao chép (CHỈ CÓ TRONG SCHEMATIC)
                if (type == BrowserType.SCHEMATIC) {
                    buttons.button(Icon.copy, Styles.emptyi, () -> SchematicHandler.Copy(schematicData))
                            .padLeft(2).padRight(2);
                }

                // Nút Tải xuống
                if (type == BrowserType.MAP) {
                    buttons.button(Icon.download, Styles.emptyi, () -> MapHandler.Download(mapData))
                            .padLeft(2).padRight(2);
                } else { // SCHEMATIC
                    buttons.button(Icon.download, Styles.emptyi, () -> SchematicHandler.Download(schematicData))
                            .padLeft(2).padRight(2);
                }

                // Nút Thông tin
                buttons.button(Icon.info, Styles.emptyi, () -> {
                    if (type == BrowserType.MAP) {
                        Api.findMapById(mapData.id(), ((MapInfoDialog) infoDialogCasted)::show);
                    } else { // SCHEMATIC
                        Api.findSchematicById(schematicData.id(), ((SchematicInfoDialog) infoDialogCasted)::show);
                    }
                }).tooltip("@info.title");

            }).growX().height(50f);

            preview.row();

            // Hàng 2: Hình ảnh và Tên
            ImageHandler.ImageType imageType = (type == BrowserType.MAP) ? ImageHandler.ImageType.MAP : ImageHandler.ImageType.SCHEMATIC;

            preview.stack(new ImageHandler(data.id(), imageType), new Table(nameTable -> {
                nameTable.top();
                nameTable.table(Styles.black3, c -> {
                    Label label = c.add(data.name()).style(Styles.outlineLabel).color(Color.white).top()
                            .growX().width(200f - 8f).get();
                    label.setEllipsis(true);
                    label.setAlignment(Align.center);
                    Draw.reset();
                }).growX().margin(1).pad(4).maxWidth(Scl.scl(200f - 8f)).padBottom(0);
            })).size(200f);

            preview.row();

            // Hàng 3: Thống kê chi tiết (Đã sửa lỗi ép kiểu sang Long)
            preview.table(stats -> DetailStats.draw(stats, (long)data.likes(), (long)data.comments(), (long)data.downloads())).margin(8);

        // Hành động Click Chính cho toàn bộ Button
        }, () -> {
            // Kiểm tra nút con đã được nhấn hay chưa (chỉ áp dụng cho Schematic)
            if (type == BrowserType.SCHEMATIC && button[0].childrenPressed()) return;

            if (type == BrowserType.MAP) {
                // Logic khi click vào Map trống (như trong đoạn code Map ban đầu)
            } else { // SCHEMATIC (Logic phức tạp hơn)
                final SchematicInfoDialog schematicInfo = (SchematicInfoDialog) infoDialogCasted;

                if (state.isMenu()) {
                    Api.findSchematicById(schematicData.id(), schematicInfo::show);
                } else {
                    if (!state.rules.schematicsAllowed) {
                        ui.showInfo("@schematic.disabled");
                    } else {
                        SchematicHandler.DownloadData(schematicData,
                                dataReceived -> control.input.useSchematic(Utils.readSchematic(dataReceived)));
                        hide();
                    }
                }
            }
        }).pad(4).style(Styles.flati).get();

        button[0].getStyle().up = Tex.pane;
        return button[0];
    }

    // --- Các hàm Gốc được Sửa đổi để gọi hàm Utility ---

    private Button drawMapPreview(Table container, ContentData mapData) {
        // Không cần ép kiểu tường minh nếu MapData implement ContentData
        return drawContentPreview(container, mapData, BrowserType.MAP);
    }

    private Button drawSchematicPreview(Table container, ContentData schematicData) {
        // Không cần ép kiểu tường minh nếu SchematicData implement ContentData
        return drawContentPreview(container, schematicData, BrowserType.SCHEMATIC);
    }
}