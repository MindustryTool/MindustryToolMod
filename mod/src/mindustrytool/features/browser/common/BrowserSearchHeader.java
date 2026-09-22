package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Timer;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import solim.core.BaseComponent;
import solim.reactive.Signal;

import mindustrytool.components.WebStyles;

/**
 * Search header with debounced text input, filter trigger button, refresh
 * button, and reactive active filter chips.
 */
public class BrowserSearchHeader extends BaseComponent {

    private static final float DEBOUNCE_SECONDS = 0.5f;

    private final BrowserState<?> state;
    private final Signal<String> inputBuffer = Signal.of("");
    private final Runnable onFilterClick;
    private @Nullable Timer.Task pendingTask;

    public BrowserSearchHeader(BrowserState<?> state, Runnable onFilterClick) {
        this.state = state;
        this.onFilterClick = onFilterClick;

        effect(() -> {
            String text = inputBuffer.get();
            scheduleSearch(text);
        });
    }

    @Override
    protected void onDispose() {
        cancelPendingSearch();
    }

    private void scheduleSearch(String text) {
        cancelPendingSearch();
        final String value = text != null ? text.trim() : "";
        pendingTask = Timer.schedule(() -> {
            if (isDisposed()) {
                return;
            }
            state.searchQuery().set(value);
            state.resetPage();
        }, DEBOUNCE_SECONDS);
    }

    private void cancelPendingSearch() {
        if (pendingTask != null) {
            pendingTask.cancel();
            pendingTask = null;
        }
    }

    @Override
    protected Element build() {
        return column().growX().gap(unit(2)).children(() -> {
            row().growX().gap(unit(2)).children(() -> {

                row().grow().gap(unit(1))
                        .paddingLeft(unit(2))
                        .rounded(unit(3), Color.clear)
                        .gap(unit(2))
                        .border(1.5f, Color.darkGray)
                        .center()
                        .children(() -> {
                            icon(Icon.zoom).size(unit(6));
                            textField(inputBuffer)
                                    .growX()
                                    .height(unit(11))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("browser.search.placeholder"))
                                    .onEnter(this::submitNow);
                        });

                button(this::refresh).style(WebStyles.outlineText()).size(unit(11))
                        .tooltip(Core.bundle.get("browser.search.refresh"))
                        .children(() -> icon(Icon.refresh).size(unit(6)));

                button(onFilterClick).style(WebStyles.outlineText()).size(unit(11))
                        .tooltip(Core.bundle.get("browser.search.filter"))
                        .children(() -> icon(Icon.filter).size(unit(6)));
            });

            dynamic(state.selectedTags(), tags -> {
                if (tags != null && !tags.isEmpty()) {
                    row().growX().gap(unit(1)).children(() -> {
                        wrap().children(() -> {
                            row().gap(unit(1)).children(() -> {
                                for (String tag : tags) {
                                    renderChip(tag);
                                }
                            });
                        });
                    });
                }
            });
        }).element();
    }

    private void renderChip(String tag) {
        button(tag, () -> state.toggleTag(tag))
                .gap(unit(1))
                .style(Styles.cleart)
                .border(1.5f, Color.lightGray)
                .rounded(6)
                .paddingX(unit(3))
                .height(unit(9))
                .children(() -> icon(Icon.cancel));
    }

    private void submitNow() {
        cancelPendingSearch();
        String value = inputBuffer.peek();
        state.searchQuery().set(value != null ? value.trim() : "");
        state.resetPage();
    }

    private void refresh() {
        cancelPendingSearch();
        inputBuffer.set(state.searchQuery().peek() != null ? state.searchQuery().peek() : "");
        state.refresh();
    }
}
