package mindustrytool.ui;

import arc.scene.ui.layout.Table;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.struct.Seq;
import arc.struct.ObjectSet;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ctype.UnlockableContent;

/**
 * A reusable table for selecting content from a list.
 * Designed for "Hide/Show" features and future extensibility.
 */
public class ContentSelectionTable extends Table {

    private final Seq<? extends UnlockableContent> contentList;
    private final ObjectSet<UnlockableContent> disabledSet; // Stores items that are DISABLED (Hidden)
    private final Runnable onChange;

    public ContentSelectionTable(Seq<? extends UnlockableContent> content, ObjectSet<UnlockableContent> disabledSet,
            Runnable onChange) {
        this.contentList = content;
        this.disabledSet = disabledSet;
        this.onChange = onChange;

        setup();
    }

    private void setup() {
        // Controls: Select All / None
        table(t -> {
            t.button("Show All", Styles.flatt, () -> {
                disabledSet.clear(); // Enable all (remove from disabled set)
                rebuildGrid();
                if (onChange != null)
                    onChange.run();
            }).size(120, 40).padRight(10);

            t.button("Hide All", Styles.flatt, () -> {
                disabledSet.addAll(contentList); // Disable all
                rebuildGrid();
                if (onChange != null)
                    onChange.run();
            }).size(120, 40);
        }).padBottom(10).row();

        // Grid
        rebuildGrid();
    }

    private void rebuildGrid() {
        if (getChildren().size > 1)
            getChildren().get(1).remove(); // Remove old scroll pane if exists

        Table grid = new Table();
        grid.defaults().size(48).pad(2);

        int cols = 0;
        int maxCols = 8; // Responsive? Fixed for now.

        for (UnlockableContent item : contentList) {
            boolean isEnabled = !disabledSet.contains(item);

            // Fix: Use consistent ImageButtonStyle. Styles.togglet is for TextButtons.
            // Styles.clearTogglei is good for image toggles.
            ImageButton btn = new ImageButton(new arc.scene.style.TextureRegionDrawable(item.uiIcon),
                    Styles.clearTogglei);
            btn.setChecked(isEnabled);

            btn.clicked(() -> {
                if (disabledSet.contains(item)) {
                    disabledSet.remove(item); // Enable it
                } else {
                    disabledSet.add(item); // Disable it
                }
                // Update visual state without full rebuild
                btn.setChecked(!disabledSet.contains(item));

                if (onChange != null)
                    onChange.run();
            });

            // Tooltip
            btn.addListener(new arc.scene.ui.Tooltip(
                    t -> t.background(Tex.button).add(item.localizedName).style(Styles.outlineLabel)));

            grid.add(btn);

            cols++;
            if (cols >= maxCols) {
                grid.row();
                cols = 0;
            }
        }

        ScrollPane pane = new ScrollPane(grid, Styles.smallPane);
        add(pane).grow();
    }
}
