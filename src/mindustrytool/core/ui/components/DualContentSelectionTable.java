package mindustrytool.core.ui.components;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

public class DualContentSelectionTable extends Table {

    private final Seq<? extends UnlockableContent> allContent;
    private final ObjectSet<UnlockableContent> hiddenSet;
    private final String bannedTitle;
    private final String unbannedTitle;
    private final Runnable onChange;
    private String searchText = "";

    private Table contentTable;

    public DualContentSelectionTable(Seq<? extends UnlockableContent> content, ObjectSet<UnlockableContent> hiddenSet,
            String bannedTitle, String unbannedTitle, Runnable onChange) {
        this.allContent = content;
        this.hiddenSet = hiddenSet;
        this.bannedTitle = bannedTitle;
        this.unbannedTitle = unbannedTitle;
        this.onChange = onChange;
        setup();
    }

    private void setup() {
        // Search Bar
        table(t -> {
            t.image(Icon.zoom).padRight(8);
            t.field(searchText, text -> {
                searchText = text;
                rebuild();
            }).growX();
        }).fillX().padBottom(10).row();

        // Content Container
        contentTable = new Table();
        add(contentTable).grow().row();

        // Initial Build
        rebuild();
    }

    private void rebuild() {
        contentTable.clear();

        Seq<UnlockableContent> hiddenList = new Seq<>();
        Seq<UnlockableContent> visibleList = new Seq<>();

        for (UnlockableContent item : allContent) {
            // Filter: Search Text
            if (!searchText.isEmpty() && !item.localizedName.toLowerCase().contains(searchText.toLowerCase())) {
                continue;
            }
            // Filter: Missing icons (Internal/invalid content usually has no icon)
            if (!item.uiIcon.found()) {
                continue;
            }

            if (hiddenSet.contains(item)) {
                hiddenList.add(item);
            } else {
                visibleList.add(item);
            }
        }

        // Layout: Two Columns
        contentTable.table(main -> {
            main.defaults().grow().uniform();

            // Left: Hidden (Banned) - Red
            main.table(t -> buildColumn(t, bannedTitle, hiddenList, Color.scarlet, true)).margin(4);

            // Right: Visible (Unbanned) - Yellow (Game Style)
            main.table(t -> buildColumn(t, unbannedTitle, Color.orange, visibleList, false)).margin(4);

        }).grow();
    }

    private void buildColumn(Table t, String title, Seq<UnlockableContent> items, Color color, boolean isHiddenSide) {
        buildColumn(t, title, color, items, isHiddenSide);
    }

    private void buildColumn(Table t, String title, Color color, Seq<UnlockableContent> items, boolean isHiddenSide) {
        t.background(Tex.pane);

        t.add(title).color(color).pad(4).labelAlign(arc.util.Align.center).row();
        t.image().color(color).height(3).growX().padBottom(4).row();

        // Use a Table with wrapping for better mobile support
        Table grid = new Table();
        grid.top().left();

        if (items.isEmpty()) {
            grid.add("<empty>").color(Color.gray).pad(10);
        } else {
            // Check available width dynamically for responsive layout
            float screenWidth = Core.graphics.getWidth();
            // In portrait, we usually stack columns, so full width. In landscape, half
            // width.
            boolean isPortrait = Core.graphics.isPortrait();
            float availableWidth = isPortrait ? screenWidth - 60f : (screenWidth / 2f) - 40f;

            // Calculate columns - button size is 42 + 4 pad
            int buttonSize = 46;
            int cols = Math.max(1, (int) (availableWidth / buttonSize));

            int i = 0;
            for (UnlockableContent item : items) {
                grid.button(new arc.scene.style.TextureRegionDrawable(item.uiIcon), Styles.clearTogglei, 32, () -> {
                    toggle(item);
                }).size(42).pad(2).tooltip(item.localizedName);

                if (++i % cols == 0) {
                    grid.row();
                }
            }
        }

        ScrollPane pane = new ScrollPane(grid);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);

        t.add(pane).grow().row();

        t.button(isHiddenSide ? "Unban All" : "Ban All", Styles.flatBordert, () -> {
            moveAll(items, !isHiddenSide);
        }).growX().height(40).padTop(4);
    }

    private void toggle(UnlockableContent item) {
        if (hiddenSet.contains(item))
            hiddenSet.remove(item);
        else
            hiddenSet.add(item);
        rebuild();
        if (onChange != null)
            onChange.run();
    }

    private void moveAll(Seq<UnlockableContent> items, boolean hide) {
        if (hide)
            hiddenSet.addAll(items);
        else
            for (UnlockableContent item : items)
                hiddenSet.remove(item);
        rebuild();
        if (onChange != null)
            onChange.run();
    }
}
