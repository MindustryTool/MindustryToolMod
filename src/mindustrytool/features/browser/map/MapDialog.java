package mindustrytool.features.browser.map;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.dto.MapData;
import mindustrytool.dto.PagingRequest;
import mindustrytool.features.browser.FilterDialog;
import mindustrytool.features.browser.SearchConfig;
import mindustrytool.services.MapService;
import mindustrytool.services.TagService;
import mindustrytool.services.TagService.TagCategoryEnum;
import mindustrytool.ui.Debouncer;
import mindustrytool.ui.DetailStats;
import mindustrytool.ui.TagBar;

import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;

public class MapDialog extends BaseDialog {

    private static final float TARGET_WIDTH = 250f;
    private static final float CARD_MARGIN = 4f;
    private static final float BUTTON_HEIGHT = 40f;
    private static final float BUTTON_WIDTH = 100f;
    private static final float ICON_BUTTON_WIDTH = 60f;
    private static final float PREVIEW_BUTTON_SIZE = 50f;
    private static final float PREVIEW_NAME_WIDTH = 200f - 8f;
    private static final float CARD_HEIGHT = 330f;

    private final MapInfoDialog infoDialog = new MapInfoDialog();
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);
    private final TagService tagService = new TagService();

    private static SearchConfig searchConfig = new SearchConfig();

    private final FilterDialog filterDialog = new FilterDialog(tagService, searchConfig,
            (tag) -> tagService.getTag(TagCategoryEnum.maps, group -> tag.get(group)));

    private Seq<MapData> mapsData = new Seq<>();
    private PagingRequest<MapData> request;
    private ObjectMap<String, String> options = new ObjectMap<>();

    private Table searchTable;
    private Table contentTable;
    private Table footerTable;
    private TextField searchField;

    private int cols;
    private int rows;
    private float cardWidth;
    private String searchText = "";

    public MapDialog() {
        super("@message.map-browser.title");

        initializeRequest();
        updateLayoutMetrics();
        initializeUI();
        setupEventHandlers();

        request.getPage(this::handleMapResult);
    }

    private void initializeRequest() {
        request = new PagingRequest<>(MapData.class, Config.API_URL + "maps");

        options.put("sort", searchConfig.getSort().getValue());
        options.put("verification", "VERIFIED");
        request.setOptions(options);
    }

    private void initializeUI() {

        searchTable = new Table();
        contentTable = new Table();
        footerTable = new Table();

        clear();
        addCloseListener();

        add(searchTable).fillX().expandX();
        row();
        add(contentTable).expand().fill();
        row();
        add(footerTable).fillX().expandX();

        rebuildSearch();
        rebuildFooter();
    }

    private void setupEventHandlers() {
        filterDialog.hidden(() -> {
            if (searchConfig.isChanged()) {
                searchConfig.update();
                updateOptions();
                reloadMaps();
            }
        });

        onResize(() -> {
            updateLayoutMetrics();
            rebuildMaps();
            if (filterDialog.isShown()) {
                filterDialog.show(searchConfig);
            }
        });

        shown(this::rebuildAll);
    }

    private void updateOptions() {
        options.put("tags", searchConfig.getSelectedTagsString());
        options.put("sort", searchConfig.getSort().getValue());
        if (!searchText.isEmpty()) {
            options.put("name", searchText);
        } else {
            options.remove("name");
        }
    }

    private void updateLayoutMetrics() {
        float availableWidth = Core.graphics.getWidth() / Scl.scl() * 0.9f;
        float targetW = Math.min(availableWidth, TARGET_WIDTH);

        cols = Mathf.clamp((int) (availableWidth / targetW), 1, 20);

        rows = Math.max((int) (Core.graphics.getHeight() / Scl.scl(370)), 1) + 1;
        cardWidth = availableWidth / cols;

        int pageSize = Math.max(cols * rows, 20);

        request.setItemPerPage(pageSize);
    }

    private void rebuildAll() {
        rebuildSearch();
        rebuildMaps();
        rebuildFooter();
    }

    private void rebuildSearch() {
        searchTable.clear();

        searchTable.table(bar -> {

            bar.button("@back", Icon.leftSmall, this::hide)
                    .width(150).pad(2);

            bar.table(wrapper -> {
                wrapper.left();

                int lastCursor = searchField == null ? searchText.length() : searchField.getCursorPosition();

                searchField = wrapper.field(searchText, this::handleSearchInput).growX().get();
                searchField.setMessageText("@map.search");

                if (!searchText.isEmpty() && searchField != null) {
                    searchField.setCursorPosition(lastCursor);

                }
            }).growX().pad(2);

            bar.button(Icon.filterSmall, () -> loadingWrapper(() -> filterDialog.show(searchConfig)))
                    .width(ICON_BUTTON_WIDTH).pad(2);

            bar.button(Icon.zoomSmall, () -> loadingWrapper(() -> request.getPage(this::handleMapResult)))
                    .width(ICON_BUTTON_WIDTH).pad(2);

        }).fillX().expandX();

        searchTable.row();

        searchTable.pane(tagBar -> {
            TagBar.draw(tagBar, searchConfig, (cfg) -> {
                updateOptions();
                reloadMaps();
                rebuildSearch();
            });
        }).scrollY(false).fillX().expandX();
    }

    private void handleSearchInput(String result) {
        searchText = result;
        options.put("name", result);
        debouncer.debounce(this::reloadMaps);
    }

    private void rebuildMaps() {
        contentTable.clear();

        if (request.isLoading()) {
            showLoading(contentTable);
            return;
        }

        if (request.isError()) {
            showError(contentTable, Core.bundle.format("error.prefix", request.getError()));
            return;
        }

        if (mapsData.isEmpty()) {
            contentTable.add("@message.no-result").center();
            return;
        }

        buildMapGrid(contentTable);
    }

    private void buildMapGrid(Table parent) {
        parent.pane(container -> {
            container.top();

            for (int i = 0; i < mapsData.size; i++) {
                MapData data = mapsData.get(i);
                addMapCard(container, data);

                if ((i + 1) % cols == 0) {
                    container.row();
                }
            }
        }).scrollY(true).expand().fill();
    }

    private void addMapCard(Table container, MapData data) {
        Button[] buttonRef = { null };

        buttonRef[0] = container.button(preview -> {
            preview.top().margin(0f);

            preview.table(buttons -> {
                buttons.center().defaults().size(PREVIEW_BUTTON_SIZE);

                buttons.button(Icon.download, Styles.emptyi, () -> handleDownloadMap(data.getId()))
                        .pad(2);
                buttons.button(Icon.info, Styles.emptyi,
                        () -> MapService.findMapById(data.getId())
                                .thenAccept(m -> Core.app.post(() -> infoDialog.show(m))))
                        .tooltip("@info.title");
            }).growX().height(PREVIEW_BUTTON_SIZE);

            preview.row();

            preview.stack(
                    new Table(t -> t.add(new MapImage(data.getId()))),
                    new Table(nameTable -> {
                        nameTable.top();
                        nameTable.table(Styles.black3, c -> {
                            Label label = c.add(data.getName())
                                    .style(Styles.outlineLabel)
                                    .color(Color.white)
                                    .top()
                                    .growX()
                                    .width(PREVIEW_NAME_WIDTH).get();

                            Draw.reset();
                            label.setEllipsis(true);
                            label.setAlignment(Align.center);
                        })
                                .growX()
                                .margin(1)
                                .pad(CARD_MARGIN)
                                .maxWidth(Scl.scl(PREVIEW_NAME_WIDTH))
                                .padBottom(0);
                    }));

            preview.row();

            preview.table().expandY().row();
            preview.table(stats -> DetailStats.draw(stats, data.getLikes(), data.getComments(), data.getDownloads()))
                    .margin(8);

        }, () -> {
            handleCardClick(buttonRef[0], data);
        }).pad(CARD_MARGIN).style(Styles.flati).width(cardWidth).height(CARD_HEIGHT).get();

        buttonRef[0].getStyle().up = Tex.pane;
    }

    private void handleCardClick(Button button, MapData data) {
        if (button.childrenPressed()) {
            return;
        }

        MapService.findMapById(data.getId()).thenAccept(m -> Core.app.post(() -> infoDialog.show(m)));
    }

    private void rebuildFooter() {
        footerTable.clear();

        footerTable.defaults().margin(4).pad(4).height(BUTTON_HEIGHT);

        footerTable.button(Icon.left, () -> request.previousPage(this::handleMapResult))
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || request.getPage() == 0);

        footerTable.table(Tex.buttonDisabled, t -> t.labelWrap(String.valueOf(request.getPage() + 1))
                .width(50).style(Styles.defaultLabel)
                .labelAlign(Align.center).center().fill()).width(60);

        footerTable.button(Icon.edit, this::showPageSelectDialog)
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || !request.hasMore());

        footerTable.button(Icon.right, () -> request.nextPage(this::handleMapResult))
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || !request.hasMore());

        footerTable.button("@upload", () -> Core.app.openURI(Config.UPLOAD_MAP_URL))
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || !request.hasMore());
    }

    private boolean shouldDisableNav() {
        return request.isLoading() || request.isError();
    }

    private void showPageSelectDialog() {
        ui.showTextInput("@select-page", "", "", input -> {
            try {
                int page = Integer.parseInt(input);

                if (page > 0)
                    page--;
                request.setPage(page);
                reloadMaps();
            } catch (NumberFormatException e) {
                ui.showInfo("@invalid-input");
            }
        });
    }

    private void showLoading(Table parent) {
        parent.labelWrap("@message.loading").labelAlign(Align.center).center().expand().fill();
    }

    private void showError(Table parent, String message) {
        parent.button(message, Styles.nonet, () -> request.getPage(this::handleMapResult))
                .center().labelAlign(Align.center).expand().fill();
    }

    public void loadingWrapper(Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading()) {
                ui.showInfoFade("@loading");
            } else {
                action.run();
            }
        });
    }

    private void reloadMaps() {
        request.setPage(0);
        loadingWrapper(() -> request.getPage(this::handleMapResult));
    }

    private void handleMapResult(Seq<MapData> maps) {
        this.mapsData = maps != null ? maps : new Seq<>();
        rebuildMaps();
        rebuildFooter();
    }

    public static void handleDownloadMap(String id) {
        MapService.downloadMap(id).thenAccept(result -> {
            Core.app.post(() -> {
                try {
                    Fi mapFile = Vars.customMapDirectory.child(id);
                    mapFile.writeBytes(result);
                    Vars.maps.importMap(mapFile);
                    ui.showInfoFade("@map.saved");
                } catch (Exception e) {
                    ui.showInfoFade(e.getMessage());
                }
            });
        }).exceptionally(error -> {
            Core.app.post(() -> {
                ui.showInfoFade(error.getMessage());
            });
            return null;
        });
    }
}
