package mindustrytool.presentation.dialog;

import arc.Core;
import arc.struct.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.util.*;
import mindustrytool.core.config.Config;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.domain.service.*;
import mindustrytool.core.model.*;
import mindustrytool.presentation.builder.*;
import java.util.concurrent.TimeUnit;

public class BrowserDialog<T extends ContentData> extends BaseDialog {
    private final ContentType type;
    private final BaseDialog infoDialog;
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);
    private final ObjectMap<String, String> options = new ObjectMap<>();
    private final PagingRequest<T> request;
    private final SearchConfig searchConfig = new SearchConfig();
    private final FilterDialog filterDialog;
    private Seq<T> dataList = new Seq<>();
    private String search = "";

    public BrowserDialog(ContentType type, Class<T> dataType, BaseDialog infoDialog) {
        super(type.title);
        this.type = type;
        this.infoDialog = infoDialog;
        this.request = new PagingRequest<>(dataType, Config.API_URL + type.endpoint);
        TagService.TagCategoryEnum tagCategory = type == ContentType.MAP ? TagService.TagCategoryEnum.maps : TagService.TagCategoryEnum.schematics;
        this.filterDialog = new FilterDialog(new TagService(), searchConfig, t -> new TagService().getTag(tagCategory, t::get));
        initOptions();
        initListeners();
        request.getPage(this::handleResult);
        shown(this::rebuild);
    }

    private void initOptions() { request.setItemPerPage(BrowserConfig.calculateItemsPerPage(type)); options.put("sort", searchConfig.getSort().getValue()); options.put("verification", "VERIFIED"); request.setOptions(options); }
    private void initListeners() {
        filterDialog.hidden(() -> Core.app.post(() -> { if (searchConfig.isChanged()) { searchConfig.update(); options.put("tags", searchConfig.getSelectedTagsString()); options.put("sort", searchConfig.getSort().getValue()); request.setPage(0); request.getPage(this::handleResult); } }));
        onResize(() -> { request.setItemPerPage(BrowserConfig.calculateItemsPerPage(type)); rebuild(); if (filterDialog.isShown()) filterDialog.show(searchConfig); });
    }

    private void rebuild() { clear(); addCloseButton(); row(); BrowserSearchBarBuilder.build(this, search, options, request, searchConfig, debouncer, filterDialog, this::handleResult, this::hide); row(); BrowserContentBuilder.build(this, request, dataList, type, infoDialog, this::hide, this::handleResult); row(); BrowserFooterBuilder.build(this, type, request, this::handleResult); }
    private void handleResult(Seq<T> items) { this.dataList = items != null ? items : new Seq<>(); rebuild(); }
}
