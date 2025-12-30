package mindustrytool.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Dialog that displays the mod's README.md documentation.
 * Uses MarkdownRenderer to display rich content including images, lists, and
 * formatting.
 */
public class ModInfoDialog extends BaseDialog {

    private static final String README_URL = "https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/main/README.md";

    private Table contentTable;
    private boolean loading = true;
    private String errorMessage = null;
    private String cachedReadme = null;

    public ModInfoDialog() {
        super("Mod Info");

        addCloseButton();

        // Add GitHub button
        buttons.button("GitHub", Icon.github, () -> {
            Core.app.openURI("https://github.com/MindustryTool/MindustryToolMod");
        });

        // Add Website button
        buttons.button("Website", Icon.planet, () -> {
            Core.app.openURI("https://mindustry-tool.com");
        });

        // Add Discord button
        buttons.button("Discord", Icon.players, () -> {
            Core.app.openURI("https://discord.gg/nQDrEHVkrt");
        });

        shown(this::onShown);
    }

    private void onShown() {
        if (cachedReadme != null) {
            // Use cached version
            loading = false;
            rebuildUI();
        } else {
            loading = true;
            errorMessage = null;
            rebuildUI();
            fetchReadme();
        }
    }

    private void fetchReadme() {
        Http.get(README_URL, response -> {
            try {
                cachedReadme = response.getResultAsString();

                // Clean up HTML-heavy content for better rendering
                cachedReadme = cleanupForRendering(cachedReadme);

                loading = false;
                Core.app.post(this::rebuildUI);
            } catch (Exception e) {
                Log.err("Failed to parse README", e);
                errorMessage = "Failed to parse README: " + e.getMessage();
                loading = false;
                Core.app.post(this::rebuildUI);
            }
        }, error -> {
            Log.err("Failed to fetch README", error);
            errorMessage = "Network error: " + error.getMessage();
            loading = false;
            Core.app.post(this::rebuildUI);
        });
    }

    /**
     * Clean up README content for better rendering in Mindustry UI.
     * Removes complex HTML, converts some elements to Markdown.
     */
    private String cleanupForRendering(String content) {
        // Remove HTML alignment tags (not supported)
        content = content.replaceAll("<p align=\"center\">", "");
        content = content.replaceAll("</p>", "");
        content = content.replaceAll("<h1 align=\"center\">", "# ");
        content = content.replaceAll("</h1>", "");
        content = content.replaceAll("<br>", "\n");
        content = content.replaceAll("<br/>", "\n");

        // Convert HTML links to Markdown
        content = content.replaceAll("<a href=\"([^\"]+)\">([^<]+)</a>", "[$2]($1)");

        // Remove HTML image tags (Markdown images will be handled)
        content = content.replaceAll("<img[^>]+>", "");

        // Remove details/summary tags (not supported)
        content = content.replaceAll("<details>", "");
        content = content.replaceAll("</details>", "");
        content = content.replaceAll("<summary>([^<]+)</summary>", "**$1**\n");

        // Remove mermaid code blocks (not renderable in Mindustry)
        content = content.replaceAll("```mermaid[\\s\\S]*?```", "[Diagram - View on GitHub]");

        // Remove HTML style attributes
        content = content.replaceAll("style=\"[^\"]+\"", "");

        // Remove empty lines created by HTML removal (multiple newlines -> double
        // newline)
        content = content.replaceAll("\n{3,}", "\n\n");

        // Remove HTML comments
        content = content.replaceAll("<!--[\\s\\S]*?-->", "");

        // Limit content length to avoid overwhelming mobile devices
        if (content.length() > 15000) {
            // Find a good cut-off point (after a section)
            int cutoff = content.indexOf("## 📸 Screenshots");
            if (cutoff > 0) {
                content = content.substring(0, cutoff)
                        + "\n\n---\n\n**[See full README on GitHub for more sections...]**";
            }
        }

        return content.trim();
    }

    private void rebuildUI() {
        cont.clear();

        if (loading) {
            cont.add("Loading README...").color(Color.lightGray).pad(50f);
            return;
        }

        if (errorMessage != null) {
            cont.table(t -> {
                t.image(Icon.warning).size(48f).color(Pal.remove).padBottom(10f).row();
                t.add("Failed to load README").color(Pal.remove).padBottom(5f).row();
                t.add(errorMessage).color(Color.gray).wrap().width(400f).padBottom(15f).row();
                t.button("Retry", Icon.refresh, Styles.flatt, () -> {
                    loading = true;
                    errorMessage = null;
                    cachedReadme = null;
                    rebuildUI();
                    fetchReadme();
                }).size(120f, 40f);
            }).pad(30f);
            return;
        }

        // Main content
        contentTable = new Table();
        contentTable.defaults().left().padBottom(5f);

        // Use MarkdownRenderer
        MarkdownRenderer renderer = new MarkdownRenderer()
                .setContentWidth(550f)
                .setMaxImageSize(500f, 350f);
        renderer.render(contentTable, cachedReadme);

        // Scroll pane
        ScrollPane scroll = new ScrollPane(contentTable, Styles.smallPane);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        cont.add(scroll).grow().pad(10f);
    }

    /**
     * Static method to open the dialog.
     */
    public static void open() {
        new ModInfoDialog().show();
    }
}
