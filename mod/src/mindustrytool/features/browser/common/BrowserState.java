package mindustrytool.features.browser.common;

import arc.struct.Seq;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import mindustrytool.Config;
import solim.reactive.Effect;
import solim.reactive.Query;
import solim.reactive.Signal;

/**
 * Generic reactive state for a paged browser with search, tag filtering, and
 * sort. Pages are zero-based internally, matching the API convention.
 *
 * @param <T> the item type returned by the API
 */
public class BrowserState<T> {

    public static final int PAGE_SIZE = 20;

    private final Signal<Integer> pageSize = Signal.of(PAGE_SIZE);
    private final Signal<String> query = Signal.of("");
    private final Signal<Seq<String>> selectedTags = Signal.of(new Seq<String>());
    private final Signal<Seq<String>> selectedBlocks = Signal.of(new Seq<String>());
    private final Signal<String> sort = Signal.of(Config.sorts.get(0).getValue());
    private final Signal<String> verification = Signal.of("VERIFIED");
    private final Signal<Integer> page = Signal.of(0);
    private final Signal<Seq<T>> items = Signal.of(new Seq<T>());
    private final Signal<Boolean> loading = Signal.of(false);
    private final Signal<String> error = Signal.of(null);

    private final Signal<Boolean> active = Signal.of(false);
    private final Query<List<T>> queryPrimitive;

    @FunctionalInterface
    public interface Fetcher<T> {
        CompletableFuture<List<T>> fetch(BrowserState<T> state);
    }

    public BrowserState(Fetcher<T> fetcher) {
        this.queryPrimitive = Query.of(active, () -> {
            query.get();
            selectedTags.get();
            selectedBlocks.get();
            sort.get();
            verification.get();
            page.get();
            pageSize.get();
            return fetcher.fetch(this);
        });

        Effect.of(() -> {
            List<T> list = queryPrimitive.data().get();
            items.set(list != null ? Seq.with(list) : new Seq<T>());
        });
        Effect.of(() -> {
            loading.set(Boolean.TRUE.equals(queryPrimitive.loading().get()));
        });
        Effect.of(() -> {
            Throwable err = queryPrimitive.error().get();
            if (err != null) {
                Throwable cause = err.getCause() != null ? err.getCause() : err;
                String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                error.set(message);
                items.set(new Seq<T>());
            } else {
                error.set(null);
            }
        });
    }

    public void start() {
        active.set(true);
    }

    public void stop() {
        active.set(false);
    }

    public void refresh() {
        queryPrimitive.refetch();
    }

    public void nextPage() {
        page.update(current -> current != null ? current + 1 : 1);
    }

    public void prevPage() {
        page.update(current -> current != null ? Math.max(0, current - 1) : 0);
    }

    public void goToPage(int targetPage) {
        page.set(Math.max(0, targetPage));
    }

    public void resetPage() {
        page.set(0);
    }

    public void toggleTag(String tag) {
        if (tag == null) {
            return;
        }
        selectedTags.update(current -> {
            Seq<String> copy = current != null ? new Seq<String>(current) : new Seq<String>();
            if (copy.contains(tag)) {
                copy.remove(tag);
            } else {
                copy.add(tag);
            }
            return copy;
        });
        resetPage();
    }

    public void toggleBlock(String block) {
        if (block == null) {
            return;
        }
        selectedBlocks.update(current -> {
            Seq<String> copy = current != null ? new Seq<String>(current) : new Seq<String>();
            if (copy.contains(block)) {
                copy.remove(block);
            } else {
                copy.add(block);
            }
            return copy;
        });
        resetPage();
    }

    public void clearTags() {
        selectedTags.set(new Seq<String>());
        resetPage();
    }

    public void clearBlocks() {
        selectedBlocks.set(new Seq<String>());
        resetPage();
    }

    public void setSort(String sortValue) {
        if (sortValue == null) {
            return;
        }
        sort.set(sortValue);
        resetPage();
    }

    public void setVerification(String verificationValue) {
        if (verificationValue == null) {
            return;
        }
        verification.set(verificationValue);
        resetPage();
    }

    public Signal<String> query() {
        return query;
    }

    public Signal<Seq<String>> selectedTags() {
        return selectedTags;
    }

    public Signal<Seq<String>> selectedBlocks() {
        return selectedBlocks;
    }

    public Signal<String> sort() {
        return sort;
    }

    public Signal<String> verification() {
        return verification;
    }

    public Signal<Integer> page() {
        return page;
    }

    public Signal<Integer> pageSize() {
        return pageSize;
    }

    public int getPageSize() {
        Integer size = pageSize.peek();
        return size != null ? size : PAGE_SIZE;
    }

    public void setPageSize(int size) {
        int clamped = Math.min(BrowserLayout.PAGE_SIZE_MAX, Math.max(BrowserLayout.PAGE_SIZE_MIN, size));
        Integer current = pageSize.peek();
        if (current == null || current != clamped) {
            pageSize.set(clamped);
            resetPage();
        }
    }

    public Signal<Seq<T>> items() {
        return items;
    }

    public Signal<Boolean> loading() {
        return loading;
    }

    public Signal<String> error() {
        return error;
    }
}
