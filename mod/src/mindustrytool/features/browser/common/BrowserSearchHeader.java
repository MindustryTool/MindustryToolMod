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
import solim.signal.Signal;

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
            state.query().set(value);
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

                card(Styles.black5).growX().height(unit(10)).children(() -> {
                    row().grow().gap(unit(1))
                            .rounded(unit(3), Color.clear)
                            .padLeft(unit(2))
                            .border(1.5f, Color.darkGray)
                            .children(() -> {
                                icon(Icon.zoom).size(unit(5));
                                textField(inputBuffer)
                                        .growX()
                                        .height(unit(8))
                                        .style(WebStyles.clearInput())
                                        .placeholder(Core.bundle.get("browser.search.placeholder"))
                                        .onEnter(this::submitNow);
                            });
                });

                button(this::refresh).style(WebStyles.outlineText()).size(unit(10))
                        .tooltip(Core.bundle.get("browser.search.refresh"))
                        .children(() -> icon(Icon.refresh).size(unit(5)));

                button(onFilterClick).style(WebStyles.outlineText()).size(unit(10))
                        .tooltip(Core.bundle.get("browser.search.filter"))
                        .children(() -> icon(Icon.filter).size(unit(5)));
            });

            dynamic(state.selectedTags(), tags -> {
                if (tags == null || tags.isEmpty()) {
                    return null;
                }
                return row().growX().gap(unit(1)).children(() -> {
                    text(Core.bundle.get("browser.search.active-filters"))
                            .color(Color.lightGray)
                            .fontScale(0.85f);

                    row().gap(unit(1)).children(() -> {
                        for (String tag : tags) {
                            renderChip(tag);
                        }
                        button(() -> state.clearTags()).style(Styles.clearNonei).size(unit(8))
                                .tooltip(Core.bundle.get("browser.search.clear-all"))
                                .children(() -> icon(Icon.cancel).size(unit(4)).color(Color.scarlet));
                    });
                });
            });
        }).element();
    }

    private void renderChip(String tag) {
        button(tag, Icon.cancelSmall, () -> state.toggleTag(tag))
                .style(Styles.cleart)
                .height(unit(8));
    }

    private void submitNow() {
        cancelPendingSearch();
        String value = inputBuffer.peek();
        state.query().set(value != null ? value.trim() : "");
        state.resetPage();
    }

    private void refresh() {
        cancelPendingSearch();
        inputBuffer.set(state.query().peek() != null ? state.query().peek() : "");
        state.refresh();
    }
}
