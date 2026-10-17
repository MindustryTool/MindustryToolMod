package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import solim.display.SolimImage;
import solim.display.Text;
import solim.graphics.Drawables;
import solim.input.Button;
import solim.layout.Column;
import solim.layout.ReactiveGrid;
import solim.layout.Row;
import solim.reactive.ForEach;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.CollectingOwner;
import solim.test.OwnershipEnv;

class ComponentOwnershipContractTest extends OwnershipEnv {


    private static class Lazy extends Component {
        int builds;

        @Override
        protected Element build() {
            builds++;
            return new Element();
        }
    }

    private static final class Eager extends Component {
        Eager() {
            super(new Element());
        }
    }

    @Test
    void eagerComponentNeverBuildsAndLazyBuildsOnce() {
        Eager eager = new Eager();
        assertSame(eager.element(), eager.element());

        Lazy lazy = new Lazy();
        assertSame(lazy.element(), lazy.element());
        assertEquals(1, lazy.builds);
    }

    @Test
    void aBuildThatProducesNoElementFailsLoudly() {
        Component broken = new Component() {
            @Override
            protected Element build() {
                return null;
            }
        };
        assertThrows(IllegalStateException.class, broken::element);
        assertTrue(broken.isDisposed(), "A failed build must not leave the component alive");
    }

    @Test
    void disposalOrderIsOnDisposeThenChildrenThenLeavesForBothStrategies() {
        for (Component subject : Arrays.asList(new Lazy(), new Eager())) {
            List<String> log = new ArrayList<>();
            Component owner = ownerOf(subject, log);
            owner.element();
            owner.dispose();

            assertEquals(Arrays.asList("owner-onDispose", "child-onDispose", "leaf-2", "leaf-1"), log,
                    "Disposal must run onDispose, then children, then own leaves in LIFO order");
        }
    }

    private static Component ownerOf(Component child, List<String> log) {
        return new Lazy() {
            @Override
            protected Element build() {
                adopt(DisposableAction.of(() -> log.add("leaf-1")));
                adopt(DisposableAction.of(() -> log.add("leaf-2")));
                adopt(child);
                child.adopt(DisposableAction.of(() -> log.add("child-onDispose")));
                return new Element();
            }

            @Override
            protected void onDispose() {
                log.add("owner-onDispose");
            }
        };
    }

    @Test
    void childrenBlockOwnsWhatItCreates() {
        Button button = new Button();
        Text[] created = new Text[1];

        button.children(() -> created[0] = Text.of("hello"));
        button.element();

        assertTrue(button.children().contains(created[0]),
                "A component created inside children() must be owned by the component that ran the block");

        button.dispose();
        assertTrue(created[0].isDisposed(), "The owned component must die with its owner");
    }

    @Test
    void aReactiveModifierIsOwnedWithNoRegistrationCallAtTheCallSite() {
        Signal<Boolean> shown = Signal.of(false);
        SolimImage image = new SolimImage();
        image.props(visible(shown));
        Element element = image.element();

        image.dispose();
        shown.set(true);
        SignalDispatcher.flush();

        assertFalse(element.visible, "A disposed component must stop reacting to its bindings");
    }

    @Test
    void aKeyedFactoryReceivesTheResourcesCreatedAroundTheComponent() {
        List<Button> buttons = new ArrayList<>();

        Column grid = new Column();
        grid.children(() -> {
            Button button = new Button();
            button.icon(Signal.of((Drawable) null));
            button.props(grow());
            buttons.add(button);
        });

        assertEquals(1, buttons.size());
        grid.dispose();
        assertTrue(buttons.get(0).isDisposed(),
                "A component mounted by a children() block must be disposed with the block's owner");
    }

    @Test
    void forEachKeepsEachComponentTypesReactiveBindingAlive() {
        Signal<Boolean> shown = Signal.of(true);
        List<Component> created = new ArrayList<>();

        ForEach<String> forEach = ForEach.of(Signal.of(Arrays.asList("a", "b")));
        forEach.items(item -> {
            Row row = new Row();
            Text text = Text.of(item);
            text.props(visible(shown));
            row.children(text::label);
            created.add(row);
            created.add(text);
            return row;
        });
        forEach.element();
        SignalDispatcher.flush();

        shown.set(false);
        SignalDispatcher.flush();

        assertEquals(4, created.size(), "The factory produced a row and a text per item");
        for (Component component : created) {
            assertFalse(component.isDisposed(),
                    "Reconciled components must stay alive while their key is present");
        }
        for (int i = 1; i < created.size(); i += 2) {
            Text text = (Text) created.get(i);
            assertFalse((((Label) text.label())).visible,
                    "A reconciled Text must keep reacting to its own bindings");
        }

        forEach.dispose();
        for (Component component : created) {
            assertTrue(component.isDisposed());
        }
    }

    @Test
    void pushingAnOwnerAllocatesNoCallback() {

        Lazy owner = new Lazy();
        OwnershipContext.push(owner);
        assertSame(owner, OwnershipContext.current(), "The pushed owner is stored as-is");
        OwnershipContext.pop();

        CollectingOwner collector = new CollectingOwner();
        OwnershipContext.push(collector);
        assertSame(collector, OwnershipContext.current());
        OwnershipContext.pop();
    }

    @Test
    void ancestorQueriesFollowTheRecordedTree() {
        Lazy[] child = new Lazy[1];
        Lazy[] grandchild = new Lazy[1];

        Component root = new Lazy() {
            @Override
            protected Element build() {

                adopt(child[0] = new Lazy());
                child[0].element();
                child[0].adopt(grandchild[0] = new Lazy());
                return new Element();
            }
        };
        root.element();

        assertSame(root, child[0].owner());
        assertSame(child[0], grandchild[0].owner());
        assertEquals(Arrays.asList(child[0]), root.children());
        assertTrue(child[0].isAncestorOf(grandchild[0]));
        assertFalse(grandchild[0].isAncestorOf(child[0]));
        assertFalse(child[0].isAncestorOf(root));

        root.dispose();
        assertEquals(null, child[0].owner(), "A disposed node detaches from its owner");
    }

    @Test
    void aReactiveGridFactoryReturningAButtonKeepsItsIconBinding() {

        Drawable first = new TextureRegionDrawable(new TextureRegion());
        Drawable second = new TextureRegionDrawable(new TextureRegion());
        Signal<Drawable> icon = Signal.of(first);
        List<Button> created = new ArrayList<>();

        ReactiveGrid<String> grid = ReactiveGrid.of(Signal.of(Arrays.asList("a")));
        grid.items(item -> {
            Button button = new Button();
            button.icon(icon);
            created.add(button);
            return button;
        });
        grid.element();
        SignalDispatcher.flush();

        Image image = iconOf(created.get(0));
        assertSame(Drawables.scalable(first), image.getDrawable());

        icon.set(second);
        SignalDispatcher.flush();

        assertSame(Drawables.scalable(second), image.getDrawable(),
                "A Button built by a keyed factory must keep reacting to its icon binding");
    }

    private static Image iconOf(Button button) {
        for (Element child : button.button().getChildren()) {
            if (child instanceof Image) {
                return (Image) child;
            }
            for (Element nested : ((TextButton) child).getChildren()) {
                if (nested instanceof Image) {
                    return (Image) nested;
                }
            }
        }
        throw new AssertionError("Button has no icon Image");
    }

    @Test
    void modifierEffectCreatedInParentBuildIsDisposedWithChild() {
        Signal<Float> widthSignal = Signal.of(100f);
        Text[] childHolder = new Text[1];

        Component parent = new Component() {
            @Override
            protected Element build() {
                Text child = new Text("hello");
                child.props(width(widthSignal), prefWidth(widthSignal));
                childHolder[0] = child;
                return child.element();
            }
        };

        parent.element();
        SignalDispatcher.flush();

        Text child = childHolder[0];
        assertNotNull(child);
        assertEquals(2, widthSignal.observerCount(),
                "Both the element-width binding and the cell-width binding must observe widthSignal");

        child.dispose();

        assertEquals(0, widthSignal.observerCount(),
                "Modifier effect must be disposed when the configured child is disposed, even if created during parent build()");

        assertFalse(parent.isDisposed(), "Parent should still be alive");
        parent.dispose();
    }
}
