package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import solim.core.BaseComponent;

/**
 * Footer with Previous/Next buttons, direct page jump, and external upload
 * shortcut. Pages are zero-based internally and displayed one-based.
 */
public class BrowserFooter extends BaseComponent {

    private final BrowserState<?> state;
    private final String uploadUrl;
    private final @Nullable Runnable onClose;

    public BrowserFooter(BrowserState<?> state, String uploadUrl, @Nullable Runnable onClose) {
        this.state = state;
        this.uploadUrl = uploadUrl;
        this.onClose = onClose;
    }

    @Override
    protected Element build() {
        return row().growX().gap(unit(2)).children(() -> {
            if (onClose != null) {
                button(Core.bundle.get("browser.footer.close"), onClose)
                        .style(WebStyles.outlineText())
                        .height(unit(10))
                        .tooltip(Core.bundle.get("browser.footer.close.tooltip"));
            }

            spacer();

            button(Core.bundle.get("browser.footer.previous"), () -> state.prevPage())
                    .style(WebStyles.outlineText())
                    .height(unit(10))
                    .enabled(state.page().map(page -> page != null && page > 0))
                    .tooltip(Core.bundle.get("browser.footer.previous.tooltip"));

            button(state.page().map(page -> Core.bundle.format("browser.footer.page", page != null ? page + 1 : 1)),
                    this::showPageJumpDialog)
                    .style(WebStyles.outlineText())
                    .height(unit(10))
                    .tooltip(Core.bundle.get("browser.footer.page-jump.title"));

            button(Core.bundle.get("browser.footer.next"), () -> state.nextPage())
                    .style(WebStyles.outlineText())
                    .height(unit(10))
                    .tooltip(Core.bundle.get("browser.footer.next.tooltip"));

            spacer();

            button(Core.bundle.get("browser.footer.upload"), Icon.upload, () -> Core.app.openURI(uploadUrl))
                    .style(WebStyles.outlineText())
                    .height(unit(10))
                    .tooltip(Core.bundle.get("browser.footer.upload.tooltip"));
        }).element();
    }

    private void showPageJumpDialog() {
        Integer current = state.page().peek();
        int displayPage = current != null ? current + 1 : 1;
        Vars.ui.showTextInput(
                Core.bundle.get("browser.footer.page-jump.title"),
                Core.bundle.get("browser.footer.page-jump.prompt"),
                String.valueOf(displayPage),
                input -> {
                    try {
                        int page = Integer.parseInt(input.trim());
                        state.goToPage(page - 1);
                    } catch (NumberFormatException ignored) {
                    }
                });
    }
}
