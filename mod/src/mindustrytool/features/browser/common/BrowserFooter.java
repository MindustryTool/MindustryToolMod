package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import solim.core.Component;

import mindustrytool.components.WebStyles;

public class BrowserFooter extends Component {

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
        return row().props(growX(), gap(unit(2))).children(() -> {
            if (onClose != null) {
                button(Core.bundle.get("browser.footer.close"), onClose)
                        .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("browser.footer.close.tooltip")));
            }

            spacer();

            button(() -> state.prevPage())
                    .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)), enabled(state.page().map(page -> page != null && page > 0)), tooltip(Core.bundle.get("browser.footer.previous.tooltip")))
                    .children(() -> icon(Icon.left));

            button(state.page().map(page -> Core.bundle.format("browser.footer.page", page != null ? page + 1 : 1)),
                    this::showPageJumpDialog)
                    .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("browser.footer.page-jump.title")));

            button(() -> state.nextPage())
                    .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("browser.footer.next.tooltip")))
                    .children(() -> icon(Icon.right));

            spacer();

            button(Core.bundle.get("browser.footer.upload"), () -> Core.app.openURI(uploadUrl))
                    .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("browser.footer.upload.tooltip")));
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
