package solim.overlay;

import arc.graphics.Color;
import arc.Core;
import solim.reactive.Readable;
import arc.Events;
import arc.func.Cons;
import arc.func.Func;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.List;
import arc.func.Prov;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.DisposableAction;
import solim.graphics.RoundedDrawable;
import solim.modifier.RoundedHelper;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Signal;

/**
 * Declarative dialog component for Solim. Wraps a Mindustry BaseDialog while
 * providing full declarative configuration, reactive signal creation, and clean
 * lifecycle management.
 */
public class SolimDialog implements Component {

    private final BaseDialog wrapped;
    private final List<Disposable> disposables = new ArrayList<>();
    private boolean isShown = false;
    private boolean isDisposed = false;
    private @Nullable Runnable contentBuilder;
    private boolean contentBuilt = false;

    /**
     * Mirrors the wrapped dialog element's name for convenient reads. Use
     * {@link #name(String)} to rename so both stay in sync.
     */
    public String name = "solim-dialog-dialog";

    public SolimDialog() {
        this("");
    }

    public SolimDialog(String title) {
        wrapped = new BaseDialog(title != null ? title : "");
        wrapped.name = name;
        wrapped.setFillParent(true);
    }

    public SolimDialog fillParent(boolean fillParent) {
        wrapped.setFillParent(fillParent);
        return this;
    }

    public boolean isFillParent() {
        return wrapped.fillParent;
    }

    public static SolimDialog of(String title, Runnable content) {
        SolimDialog d = new SolimDialog(title);
        d.children(content);
        return d;
    }

    public SolimDialog children(@Nullable Runnable contentBuilder) {
        this.contentBuilder = contentBuilder;
        return this;
    }

    public void ensureContentBuilt() {
        if (!contentBuilt && contentBuilder != null) {
            contentBuilt = true;
            BaseComponent comp = new BaseComponent() {
                @Override
                protected Element build() {
                    AttachmentStack.push(wrapped.cont);
                    try {
                        contentBuilder.run();
                    } finally {
                        AttachmentStack.pop();
                    }
                    return wrapped.cont;
                }
            };
            registerDisposable(comp);
            comp.element();
        }
    }

    public SolimDialog actionButton(String text, Runnable action) {
        if (wrapped.buttons != null) {
            wrapped.buttons.button(text != null ? text : "", action).wrapLabel(false);
        }
        return this;
    }

    public SolimDialog actionButton(String text, Drawable icon, Runnable action) {
        if (wrapped.buttons != null) {
            if (icon != null) {
                wrapped.buttons.button(text != null ? text : "", icon, action).wrapLabel(false);
            } else {
                wrapped.buttons.button(text != null ? text : "", action).wrapLabel(false);
            }
        }
        return this;
    }

    public SolimDialog actionButton(String text, float width, float height, Runnable action) {
        return actionButton(text, null, width, height, action);
    }

    public SolimDialog actionButton(String text, @Nullable Drawable icon, float width, float height, Runnable action) {
        if (wrapped.buttons != null) {
            Cell<?> cell;
            if (icon != null) {
                cell = wrapped.buttons.button(text != null ? text : "", icon, action).wrapLabel(false);
            } else {
                cell = wrapped.buttons.button(text != null ? text : "", action).wrapLabel(false);
            }
            if (cell != null) {
                cell.size(width, height);
            }
        }
        return this;
    }

    /** Registers a disposable resource with this dialog's lifecycle. */
    public <T extends Disposable> T registerDisposable(T disposable) {
        if (disposable != null) {
            disposables.add(disposable);
        }
        return disposable;
    }

    /**
     * Registers an Arc event listener that automatically unregisters when this
     * dialog is disposed.
     */
    public <T> Disposable listen(Class<T> eventType, Cons<T> listener) {
        Events.on(eventType, listener);
        Disposable d = DisposableAction.of(() -> Events.remove(eventType, listener));
        disposables.add(d);
        return d;
    }

    /**
     * Creates a reactive signal initialized from the Prov that recalculates
     * whenever the callback registrar invokes the given callback. The returned
     * disposable is owned by this dialog.
     */
    public <T> Signal<T> createSignal(Func<Runnable, Disposable> registrar, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        if (registrar != null) {
            Disposable d = registrar.get(() -> signal.set(Prov.get()));
            registerDisposable(d);
        }
        return signal;
    }

    /**
     * Creates a reactive signal initialized from the Prov that recalculates
     * whenever the specified Arc event fires. The event listener is automatically
     * cleaned up when this dialog is disposed.
     */
    public <E, T> Signal<T> createSignal(Class<E> eventType, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        listen(eventType, e -> signal.set(Prov.get()));
        return signal;
    }

    /**
     * Creates a reactive signal that updates with mapped event data whenever the
     * specified Arc event fires. The event listener is automatically cleaned up
     * when this dialog is disposed.
     */
    public <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
        Signal<T> signal = Signal.of(initial);
        listen(eventType, e -> signal.set(mapper.get(e)));
        return signal;
    }

    /** Returns the wrapped Mindustry dialog, building deferred content first. */
    public BaseDialog dialog() {
        ensureContentBuilt();
        return wrapped;
    }

    /** Returns the wrapped dialog's content table. */
    public Table cont() {
        ensureContentBuilt();
        return wrapped.cont;
    }

    /** Returns the wrapped dialog's button row table. */
    public Table buttons() {
        return wrapped.buttons;
    }

    @Override
    public Element element() {
        return wrapped;
    }

    public SolimDialog name(String name) {
        this.name = name;
        wrapped.name = name;
        return this;
    }

    public SolimDialog addCloseButton() {
        wrapped.addCloseButton();
        return this;
    }

    public SolimDialog addCloseButton(float width) {
        wrapped.addCloseButton(width);
        return this;
    }

    public SolimDialog closeOnBack() {
        wrapped.closeOnBack();
        return this;
    }

    public SolimDialog closeOnBack(Runnable run) {
        wrapped.closeOnBack(run);
        return this;
    }

    public SolimDialog hidden(Runnable run) {
        wrapped.hidden(run);
        return this;
    }

    public SolimDialog shown(Runnable run) {
        wrapped.shown(run);
        return this;
    }

    public SolimDialog setWidth(float width) {
        wrapped.setWidth(width);
        return this;
    }

    public SolimDialog maxWidth(float maxWidth) {
        wrapped.cont.defaults().maxWidth(maxWidth);
        return this;
    }

    public SolimDialog show(Scene scene) {
        ensureContentBuilt();
        isShown = true;
        if (scene != null || Core.scene != null) {
            wrapped.show(scene != null ? scene : Core.scene);
        }
        return this;
    }

    public SolimDialog show() {
        return show(Core.scene);
    }

    public void hide() {
        isShown = false;
        if (Core.scene != null) {
            wrapped.hide();
        }
    }

    public boolean isShown() {
        if (Core.scene != null) {
            return wrapped.isShown();
        }
        return isShown;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    protected void onDispose() {
    }

    public SolimDialog rounded(int radius) {
        RoundedHelper.getOrCreateRounded(wrapped.cont, radius);
        return this;
    }

    public SolimDialog rounded(int radius, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(wrapped.cont, radius);
        if (color != null) {
            rd.fillColor(color);
        }
        return this;
    }

    public SolimDialog rounded(int radius, @Nullable Readable<Color> color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(wrapped.cont, radius);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null)
                    rd.fillColor(c);
            });
            registerDisposable(e);
        }
        return this;
    }

    public SolimDialog border(float stroke, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(wrapped.cont, 8);
        rd.border(stroke, color != null ? color : Color.white);
        return this;
    }

    public SolimDialog border(float stroke, @Nullable Readable<Color> color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(wrapped.cont, 8);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                rd.border(stroke, c != null ? c : Color.white);
            });
            registerDisposable(e);
        } else {
            rd.border(stroke, Color.white);
        }
        return this;
    }

    @Override
    public void dispose() {
        if (isDisposed) {
            return;
        }
        isDisposed = true;
        hide();
        onDispose();
        for (Disposable d : disposables) {
            try {
                d.dispose();
            } catch (Throwable t) {
                Log.err("Error disposing resource in SolimDialog", t);
            }
        }
        disposables.clear();
    }
}
