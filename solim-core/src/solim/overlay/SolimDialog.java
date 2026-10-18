package solim.overlay;

import arc.Core;
import solim.reactive.Readable;
import arc.func.Cons;
import arc.func.Func;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.ui.dialogs.BaseDialog;

import arc.func.Prov;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.TableCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class SolimDialog extends Component
        implements Configurable<SolimDialog>, TableCapability, ContainerCapability<SolimDialog> {

    private final BaseDialog wrapped;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimDialog> pending;
    private boolean isShown = false;
    private @Nullable Runnable contentBuilder;
    private boolean contentBuilt = false;

    public String name = "solim-dialog-dialog";

    public SolimDialog() {
        this("");
    }

    public SolimDialog(String title) {
        this(new BaseDialog(title != null ? title : ""));
    }

    private SolimDialog(BaseDialog wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
        wrapped.name = name;
        wrapped.setFillParent(true);
        SolimToken.bind(wrapped, this, constraints);
    }

    @Override
    public Table table() {
        return wrapped.cont;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<SolimDialog> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimDialog> props) {
        this.pending = props;
    }

    @Override
    public SolimDialog self() {
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
        applyPendingProps();
        this.contentBuilder = contentBuilder;
        return this;
    }

    public void ensureContentBuilt() {
        if (!contentBuilt && contentBuilder != null) {
            contentBuilt = true;
            OwnershipContext.push(this);
            try {
                Component comp = new Component() {
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
                comp.element();
            } finally {
                OwnershipContext.pop();
            }
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

    public <T extends Disposable> T registerDisposable(T disposable) {
        adopt(disposable);
        return disposable;
    }

    public <T> Disposable listen(Class<T> eventType, Cons<T> listener) {
        return super.listen(eventType, listener);
    }

    public <T> Signal<T> createSignal(Func<Runnable, Disposable> registrar, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        if (registrar != null) {
            Disposable registration = registrar.get(() -> signal.set(Prov.get()));
            adopt(registration);
        }
        return signal;
    }

    public <E, T> Signal<T> createSignal(Class<E> eventType, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        listen(eventType, e -> signal.set(Prov.get()));
        return signal;
    }

    public <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
        Signal<T> signal = Signal.of(initial);
        listen(eventType, e -> signal.set(mapper.get(e)));
        return signal;
    }

    public BaseDialog dialog() {
        ensureContentBuilt();
        return wrapped;
    }

    public Table cont() {
        ensureContentBuilt();
        return wrapped.cont;
    }

    public Table buttons() {
        return wrapped.buttons;
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

    public SolimDialog maxWidth(Readable<Float> maxWidth) {
        if (maxWidth != null) {
            Effect.of(() -> {
                Float w = maxWidth.get();
                if (w != null) {
                    wrapped.cont.defaults().maxWidth(w);
                }
            });
        }
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

    @Override
    protected void onDispose() {
        hide();
    }
}
