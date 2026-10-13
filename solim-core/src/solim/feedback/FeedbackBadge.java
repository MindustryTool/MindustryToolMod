package solim.feedback;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.func.Cons;
import solim.core.Disposable;
import solim.reactive.Computed;
import solim.reactive.Effect;

public final class FeedbackBadge implements Disposable {
    private final Table table = new Table();
    private final Label label = new Label("");
    private Effect binding;
    private boolean disposed = false;

    public FeedbackBadge() {
        table.add(label);
    }

    public FeedbackBadge(Computed<String> text) {
        table.add(label);
        this.binding = Effect.of((Cons<Effect.Cleanup>) cleanup -> {
            label.setText(text.get());
        });
    }

    public static FeedbackBadge of(Computed<String> text) {
        return new FeedbackBadge(text);
    }

    public Table table() {
        return table;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (binding != null)
            binding.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
