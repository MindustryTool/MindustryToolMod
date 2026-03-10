package mindustrytool.features.browser;

import java.util.stream.Collectors;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.dto.ModData;
import mindustrytool.dto.TagCategory;
import mindustrytool.services.ModService;
import mindustrytool.services.TagService;
import mindustrytool.ui.NetworkImage;

public class FilterDialog extends BaseDialog {
    private static final float TARGET_WIDTH = 250f;

    private TextButtonStyle style = Styles.togglet;
    private final Cons<Cons<Seq<TagCategory>>> tagProvider;
    private float scale = 1;
    private final int CARD_GAP = 4;
    private Seq<String> modIds = new Seq<>();
    private String filterText = "";
    private Table resultsTable;
    private int rebuildToken = 0;

    private ModService modService = new ModService();
    private SearchConfig searchConfig;
    private boolean useBlocks = false;

    public FilterDialog(//
            TagService tagService,
            SearchConfig searchConfig, //
            Cons<Cons<Seq<TagCategory>>> tagProvider//
    ) {
        super("");
        setup();

        this.tagProvider = tagProvider;
        this.searchConfig = searchConfig;

        setFillParent(true);
        addCloseListener();

        // Register listeners once
        modService.onUpdate(() -> {
            if (isShown()) {
                rebuild();
            }
        });

        tagService.onUpdate(() -> {
            if (isShown()) {
                rebuild();
            }
        });

        onResize(() -> {
            if (isShown()) {
                rebuild();
            }
        });
    }

    public void show(SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        rebuild();
        super.show();
    }

    private void setup() {
        cont.table(t -> {
            t.image(Icon.zoom).padRight(8);
            t.field(filterText, text -> {
                filterText = text.toLowerCase();
                rebuildResults();
            }).growX().get().setMessageText(Core.bundle.get("message.search"));
        }).fillX().pad(10).row();

        cont.pane(container -> {
            resultsTable = container;
        }).grow().scrollY(true);
    }

    private void rebuildResults() {
        rebuild();
    }

    public FilterDialog setUseBlocks(boolean useBlocks) {
        this.useBlocks = useBlocks;
        return this;
    }

    private void rebuild() {
        if (searchConfig == null || resultsTable == null) {
            return;
        }

        try {
            int token = ++rebuildToken;
            scale = Vars.mobile ? 0.8f : 1f;

            // Calculate layout metrics based on SchematicDialog logic
            float availableWidth = Core.graphics.getWidth() / Scl.scl() * 0.9f;
            float targetW = Math.min(availableWidth, TARGET_WIDTH);

            // For grid-based selectors (Mod, Sort)
            int cols = Mathf.clamp((int) (availableWidth / targetW), 1, 20);
            float cardWidth = availableWidth / cols;

            resultsTable.clear();
            resultsTable.top().left();

            modService.getMod(mods -> {
                if (token != rebuildToken)
                    return;
                ModSelector(resultsTable, searchConfig, mods, cols, cardWidth);

                resultsTable.row();
                SortSelector(resultsTable, searchConfig, cols, cardWidth);
                resultsTable.row();

                tagProvider.get(categories -> {
                    if (token != rebuildToken)
                        return;
                    for (var category : categories.sort((a, b) -> a.getPosition() - b.getPosition())) {
                        TagSelector(resultsTable, searchConfig, category, availableWidth);
                    }

                    resultsTable.row();

                    if (useBlocks) {
                        BlockSelector(resultsTable, searchConfig, cols, cardWidth);
                    }
                });
            });

            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);

            addCloseButton();
        } catch (Exception e) {
            Log.err(e);
        }
    }

    public void BlockSelector(Table container, SearchConfig searchConfig, int cols, float cardWidth) {
        var blocks = Vars.content.blocks().select(block -> block.isVisible()
                && (filterText.isEmpty() || block.localizedName.toLowerCase().contains(filterText)));

        if (blocks.isEmpty()) {
            return;
        }

        container.table(Styles.flatOver,
                text -> text.add(Core.bundle.format("message.block"))
                        .fontScale(scale)
                        .left()
                        .labelAlign(Align.left))
                .top()
                .left()
                .expandX()
                .padBottom(4);

        container.row();
        container.table(card -> {
            card.defaults().size(cardWidth, 50); // Use calculated width
            int i = 0;
            for (var block : blocks) {
                card.button(btn -> {
                    btn.left();
                    btn.add(block.emoji())
                            .size(40 * scale)
                            .padRight(4)
                            .marginRight(4);
                    btn.add(block.localizedName).fontScale(scale);
                }, style,
                        () -> {
                            searchConfig.toggleBlock(block.name);
                        })
                        .checked(searchConfig.containBlock(block.name))
                        .padRight(CARD_GAP)
                        .padBottom(CARD_GAP)
                        .left()
                        .fillX()
                        .margin(12);

                if (++i % cols == 0) {
                    card.row();
                }
            }
        })
                .top()
                .left()
                .expandX()
                .padBottom(48);
    }

    public void ModSelector(Table table, SearchConfig searchConfig, Seq<ModData> mods, int cols, float cardWidth) {
        table.table(Styles.flatOver,
                text -> text.add(Core.bundle.format("message.mod"))
                        .fontScale(scale)
                        .left()
                        .labelAlign(Align.left))
                .top()
                .left()
                .expandX()
                .padBottom(4);

        table.row();
        table.table(card -> {
            card.defaults().size(cardWidth, 50); // Use calculated width
            int i = 0;
            for (var mod : mods.sort((a, b) -> a.getPosition() - b.getPosition())) {
                card.button(btn -> {
                    btn.left();
                    if (mod.getIcon() != null && !mod.getIcon().isEmpty()) {
                        btn.add(new NetworkImage(mod.getIcon()))
                                .size(40 * scale)
                                .padRight(4)
                                .marginRight(4);
                    }
                    btn.add(mod.getName()).fontScale(scale);
                }, style,
                        () -> {
                            if (modIds.contains(mod.getId())) {
                                modIds.remove(mod.getId());
                            } else {
                                modIds.add(mod.getId());
                            }
                            Core.app.post(this::rebuild);
                        })
                        .checked(modIds.contains(mod.getId()))
                        .padRight(CARD_GAP)
                        .padBottom(CARD_GAP)
                        .left()
                        .fillX()
                        .margin(12);

                if (++i % cols == 0) {
                    card.row();
                }
            }
        })
                .top()
                .left()
                .expandX()
                .scrollY(false)
                .padBottom(48);
    }

    public void SortSelector(Table table, SearchConfig searchConfig, int cols, float cardWidth) {
        var buttonGroup = new ButtonGroup<>();

        table.table(Styles.flatOver,
                text -> text.add(Core.bundle.format("message.sort"))
                        .fontScale(scale)
                        .left()
                        .labelAlign(Align.left))
                .top()
                .left()
                .expandX()
                .padBottom(4);

        table.row();
        table.table(card -> {
            card.defaults().size(cardWidth, 50); // Use calculated width
            int i = 0;
            for (var sort : Config.sorts) {
                card.button(btn -> btn.add(formatTag(sort.getName())).fontScale(scale), style, () -> {
                    searchConfig.setSort(sort);
                })
                        .group(buttonGroup)
                        .checked(sort.equals(searchConfig.getSort()))
                        .padRight(CARD_GAP)
                        .padBottom(CARD_GAP);

                if (++i % cols == 0) {
                    card.row();
                }
            }
        })
                .top()
                .left()
                .expandX()
                .scrollY(false)
                .padBottom(48);
    }

    public void TagSelector(Table table, SearchConfig searchConfig, TagCategory category, float availableWidth) {
        var tags = category.getTags()
                .stream().filter(value -> value.getPlanetIds() == null || value.getPlanetIds().size() == 0
                        || value.getPlanetIds().stream().anyMatch(t -> modIds.contains(t)))
                .filter(tag -> filterText.isEmpty() || formatTag(tag.getName()).toLowerCase().contains(filterText))
                .sorted((a, b) -> a.getPosition() - b.getPosition())
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            return;
        }

        table.table(Styles.flatOver,
                text -> text.add(category.getName())
                        .fontScale(scale)
                        .left()
                        .color(category.color())
                        .labelAlign(Align.left))
                .top()
                .left()
                .padBottom(4);

        table.row();

        // Flex layout using nested tables
        Table container = new Table();
        container.left();

        // Initial row table
        final Table[] currentRow = { new Table() };
        currentRow[0].left();
        container.add(currentRow[0]).left().row();

        float currentWidth = 0;

        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

        for (var tag : tags) {
            String tagName = formatTag(tag.getName());
            float iconSize = (tag.getIcon() != null && !tag.getIcon().isEmpty()) ? 40 * scale + 8 : 0;

            // Estimate button width
            float textWidth = 0;
            try {
                layout.setText(style.font, tagName);
                textWidth = layout.width * scale;
            } catch (Exception e) {
                textWidth = tagName.length() * 10 * scale; // Fallback
            }

            float buttonWidth = iconSize + textWidth + 24 + 10; // +24 margin, +10 safety buffer

            if (currentWidth + buttonWidth + CARD_GAP > availableWidth) {
                currentRow[0] = new Table();
                currentRow[0].left();
                container.add(currentRow[0]).left().row();
                currentWidth = 0;
            }

            currentRow[0].button(btn -> {
                btn.left();
                if (tag.getIcon() != null && !tag.getIcon().isEmpty()) {
                    btn.add(new NetworkImage(tag.getIcon()))
                            .size(40 * scale)
                            .padRight(4)
                            .marginRight(4);
                }
                btn.add(tagName).color(tag.color()).fontScale(scale);

            }, style, () -> {
                searchConfig.setTag(category, tag);
            })
                    .checked(searchConfig.containTag(category, tag))
                    .padRight(CARD_GAP)
                    .padBottom(CARD_GAP)
                    .left()
                    .margin(12);

            currentWidth += buttonWidth + CARD_GAP;
        }

        Pools.free(layout);

        table.add(container).width(availableWidth).left().padBottom(48);
        table.row();
    }

    private String formatTag(String name) {
        return name;
    }

}
