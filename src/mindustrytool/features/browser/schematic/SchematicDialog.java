package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.func.Cons;
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
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.ui.Debouncer;
import mindustrytool.Utils;
import mindustrytool.dto.SchematicData;
import mindustrytool.services.TagService.TagCategoryEnum;
import mindustrytool.dto.PagingRequest;
import mindustrytool.ui.TagBar;
import mindustrytool.ui.DetailStats;

import mindustrytool.features.browser.FilterDialog;
import mindustrytool.features.browser.SearchConfig;
import mindustrytool.services.SchematicService;
import mindustrytool.services.TagService;

import static mindustry.Vars.*;

import java.util.concurrent.TimeUnit;

public class SchematicDialog extends BaseDialog {

    private final SchematicInfoDialog infoDialog = new SchematicInfoDialog();

    private Seq<SchematicData> schematicsData = new Seq<>();
    private final Debouncer debouncer = new Debouncer(500, TimeUnit.MILLISECONDS);

    private final float IMAGE_SIZE = 210;
    private final float INFO_TABLE_HEIGHT = 60;

    private static SearchConfig searchConfig = new SearchConfig();
    private final TagService tagService = new TagService();
    private final SchematicService schematicService = new SchematicService();
    private final FilterDialog filterDialog = new FilterDialog(tagService, searchConfig,
            (tag) -> tagService.getTag(TagCategoryEnum.schematics, group -> tag.get(group)));

    private String search = "";

    TextField searchField;

    private PagingRequest<SchematicData> request;
    private ObjectMap<String, String> options = new ObjectMap<String, String>();

    public SchematicDialog() {
        super("Schematic Browser");

        request = new PagingRequest<>(SchematicData.class, Config.API_URL + "schematics");

        setItemPerPage();

        options.put("sort", searchConfig.getSort().getValue());
        options.put("verification", "VERIFIED");

        request.setOptions(options);

        filterDialog.hidden(() -> {
            if (searchConfig.isChanged()) {
                searchConfig.update();
                options.put("tags", searchConfig.getSelectedTagsString());
                options.put("sort", searchConfig.getSort().getValue());
                request.setPage(0);
                request.getPage(this::handleSchematicResult);
            }
        });

        onResize(() -> {
            setItemPerPage();
            SchematicBrowser();

            if (filterDialog.isShown()) {
                filterDialog.show(searchConfig);
            }
        });
        request.getPage(this::handleSchematicResult);
        shown(this::SchematicBrowser);
    }

    private void setItemPerPage() {
        int columns = (int) (Core.graphics.getWidth() / Scl.scl() * 0.9f / Scl.scl(IMAGE_SIZE)) - 1;
        int rows = (int) (Core.graphics.getHeight() / Scl.scl(IMAGE_SIZE + INFO_TABLE_HEIGHT * 2));
        int size = Math.max(columns * rows, 20);

        request.setItemPerPage(size);
    }

    private void SchematicBrowser() {
        clear();

        try {
            addCloseButton();
            row();
            SearchBar();
            row();
            SchematicContainer();
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
            searchBar.button("@back", Icon.leftSmall, this::hide)//
                    .width(150).padLeft(2).padRight(2);

            searchBar.table(searchBarWrapper -> {
                searchBarWrapper.left();
                searchField = searchBarWrapper.field(search, (result) -> {
                    search = result;
                    options.put("name", result);
                    request.setPage(0);
                    debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleSchematicResult)));

                }).growX().get();

                searchField.setMessageText("@schematic.search");
            })//
                    .fillX()//
                    .expandX()//
                    .padBottom(2)//
                    .padLeft(2)//
                    .padRight(2);

            searchBar.button(Icon.filterSmall, () -> loadingWrapper(() -> filterDialog.show(searchConfig))).padLeft(2)
                    .padRight(2).width(60);
            searchBar.button(Icon.zoomSmall, () -> loadingWrapper(() -> request.getPage(this::handleSchematicResult)))
                    .padLeft(2).padRight(2).width(60);

        }).fillX().expandX();

        row();
        pane(tagBar -> {
            TagBar.draw(tagBar, searchConfig, (searchConfig) -> {
                options.put("tags", searchConfig.getSelectedTagsString());
                request.setPage(0);
                debouncer.debounce(() -> loadingWrapper(() -> request.getPage(this::handleSchematicResult)));
                SchematicBrowser();
            });
        }).scrollY(false);
    }

    private Cell<TextButton> Error(Table parent, String message) {
        Cell<TextButton> error = parent.button(message, Styles.nonet,
                () -> request.getPage(this::handleSchematicResult));

        return error.center().labelAlign(0).expand().fill();
    }

    private Cell<Label> Loading(Table parent) {
        return parent.labelWrap(Core.bundle.format("message.loading")).center().labelAlign(0).expand().fill();
    }

    private Cell<ScrollPane> SchematicScrollContainer(Table parent) {
        if (schematicsData.size == 0)
            return parent.pane(container -> container.add("message.no-result"));

        return parent.pane(container -> {
            float sum = 0;

            for (SchematicData schematicData : schematicsData) {
                if (sum + IMAGE_SIZE * 2 >= Core.graphics.getWidth()) {
                    container.row();
                    sum = 0;
                }

                try {

                    Button[] button = { null };
                    button[0] = container.button(schematicPreview -> {
                        schematicPreview.top();
                        schematicPreview.margin(0f);
                        schematicPreview.table(buttons -> {
                            buttons.center();
                            buttons.defaults().size(50f);
                            buttons.button(Icon.copy, Styles.emptyi, () -> handleCopySchematic(schematicData))
                                    .padLeft(2).padRight(2);
                            buttons.button(Icon.download, Styles.emptyi, () -> handleDownloadSchematic(schematicData))
                                    .padLeft(2).padRight(2);
                            buttons.button(Icon.info, Styles.emptyi,
                                    () -> schematicService.findSchematicById(schematicData.id(), infoDialog::show))
                                    .tooltip("@info.title");

                        }).growX().height(50f);

                        schematicPreview.row();
                        schematicPreview.stack(new SchematicImage(schematicData.id()), new Table(schematicName -> {
                            schematicName.top();
                            schematicName.table(Styles.black3, c -> {
                                Label label = c.add(schematicData.name())//
                                        .style(Styles.outlineLabel)//
                                        .color(Color.white)//
                                        .top()//
                                        .growX()//
                                        .width(200f - 8f).get();
                                Draw.reset();

                                label.setEllipsis(true);
                                label.setAlignment(Align.center);
                            })//
                                    .growX()//
                                    .margin(1)//
                                    .pad(4)//
                                    .maxWidth(Scl.scl(200f - 8f))//
                                    .padBottom(0);

                        })).size(200f);

                        schematicPreview.row();
                        schematicPreview.table(stats -> DetailStats.draw(stats, schematicData.likes(),
                                schematicData.comments(), schematicData.downloads())).margin(8);

                    }, () -> {
                        if (button[0].childrenPressed())
                            return;

                        if (state.isMenu()) {
                            schematicService.findSchematicById(schematicData.id(), infoDialog::show);
                        } else {
                            if (!state.rules.schematicsAllowed) {
                                ui.showInfo("@schematic.disabled");
                            } else {
                                handleDownloadSchematicData(schematicData,
                                        data -> control.input.useSchematic(Utils.readSchematic(data)));
                                hide();
                            }
                        }

                    }).pad(4).style(Styles.flati).get();

                    button[0].getStyle().up = Tex.pane;

                    sum += button[0].getPrefWidth();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            container.top();
        }).scrollY(true).expand().fill();

    }

    private void Footer() {
        table(footer -> {
            footer.button(Icon.left, () -> request.previousPage(this::handleSchematicResult)).margin(4).pad(4)
                    .width(100).disabled(request.isLoading() || request.getPage() == 0 || request.isError()).height(40);

            footer.table(Tex.buttonDisabled, table -> {
                table.labelWrap(String.valueOf(request.getPage() + 1)).width(50).style(Styles.defaultLabel)
                        .labelAlign(0).center().fill();
            }).pad(4).height(40);

            footer.button(Icon.edit, () -> {
                ui.showTextInput("@select-page", "", "", input -> {
                    try {
                        request.setPage(Integer.parseInt(input));
                        shown(this::SchematicBrowser);
                    } catch (Exception e) {
                        ui.showInfo("Invalid input");
                    }
                });
            })//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button(Icon.right, () -> request.nextPage(this::handleSchematicResult))//
                    .margin(4)//
                    .pad(4)//
                    .width(100)//
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.button("@upload", () -> Core.app.openURI(Config.UPLOAD_SCHEMATIC_URL)).margin(4).pad(4).width(100)
                    .disabled(request.isLoading() || request.hasMore() == false || request.isError()).height(40);

            footer.bottom();
        }).expandX().fillX();
    }

    private Cell<Table> SchematicContainer() {
        return table(container -> {
            if (request.isLoading()) {
                Loading(container);
                return;
            }

            if (request.isError()) {
                Error(container, String.format("There is an error, reload? (%s)", request.getError()));
                return;
            }

            SchematicScrollContainer(container);
        }).expand().fill().top();
    }

    private void handleSchematicResult(Seq<SchematicData> schematics) {
        if (schematics != null)
            this.schematicsData = schematics;
        else
            this.schematicsData.clear();

        SchematicBrowser();
    }

    private void handleCopySchematic(SchematicData schematic) {
        handleDownloadSchematicData(schematic, data -> {
            Schematic s = Utils.readSchematic(data);
            Core.app.setClipboardText(schematics.writeBase64(s));
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
