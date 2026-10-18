package mindustrytool.features.browser.common;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.paddingLeft;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Timer;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import solim.core.Component;
import solim.modifier.Values;
import solim.reactive.Signal;

import mindustrytool.components.WebStyles;

public class BrowserSearchHeader extends Component {

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
        return column().props(growX(), gap(unit(2))).children(() -> {
            row().props(growX(), gap(unit(2))).children(() -> {

                row().props(grow(), gap(unit(2)), paddingLeft(unit(2)), rounded(unit(3), Color.clear), border(1.5f, Color.darkGray), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                            textField(inputBuffer)
                                    .props(style(WebStyles.clearInput()), growX(), height(unit(11)), prefHeight(unit(11)),
                                            Values.placeholder(Core.bundle.get("browser.search.placeholder")))
                                    .onEnter(this::submitNow);
                        });

                button(this::refresh).props(style(WebStyles.outlineText()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("browser.search.refresh")))
                        .children(() -> icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                button(onFilterClick).props(style(WebStyles.outlineText()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("browser.search.filter")))
                        .children(() -> icon(Icon.filter).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
            });

            dynamic(state.selectedTags(), tags -> {
                if (tags != null && !tags.isEmpty()) {
                    row().props(growX(), gap(unit(1))).children(() -> {
                        wrap().children(() -> {
                            row().props(gap(unit(1))).children(() -> {
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
                .props(style(Styles.cleart), border(1.5f, Color.lightGray), rounded(6), paddingX(unit(3)), height(unit(9)), prefHeight(unit(9)), gap(unit(1)))
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
