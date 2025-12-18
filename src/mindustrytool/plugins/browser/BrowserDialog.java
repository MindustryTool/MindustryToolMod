package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.ScrollPane;
import arc.struct.*;
import mindustrytool.plugins.browser.ui.BaseBrowserDialog;
import mindustry.ui.dialogs.BaseDialog;
import java.util.concurrent.TimeUnit;

public class BrowserDialog<T extends ContentData> extends BaseBrowserDialog {
    private static final float IMG_SIZE = 210, INFO_H = 60;
    private final ContentType type;
    private final BaseDialog infoDialog;
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);
    private final ObjectMap<String, String> options = new ObjectMap<>();
    private final PagingRequest<T> request;
    private final SearchConfig searchConfig = new SearchConfig();
    private final FilterDialog filterDialog;
    private Seq<T> dataList = new Seq<>();
    private String search = "";

    // UI Components
    private Table contentTable;
    private Table footerTable;
    private Table tagBarTable;
    private ScrollPane tagPane;

    public BrowserDialog(ContentType type, Class<T> dataType, BaseDialog infoDialog) {
        super(type.title);
        this.type = type;
        this.infoDialog = infoDialog;
        this.request = new PagingRequest<>(dataType, Config.API_URL + type.endpoint);
        TagService.TagCategoryEnum tagCategory = type == ContentType.MAP ? TagService.TagCategoryEnum.maps
                : TagService.TagCategoryEnum.schematics;
        this.filterDialog = new FilterDialog(new TagService(), searchConfig,
                t -> new TagService().getTag(tagCategory, t::get));

        setupUI();

        initOptions();
        initListeners();
        request.getPage(this::handleResult);
    }

    private static int calculateItemsPerPage(ContentType type) {
        // Use settings value first, fallback to calculated value
        int settingsValue = BrowserSettingsDialog.getItemsPerPage(type);
        if (settingsValue > 0)
            return settingsValue;

        float s = type == ContentType.MAP ? IMG_SIZE : Scl.scl(IMG_SIZE);
        return Math.max((int) (Core.graphics.getWidth() / Scl.scl(s))
                * (int) (Core.graphics.getHeight() / Scl.scl(IMG_SIZE + INFO_H * 2)), 20);
    }

    private void initOptions() {
        request.setItemPerPage(calculateItemsPerPage(type));
        options.put("sort", searchConfig.getSort().getValue());
        options.put("verification", "VERIFIED");
        request.setOptions(options);
    }

    private void initListeners() {
        filterDialog.hidden(() -> Core.app.post(() -> {
            if (searchConfig.isChanged()) {
                searchConfig.update();
                options.put("tags", searchConfig.getSelectedTagsString());
                options.put("sort", searchConfig.getSort().getValue());
                request.setPage(0);
                request.getPage(this::handleResult);
            }
        }));
        onResize(() -> {
            request.setItemPerPage(calculateItemsPerPage(type));
            updateContent();
            if (filterDialog.isShown())
                filterDialog.show(searchConfig);
        });
    }

    private void setupUI() {
        clear();
        addCloseButton();

        row();
        BrowserSearchBarBuilder.build(this, search, options, request, searchConfig, debouncer, filterDialog,
                this::handleResult, this::hide);

        row();
        // Persistent Tag Bar Table
        tagBarTable = new Table();
        tagBarTable.left();

        // Wrap in scroll pane and store reference
        tagPane = new ScrollPane(tagBarTable);
        // Increased height to 50 to fit chips comfortably
        add(tagPane).fillX().expandX().height(50).pad(2);

        row();
        contentTable = new Table();
        add(contentTable).expand().fill();

        row();
        footerTable = new Table();
        add(footerTable).expandX().fillX();
    }

    private void updateContent() {
        if (contentTable == null || footerTable == null || tagBarTable == null)
            return;

        String url = type == ContentType.MAP ? Config.UPLOAD_MAP_URL : Config.UPLOAD_SCHEMATIC_URL;

        // Update Tag Bar with double-buffering
        tagBarTable = TagBar.rebuild(tagPane, searchConfig, sc -> {
            options.put("tags", sc.getSelectedTagsString());
            request.setPage(0);
            debouncer.debounce(() -> request.getPage(this::handleResult));
        });

        BrowserContentBuilder.build(contentTable, request, dataList, type, infoDialog, this::hide, this::handleResult);
        PaginationFooter.build(footerTable, request, this::handleResult, url);
    }

    private void handleResult(Seq<T> items) {
        this.dataList = items != null ? items : new Seq<>();
        updateContent();
    }

    /** Reload settings and refresh the view. */
    public void reloadSettings() {
        request.setItemPerPage(calculateItemsPerPage(type));
        updateContent();
    }

    /** Dispose resources when unloading. */
    public void dispose() {
        debouncer.shutdown();
    }
}
