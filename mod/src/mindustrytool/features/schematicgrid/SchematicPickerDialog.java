package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.layout.Scroll;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Solim picker for local schematics with search, tag filtering, and preview cards.
 */
public class SchematicPickerDialog extends SolimDialog {

    public static final int INITIAL_BATCH = 36;
    public static final int BATCH_STEP = 36;
    public static final int MAX_PREVIEWS_PER_FRAME = 2;

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<String> selectedTag = Signal.of((String) null);
    private final Signal<Integer> displayCount = Signal.of(INITIAL_BATCH);
    private @Nullable Scroll gridScroll;

    public SchematicPickerDialog(Consumer<Schematic> onSelect) {
        super(Core.bundle.get("feature.quick-schematic-grid.picker.title"));

        name("schematicPickerDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(1000f);

        Seq<Schematic> all = snapshotSchematics();
        Seq<String> tags = collectTags(all);

        Readable<Seq<Schematic>> filtered = Signal.computed(() -> filter(all, searchQuery.get(), selectedTag.get()));
        Readable<Seq<Schematic>> displayed = Signal.computed(() -> takeFirst(filtered.get(), displayCount.get()));
        Readable<Integer> remaining = Signal.computed(() -> {
            Seq<Schematic> full = filtered.get();
            Seq<Schematic> shown = displayed.get();
            int total = full != null ? full.size : 0;
            int shownCount = shown != null ? shown.size : 0;
            return Math.max(0, total - shownCount);
        });

        Disposable searchSub = searchQuery.subscribe(query -> resetBatch());
        Disposable tagSub = selectedTag.subscribe(tag -> resetBatch());
        registerDisposable(searchSub);
        registerDisposable(tagSub);

        Computed<Float> viewportWidth = dvw(100f);
        Readable<Boolean> portrait = isPortrait();
        Computed<Integer> columnCount = new Computed<>(() -> {
            Float width = viewportWidth.get();
            float w = width != null ? width : 800f;
            if (Boolean.TRUE.equals(portrait.get())) {
                return w < 500f ? 2 : 3;
            }
            if (w < 650f) {
                return 2;
            }
            if (w < 1100f) {
                return 3;
            }
            return Math.max(3, Math.min(5, (int) (w / 260f)));
        });

        children(() -> {
            column().grow().gap(unit(2.5f)).padding(unit(3)).center().children(() -> {
                row().growX().gap(unit(1.5f))
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .center()
                        .children(() -> {
                            icon(Icon.zoom).size(unit(4.5f)).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .growX()
                                    .height(unit(8))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.quick-schematic-grid.picker.search"));
                        });

                scroll().minHeight(unit(16)).growX().children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        tagChip(null, tags);
                        for (String tag : tags) {
                            tagChip(tag, tags);
                        }
                    });
                });

                gridScroll = scroll().grow().onReachBottom(100f, () -> expandBatch(filtered));
                gridScroll.children(() -> {
                    reactiveGrid(displayed).columns(columnCount).key(SchematicPickerDialog::keyOf)
                            .empty(() -> {
                                text(Core.bundle.get("feature.quick-schematic-grid.picker.empty"))
                                        .color(Color.gray)
                                        .padding(unit(4));
                            })
                            .gap(unit(3))
                            .children(schematic -> new SchematicPickerCard(schematic, () -> {
                                if (onSelect != null) {
                                    onSelect.accept(schematic);
                                }
                                hide();
                            }));

                    dynamic(remaining, left -> {
                        Integer count = left != null ? left : 0;
                        if (count <= 0) {
                            return null;
                        }
                        String label = Core.bundle.format("feature.quick-schematic-grid.picker.load-more", count);
                        return button(label, () -> expandBatch(filtered))
                                .style(WebStyles.secondary())
                                .growX()
                                .height(unit(9f));
                    });
                });
            });
        });
    }

    private Component tagChip(@Nullable String tag, Seq<String> tags) {
        String label = tag == null
                ? Core.bundle.get("feature.quick-schematic-grid.picker.tag.all")
                : tag;
        return button()
                .style(WebStyles.filterChip())
                .checked(selectedTag.map(sel -> tag == null ? sel == null : tag.equals(sel)))
                .onClick(() -> selectedTag.set(tag))
                .padding(unit(1.5f))
                .tooltip(label)
                .children(() -> text(label));
    }

    private void resetBatch() {
        displayCount.set(INITIAL_BATCH);
        Scroll pane = gridScroll;
        if (pane != null) {
            try {
                Core.app.post(pane::scrollToTop);
            } catch (Exception ignored) {
            }
        }
    }

    private void expandBatch(Readable<Seq<Schematic>> filtered) {
        Seq<Schematic> full = filtered != null ? filtered.peek() : null;
        int total = full != null ? full.size : 0;
        Integer current = displayCount.peek();
        int shown = current != null ? current : INITIAL_BATCH;
        if (shown < total) {
            displayCount.set(Math.min(total, shown + BATCH_STEP));
        }
    }

    static Seq<Schematic> takeFirst(@Nullable Seq<Schematic> all, @Nullable Integer count) {
        Seq<Schematic> result = new Seq<>();
        if (all == null) {
            return result;
        }
        int limit = count != null ? count : INITIAL_BATCH;
        int capped = Math.max(0, Math.min(limit, all.size));
        for (int i = 0; i < capped; i++) {
            result.add(all.get(i));
        }
        return result;
    }

    private static Seq<Schematic> snapshotSchematics() {
        try {
            Seq<Schematic> all = Vars.schematics.all();
            return all != null ? new Seq<>(all) : new Seq<>();
        } catch (Exception ignored) {
            return new Seq<>();
        }
    }

    private static Seq<String> collectTags(Seq<Schematic> all) {
        Seq<String> tags = new Seq<>();
        if (all == null) {
            return tags;
        }
        for (Schematic schematic : all) {
            if (schematic == null || schematic.labels == null) {
                continue;
            }
            for (String label : schematic.labels) {
                if (label != null && !label.trim().isEmpty() && !tags.contains(label)) {
                    tags.add(label);
                }
            }
        }
        tags.sort();
        return tags;
    }

    static Seq<Schematic> filter(@Nullable Seq<Schematic> all, @Nullable String query, @Nullable String tag) {
        Seq<Schematic> result = new Seq<>();
        if (all == null) {
            return result;
        }
        String lower = query != null ? query.trim().toLowerCase() : "";
        boolean hasQuery = !lower.isEmpty();
        boolean hasTag = tag != null && !tag.trim().isEmpty();
        for (Schematic schematic : all) {
            if (schematic == null) {
                continue;
            }
            if (hasTag && (schematic.labels == null || !schematic.labels.contains(tag))) {
                continue;
            }
            if (hasQuery) {
                String name = schematic.name() != null ? schematic.name().toLowerCase() : "";
                String desc = schematic.description() != null ? schematic.description().toLowerCase() : "";
                if (!name.contains(lower) && !desc.contains(lower)) {
                    continue;
                }
            }
            result.add(schematic);
        }
        return result;
    }

    static String keyOf(Schematic schematic) {
        if (schematic.file != null && schematic.file.name() != null) {
            return schematic.file.name();
        }
        String name = schematic.name() != null ? schematic.name() : "schematic";
        return name + "@" + schematic.width + "x" + schematic.height;
    }

    static List<String> collectTagList(List<String> labels) {
        List<String> tags = new ArrayList<>();
        if (labels == null) {
            return tags;
        }
        for (String label : labels) {
            if (label != null && !label.trim().isEmpty() && !tags.contains(label)) {
                tags.add(label);
            }
        }
        return tags;
    }

    /**
     * Schematic card showing preview thumbnail, title, dimensions, and selection callback.
     * Shows a lightweight placeholder while the preview texture is generated through
     * a throttled queue (at most 2 FrameBuffers per frame) to keep 60 FPS.
     */
    public static class SchematicPickerCard extends BaseComponent {

        private final Schematic schematic;
        private final Runnable onClick;
        private final Signal<Boolean> previewReady = Signal.of(false);

        public SchematicPickerCard(Schematic schematic, Runnable onClick) {
            this.schematic = schematic;
            this.onClick = onClick;
            boolean cached = true;
            try {
                cached = Vars.schematics == null || Vars.schematics.hasPreview(schematic);
            } catch (Exception ignored) {
                cached = true;
            }
            previewReady.set(cached);
            if (!cached) {
                PreviewQueue.enqueue(schematic, previewReady, this);
            }
        }

        @Override
        protected Element build() {
            String title = schematic.name() != null && !schematic.name().trim().isEmpty()
                    ? schematic.name()
                    : Core.bundle.get("browser.schematic.unnamed");
            String dimensions = schematic.width + " x " + schematic.height;

            return column()
                    .growX()
                    .gap(unit(1.5f))
                    .children(() -> {
                        dynamic(previewReady, ready -> {
                            if (Boolean.TRUE.equals(ready)) {
                                return card(WebStyles.previewCardBackground())
                                        .growX()
                                        .height(unit(42f))
                                        .onClick(onClick)
                                        .tooltip(title)
                                        .children(() -> {
                                            new BoundedSchematicImage(
                                                    schematic, Readable.of(unit(42f)), unit(42f));
                                        });
                            }
                            return card(WebStyles.previewCardBackground())
                                    .growX()
                                    .height(unit(42f))
                                    .onClick(onClick)
                                    .tooltip(title)
                                    .children(() -> {
                                        column().grow().center().gap(unit(1f)).children(() -> {
                                            icon(Icon.image).size(unit(6f)).color(Color.lightGray);
                                            text(Core.bundle.get(
                                                    "feature.quick-schematic-grid.picker.preview-generating"))
                                                    .color(Color.lightGray)
                                                    .fontScale(0.8f)
                                                    .center();
                                        });
                                    });
                        });

                        text(title)
                                .growX()
                                .left()
                                .ellipsis(true)
                                .color(Color.white);

                        text(dimensions)
                                .growX()
                                .left()
                                .color(Color.lightGray)
                                .fontScale(0.85f);
                    }).element();
        }
    }

    /**
     * Throttled FIFO queue for schematic preview generation.
     * FrameBuffer creation must run on the main GL thread, so tasks are pumped
     * via {@code Core.app.post()} at most 2 previews per frame.
     */
    static final class PreviewQueue {
        private final Seq<PreviewTask> queue = new Seq<>();
        private boolean scheduled = false;

        private static final PreviewQueue INSTANCE = new PreviewQueue();

        private PreviewQueue() {
        }

        static void enqueue(Schematic schematic, Signal<Boolean> ready, BaseComponent owner) {
            if (schematic == null || ready == null) {
                return;
            }
            if (Boolean.TRUE.equals(ready.peek())) {
                return;
            }
            INSTANCE.enqueueInternal(schematic, ready, owner);
        }

        private synchronized void enqueueInternal(Schematic schematic, Signal<Boolean> ready, BaseComponent owner) {
            for (PreviewTask existing : queue) {
                if (existing != null && existing.schematic == schematic && existing.ready == ready) {
                    return;
                }
            }
            queue.add(new PreviewTask(schematic, ready, owner));
            ensureScheduled();
        }

        private void ensureScheduled() {
            if (scheduled) {
                return;
            }
            scheduled = true;
            try {
                if (Core.app != null) {
                    Core.app.post(this::pump);
                } else {
                    pump();
                }
            } catch (Exception ignored) {
                scheduled = false;
            }
        }

        private void pump() {
            int processed = 0;
            while (processed < MAX_PREVIEWS_PER_FRAME) {
                PreviewTask task;
                synchronized (this) {
                    if (queue.isEmpty()) {
                        break;
                    }
                    task = queue.remove(0);
                }
                if (task == null) {
                    continue;
                }
                if (task.owner != null && task.owner.isDisposed()) {
                    continue;
                }
                try {
                    boolean has;
                    try {
                        has = Vars.schematics == null || Vars.schematics.hasPreview(task.schematic);
                    } catch (Exception ignored) {
                        has = true;
                    }
                    if (!has) {
                        try {
                            if (Vars.schematics != null) {
                                Vars.schematics.getPreview(task.schematic);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } finally {
                    try {
                        if (task.owner == null || !task.owner.isDisposed()) {
                            task.ready.set(true);
                        }
                    } catch (Exception ignored) {
                    }
                }
                processed++;
            }
            synchronized (this) {
                if (queue.isEmpty()) {
                    scheduled = false;
                    return;
                }
            }
            try {
                if (Core.app != null) {
                    Core.app.post(this::pump);
                } else {
                    scheduled = false;
                }
            } catch (Exception ignored) {
                synchronized (this) {
                    scheduled = false;
                }
            }
        }

        synchronized int size() {
            return queue.size;
        }

        synchronized void clear() {
            queue.clear();
            scheduled = false;
        }

        private static final class PreviewTask {
            final Schematic schematic;
            final Signal<Boolean> ready;
            final @Nullable BaseComponent owner;

            PreviewTask(Schematic schematic, Signal<Boolean> ready, @Nullable BaseComponent owner) {
                this.schematic = schematic;
                this.ready = ready;
                this.owner = owner;
            }
        }
    }
}
