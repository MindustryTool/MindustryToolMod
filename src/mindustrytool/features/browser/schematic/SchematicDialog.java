package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.func.Cons;
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
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.dto.PagingRequest;
import mindustrytool.dto.SchematicData;
import mindustrytool.features.browser.FilterDialog;
import mindustrytool.features.browser.SearchConfig;
import mindustrytool.services.SchematicService;
import mindustrytool.services.TagService;
import mindustrytool.services.TagService.TagCategoryEnum;
import mindustrytool.ui.Debouncer;
import mindustrytool.ui.DetailStats;
import mindustrytool.ui.TagBar;

import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;

public class SchematicDialog extends BaseDialog {

    private static final float TARGET_WIDTH = 250f;
    private static final float CARD_MARGIN = 4f;
    private static final float BUTTON_HEIGHT = 40f;
    private static final float BUTTON_WIDTH = 100f;
    private static final float ICON_BUTTON_WIDTH = 60f;
    private static final float PREVIEW_BUTTON_SIZE = 50f;
    private static final float PREVIEW_NAME_WIDTH = 200f - 8f;
    private static final float CARD_HEIGHT = 330f;

    private final SchematicInfoDialog infoDialog = new SchematicInfoDialog();
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);
    private final TagService tagService = new TagService();
    private final SchematicService schematicService = new SchematicService();

    private static SearchConfig searchConfig = new SearchConfig();

    private final FilterDialog filterDialog = new FilterDialog(tagService, searchConfig,
            (tag) -> tagService.getTag(TagCategoryEnum.schematics, group -> tag.get(group)));

    private Seq<SchematicData> schematicsData = new Seq<>();
    private PagingRequest<SchematicData> request;
    private ObjectMap<String, String> options = new ObjectMap<>();

    private Table searchTable;
    private Table contentTable;
    private Table footerTable;
    private TextField searchField;

    private int cols;
    private int rows;
    private float cardWidth;
    private String searchText = "";

    public SchematicDialog() {
        super("Schematic Browser");

        initializeRequest();
        updateLayoutMetrics();
        initializeUI();
        setupEventHandlers();

        request.getPage(this::handleSchematicResult);
    }

    private void initializeRequest() {
        request = new PagingRequest<>(SchematicData.class, Config.API_URL + "schematics");

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
                reloadSchematics();
            }
        });

        onResize(() -> {
            updateLayoutMetrics();
            rebuildSchematics();
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
        rebuildSchematics();
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
                searchField.setMessageText("@schematic.search");

                if (!searchText.isEmpty() && searchField != null) {
                    searchField.setCursorPosition(lastCursor);

                }
            }).growX().pad(2);

            bar.button(Icon.filterSmall, () -> loadingWrapper(() -> filterDialog.show(searchConfig)))
                    .width(ICON_BUTTON_WIDTH).pad(2);

            bar.button(Icon.zoomSmall, () -> loadingWrapper(() -> request.getPage(this::handleSchematicResult)))
                    .width(ICON_BUTTON_WIDTH).pad(2);

        }).fillX().expandX();

        searchTable.row();

        searchTable.pane(tagBar -> {
            TagBar.draw(tagBar, searchConfig, (cfg) -> {
                updateOptions();
                reloadSchematics();
                rebuildSearch();
            });
        }).scrollY(false).fillX().expandX();
    }

    private void handleSearchInput(String result) {
        searchText = result;
        options.put("name", result);
        debouncer.debounce(this::reloadSchematics);
    }

    private void rebuildSchematics() {
        contentTable.clear();

        if (request.isLoading()) {
            showLoading(contentTable);
            return;
        }

        if (request.isError()) {
            showError(contentTable, "Error: " + request.getError());
            return;
        }

        if (schematicsData.isEmpty()) {
            contentTable.add("@message.no-result").center();
            return;
        }

        buildSchematicGrid(contentTable);
    }

    private void buildSchematicGrid(Table parent) {
        parent.pane(container -> {
            container.top();

            for (int i = 0; i < schematicsData.size; i++) {
                SchematicData data = schematicsData.get(i);
                addSchematicCard(container, data);

                if ((i + 1) % cols == 0) {
                    container.row();
                }
            }
        }).scrollY(true).expand().fill();
    }

    private void addSchematicCard(Table container, SchematicData data) {
        Button[] buttonRef = { null };

        buttonRef[0] = container.button(preview -> {
            preview.top().margin(0f);

            preview.table(buttons -> {
                buttons.center().defaults().size(PREVIEW_BUTTON_SIZE);

                buttons.button(Icon.copy, Styles.emptyi, () -> handleCopySchematic(data))
                        .pad(2);
                buttons.button(Icon.download, Styles.emptyi, () -> handleDownloadSchematic(data))
                        .pad(2);
                buttons.button(Icon.info, Styles.emptyi,
                        () -> schematicService.findSchematicById(data.id(), infoDialog::show))
                        .tooltip("@info.title");
            }).growX().height(PREVIEW_BUTTON_SIZE);

            preview.row();

            preview.stack(
                    new Table(t -> t.add(new SchematicImage(data.id()))),
                    new Table(nameTable -> {
                        nameTable.top();
                        nameTable.table(Styles.black3, c -> {
                            Label label = c.add(data.name())
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
            preview.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads())).margin(8);

        }, () -> {
            handleCardClick(buttonRef[0], data);
        }).pad(CARD_MARGIN).style(Styles.flati).width(cardWidth).height(CARD_HEIGHT).get();

        buttonRef[0].getStyle().up = Tex.pane;
    }

    private void handleCardClick(Button button, SchematicData data) {
        if (button.childrenPressed())
            return;

        if (state.isMenu()) {
            schematicService.findSchematicById(data.id(), infoDialog::show);
        } else {
            if (!state.rules.schematicsAllowed) {
                ui.showInfo("@schematic.disabled");
            } else {
                handleDownloadSchematicData(data,
                        content -> control.input.useSchematic(Utils.readSchematic(content)));
                hide();
            }
        }
    }

    private void rebuildFooter() {
        footerTable.clear();

        footerTable.defaults().margin(4).pad(4).height(BUTTON_HEIGHT);

        footerTable.button(Icon.left, () -> request.previousPage(this::handleSchematicResult))
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || request.getPage() == 0);

        footerTable.table(Tex.buttonDisabled, t -> t.labelWrap(String.valueOf(request.getPage() + 1))
                .width(50).style(Styles.defaultLabel)
                .labelAlign(Align.center).center().fill()).width(60);

        footerTable.button(Icon.edit, this::showPageSelectDialog)
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || !request.hasMore());

        footerTable.button(Icon.right, () -> request.nextPage(this::handleSchematicResult))
                .width(BUTTON_WIDTH)
                .disabled(shouldDisableNav() || !request.hasMore());

        footerTable.button("@upload", () -> Core.app.openURI(Config.UPLOAD_SCHEMATIC_URL))
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
                reloadSchematics();
            } catch (NumberFormatException e) {
                ui.showInfo("Invalid input");
            }
        });
    }

    private void showLoading(Table parent) {
        parent.labelWrap("@message.loading").labelAlign(Align.center).center().expand().fill();
    }

    private void showError(Table parent, String message) {
        parent.button(message, Styles.nonet, () -> request.getPage(this::handleSchematicResult))
                .center().labelAlign(Align.center).expand().fill();
    }

    public void loadingWrapper(Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading()) {
                ui.showInfoFade("Loading");
            } else {
                action.run();
            }
        });
    }

    private void reloadSchematics() {
        request.setPage(0);
        loadingWrapper(() -> request.getPage(this::handleSchematicResult));
    }

    private void handleSchematicResult(Seq<SchematicData> schematics) {
        this.schematicsData = schematics != null ? schematics : new Seq<>();
        rebuildSchematics();
        rebuildFooter();
    }

    private void handleCopySchematic(SchematicData schematic) {
        handleDownloadSchematicData(schematic, data -> {
            Schematic s = Utils.readSchematic(data);
            Core.app.setClipboardText(Vars.schematics.writeBase64(s));
            ui.showInfoFade("@copied");
        });
    }

    private void handleDownloadSchematic(SchematicData schematic) {
        handleDownloadSchematicData(schematic, data -> {
            Schematic s = Utils.readSchematic(data);
            schematicService.findSchematicById(schematic.id(), detail -> {
                s.labels.add(detail.tags().map(i -> i.name()));
                s.removeSteamID();
                Vars.schematics.add(s);
                ui.showInfoFade("@schematic.saved");
            });
        });
    }

    private void handleDownloadSchematicData(SchematicData data, Cons<String> cons) {
        schematicService.downloadSchematic(data.id(), result -> {
            cons.get(new String(Base64Coder.encode(result)));
        });
    }
}
