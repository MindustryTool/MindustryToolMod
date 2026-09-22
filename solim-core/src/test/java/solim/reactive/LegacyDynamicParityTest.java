package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import arc.func.Func;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.layout.Column;
import solim.layout.GapContainer;
import solim.layout.Row;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimCoreEnv;

/**
 * Parity between the pre-migration {@code dynamic(source, Func<T, Component>)}
 * (single-root factory, {@code null} collapses) and the migrated void-factory
 * {@link Dynamic} / {@link When}.
 *
 * <p>Every {@code dynamic(...)} call site that existed in {@code mod/} before
 * the migration is mapped to exactly one test below (see the covered-sites
 * note on each test). Each test drives the old shape and the migrated shape
 * through the same signal values and requires identical results in every way:
 * container visibility, container child count, recursive child structure and
 * the laid-out parent size/cell state (i.e. collapsed branches take no space
 * on both sides).</p>
 */
class LegacyDynamicParityTest extends SolimCoreEnv {

    // ------------------------------------------------------------------
    // LegacyDynamic: verbatim port of the pre-migration Dynamic
    // (Func<T, Component> factory, ParentStack.isolate, single cell).
    // ------------------------------------------------------------------

    static final class LegacyDynamic<T> extends BaseComponent
            implements CellConfig<LegacyDynamic<T>>, TableConfig<LegacyDynamic<T>>, ElementConfig<LegacyDynamic<T>> {
        private static final Object SENTINEL = new Object();
        private final Table container = new Table();
        private final PendingCellConfig constraints = new PendingCellConfig();
        private final Readable<T> source;
        private final Func<T, Component> factory;
        private Component currentComponent;
        @SuppressWarnings("unchecked")
        private T lastValue = (T) SENTINEL;
        private final List<Disposable> currentBindings = new ArrayList<>();

        LegacyDynamic(Readable<T> source, Func<T, Component> factory) {
            this.source = Objects.requireNonNull(source, "source must not be null");
            this.factory = Objects.requireNonNull(factory, "factory must not be null");
            SolimToken.bind(this.container, this, constraints);
            this.container.top().left();
            this.container.defaults().top().left();
        }

        Table container() {
            return container;
        }

        @Override
        public Table table() {
            return container;
        }

        @Override
        public LegacyDynamic<T> name(@Nullable String name) {
            super.name(name);
            return this;
        }

        @Override
        public PendingCellConfig cellConfig() {
            return constraints;
        }

        @Override
        protected Element build() {
            applyContainerAlign();
            Effect.of(() -> {
                T value = source.get();
                if (Objects.equals(value, lastValue)) {
                    return;
                }
                lastValue = value;
                if (currentComponent != null) {
                    currentComponent.dispose();
                    currentComponent = null;
                }
                for (Disposable d : currentBindings) {
                    d.dispose();
                }
                currentBindings.clear();
                container.clearChildren();
                currentComponent = ReactiveContext.untracked(() -> ParentStack.isolate(() -> {
                    Component c = factory.get(value);
                    if (c != null) {
                        c.element();
                    }
                    return c;
                }));
                if (currentComponent != null) {
                    Element el = currentComponent.element();
                    Cell<?> cell = container.add(el);
                    cell.minWidth(0f);
                    PendingCellConfig sc = PendingCellConfig.find(currentComponent);
                    if (sc == null) {
                        sc = PendingCellConfig.find(el);
                    }
                    if (sc != null) {
                        currentBindings.addAll(sc.applyToCell(cell));
                    } else if (SolimToken.isExpandingChild(el)) {
                        cell.growX();
                    }
                }
                applyContainerAlign();
                updateParentCell();
                container.invalidateHierarchy();
            });
            return container;
        }

        private void applyContainerAlign() {
            if (constraints.align != null) {
                container.align(constraints.align);
            } else {
                container.top();
            }
        }

        private void updateParentCell() {
            Cell<?> parentCell = container.parent instanceof Table ? ((Table) container.parent).getCell(container)
                    : null;
            if (currentComponent != null) {
                container.visible = true;
                if (parentCell != null) {
                    parentCell.minWidth(Float.NEGATIVE_INFINITY).minHeight(Float.NEGATIVE_INFINITY);
                    parentCell.maxWidth(Float.NEGATIVE_INFINITY).maxHeight(Float.NEGATIVE_INFINITY);
                    constraints.applyToCell(parentCell);
                }
            } else {
                container.visible = false;
                if (parentCell != null) {
                    parentCell.size(0f).pad(0f);
                }
            }
            if (container.parent instanceof Table) {
                GapContainer.respace((Table) container.parent);
            }
        }

        @Override
        protected void onDispose() {
            if (currentComponent != null) {
                currentComponent.dispose();
                currentComponent = null;
            }
            for (Disposable d : currentBindings) {
                d.dispose();
            }
            currentBindings.clear();
            container.clearChildren();
        }

        @Override
        public LegacyDynamic<T> self() {
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Harness: stubs, structure signatures and parity assertions.
    // ------------------------------------------------------------------

    static final class Leaf extends BaseComponent {
        Leaf(String name) {
            name(name);
        }

        @Override
        protected Element build() {
            return new Element();
        }
    }

    static Row sizedRow(float height) {
        Row row = new Row();
        row.cellConfig().prefHeight = Readable.of(height);
        return row;
    }

    static Column sizedColumn(float height) {
        Column column = new Column();
        column.cellConfig().prefHeight = Readable.of(height);
        return column;
    }

    static String structure(Element element) {
        if (element instanceof Table) {
            StringBuilder sb = new StringBuilder("Table[");
            Seq<Element> children = ((Table) element).getChildren();
            for (int i = 0; i < children.size; i++) {
                sb.append(structure(children.get(i)));
            }
            return sb.append("]").toString();
        }
        return element.getClass().getSimpleName() + ":" + element.name + ";";
    }

    /**
     * Rendered-structure signature. Single-child {@link Table} wrappers are
     * transparent: a migrated branch legitimately adds one container level per
     * nested {@code when()} (old nesting mounted content one level shallower),
     * and such wrappers measure exactly to their child. Tables with zero or
     * several children stay explicit, so stolen/duplicated roots and
     * fall-through siblings still fail.
     */
    static String boxSignature(Table box) {
        return "Box[" + box.visible + "](" + innerSignature(box) + ")";
    }

    static String innerSignature(Table table) {
        StringBuilder sb = new StringBuilder();
        Seq<Element> children = table.getChildren();
        for (int i = 0; i < children.size; i++) {
            Element child = children.get(i);
            if (child instanceof Table && ((Table) child).getChildren().size == 1) {
                sb.append(innerSignature((Table) child));
            } else {
                sb.append(structure(child));
            }
        }
        return sb.toString();
    }

    static final class Side {
        final Column parent;
        final Table parentTable;

        Side() {
            parent = new Column().gap(8f);
            parentTable = parent.table();
        }

        void layout() {
            parentTable.pack();
            parentTable.layout();
        }

        void dispose() {
            parent.dispose();
        }
    }

    static void assertParity(String step, Table oldBox, Table oldParent, Table newBox, Table newParent) {
        oldParent.pack();
        oldParent.layout();
        newParent.pack();
        newParent.layout();
        assertEquals(oldBox.visible, newBox.visible, step + ": visible");
        assertEquals(oldBox.getChildren().size, newBox.getChildren().size, step + ": childCount");
        assertEquals(boxSignature(oldBox), boxSignature(newBox), step + ": structure");
        assertEquals(oldParent.getHeight(), newParent.getHeight(), 0.5f, step + ": parentHeight");
        assertEquals(oldParent.getWidth(), newParent.getWidth(), 0.5f, step + ": parentWidth");
        Cell<?> oldCell = oldParent.getCell(oldBox);
        Cell<?> newCell = newParent.getCell(newBox);
        if (oldCell != null && newCell != null) {
            assertEquals(CellAccess.padTop(oldCell), CellAccess.padTop(newCell), 0.01f, step + ": padTop");
            assertEquals(CellAccess.padBottom(oldCell), CellAccess.padBottom(newCell), 0.01f,
                    step + ": padBottom");
            assertEquals(CellAccess.minHeight(oldCell), CellAccess.minHeight(newCell), 0.01f,
                    step + ": minHeight");
        }
    }

    static void finish(Table oldBox, Side oldSide, Table newBox, Side newSide) {
        Disposable oldRoot = (Disposable) SolimToken.getComponent(oldBox);
        Disposable newRoot = (Disposable) SolimToken.getComponent(newBox);
        if (oldRoot != null) {
            oldRoot.dispose();
        }
        if (newRoot != null) {
            newRoot.dispose();
        }
        oldSide.dispose();
        newSide.dispose();
    }

    // ------------------------------------------------------------------
    // S1: boolean-exclusive container roots (then/else).
    // ------------------------------------------------------------------

    /**
     * Covers AttachContentDialog (uploading row vs form column).
     */
    @Test
    @SuppressWarnings("unchecked")
    void attachContent_uploadingVsForm() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();
        List<Disposable> owned = new ArrayList<>();

        oldSide.parent.children(() -> {
            owned.add(new LegacyDynamic<>(oldSignal, uploading -> {
                if (Boolean.TRUE.equals(uploading)) {
                    return sizedRow(40f).growX();
                }
                return sizedColumn(120f).growX();
            }).grow());
        });
        newSide.parent.children(() -> {
            owned.add(When.of(newSignal)
                    .thenDo(() -> sizedRow(40f).growX())
                    .elseDo(() -> sizedColumn(120f).growX())
                    .grow());
        });
        LegacyDynamic<Boolean> oldDyn = (LegacyDynamic<Boolean>) owned.get(0);
        When newWhen = (When) owned.get(1);
        SignalDispatcher.flush();

        assertParity("form", oldDyn.container(), oldSide.parentTable, newWhen.container(), newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("uploading", oldDyn.container(), oldSide.parentTable, newWhen.container(), newSide.parentTable);

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("back-to-form", oldDyn.container(), oldSide.parentTable, newWhen.container(),
                newSide.parentTable);
        finish(oldDyn.container(), oldSide, newWhen.container(), newSide);

        oldDyn.dispose();
        newWhen.dispose();
        oldSide.dispose();
        newSide.dispose();
    }

    /**
     * Covers ChatInputView (login row vs composer column containing a nested
     * reply-target dynamic plus a static row).
     */
    @Test
    void chatInput_loginVsComposerWithNestedReply() {
        Signal<Boolean> oldLogged = Signal.of(false);
        Signal<Boolean> newLogged = Signal.of(false);
        Signal<String> oldReply = Signal.of(null);
        Signal<String> newReply = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldLogged, notLoggedIn -> {
                if (Boolean.TRUE.equals(notLoggedIn)) {
                    return sizedRow(32f).growX();
                }
                return sizedColumn(80f).growX().children(() -> {
                    new LegacyDynamic<>(oldReply, target -> target != null ? sizedRow(24f).growX() : null)
                            .growX();
                    sizedRow(48f).growX().children(() -> new Leaf("composer"));
                });
            }).grow();
        });
        newSide.parent.children(() -> {
            When.of(newLogged)
                    .thenDo(() -> sizedRow(32f).growX())
                    .elseDo(() -> sizedColumn(80f).growX().children(() -> {
                        Dynamic.of(newReply, target -> {
                            if (target != null) {
                                sizedRow(24f).growX();
                            }
                        }).growX();
                        sizedRow(48f).growX().children(() -> new Leaf("composer"));
                    }))
                    .grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("logged-out", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldLogged.set(true);
        newLogged.set(true);
        SignalDispatcher.flush();
        assertParity("logged-in-no-reply", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldReply.set("msg-1");
        newReply.set("msg-1");
        SignalDispatcher.flush();
        assertParity("logged-in-reply", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldReply.set(null);
        newReply.set(null);
        SignalDispatcher.flush();
        assertParity("reply-cleared", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldLogged.set(false);
        newLogged.set(false);
        SignalDispatcher.flush();
        assertParity("logged-out-again", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers MapDetailDialog and SchematicDetailDialog (portrait vs landscape
     * method-reference branches) plus HostRoomDialog step1/step2 and
     * QuickSchematicGridHudView page-position column/row.
     */
    @Test
    void detailDialogs_portraitVsLandscape() {
        Signal<Boolean> oldSignal = Signal.of(true);
        Signal<Boolean> newSignal = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, portrait -> {
                if (Boolean.TRUE.equals(portrait)) {
                    return sizedColumn(200f);
                }
                return sizedRow(120f);
            }).grow();
        });
        newSide.parent.children(() -> {
            When.of(newSignal)
                    .thenDo(() -> sizedColumn(200f))
                    .elseDo(() -> sizedRow(120f))
                    .grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("portrait", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("landscape", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("portrait-again", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers TeamResourceHudView expanded content vs empty-row placeholder
     * (also the showItems/showUnits/showPower/showStoredPower outer shape).
     */
    @Test
    void teamResources_expandedVsPlaceholderRow() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, expanded -> Boolean.TRUE.equals(expanded) ? sizedColumn(150f) : new Row())
                    .growX();
        });
        newSide.parent.children(() -> {
            When.of(newSignal)
                    .thenDo(() -> sizedColumn(150f))
                    .elseDo(Row::new)
                    .growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("expanded", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers RoomBrowserView (collapsed row vs list column with a nested
     * grouped-rooms dynamic) and AuthLoginDialog (url button vs loader).
     */
    @Test
    void roomBrowser_collapsedVsListWithNestedGroups() {
        Signal<Boolean> oldCollapsed = Signal.of(true);
        Signal<Boolean> newCollapsed = Signal.of(true);
        Signal<Boolean> oldEmpty = Signal.of(true);
        Signal<Boolean> newEmpty = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldCollapsed, collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return new Row();
                }
                return sizedColumn(100f).growX().children(() -> {
                    new LegacyDynamic<>(oldEmpty, empty -> {
                        if (Boolean.TRUE.equals(empty)) {
                            return sizedColumn(30f).growX();
                        }
                        return sizedColumn(80f).growX();
                    }).growX();
                });
            }).growX();
        });
        newSide.parent.children(() -> {
            When.of(newCollapsed)
                    .thenDo(Row::new)
                    .elseDo(() -> sizedColumn(100f).growX().children(() -> {
                        Dynamic.of(newEmpty, empty -> {
                            if (Boolean.TRUE.equals(empty)) {
                                sizedColumn(30f).growX();
                            } else {
                                sizedColumn(80f).growX();
                            }
                        }).growX();
                    }))
                    .growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldCollapsed.set(false);
        newCollapsed.set(false);
        SignalDispatcher.flush();
        assertParity("list-empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldEmpty.set(false);
        newEmpty.set(false);
        SignalDispatcher.flush();
        assertParity("list-filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers QuickSchematicGridSlotDialog resolved schematic (warning row vs
     * image row) with outer grow.
     */
    @Test
    void schematicSlot_nullVsContentRow() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, schematic -> {
                if (schematic == null) {
                    return sizedRow(48f).grow();
                }
                return sizedRow(96f).grow();
            }).grow();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, schematic -> {
                if (schematic == null) {
                    sizedRow(48f).grow();
                } else {
                    sizedRow(96f).grow();
                }
            }).grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("unresolved", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("abc");
        newSignal.set("abc");
        SignalDispatcher.flush();
        assertParity("resolved", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers ChatOverlayHudView collapsed badge vs expanded window.
     */
    @Test
    void chatOverlay_collapsedBadgeVsWindow() {
        Signal<Boolean> oldSignal = Signal.of(true);
        Signal<Boolean> newSignal = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return sizedRow(28f);
                }
                return sizedColumn(220f);
            });
        });
        newSide.parent.children(() -> {
            When.of(newSignal)
                    .thenDo(() -> sizedRow(28f))
                    .elseDo(() -> sizedColumn(220f));
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("expanded", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers SchematicPickerDialog preview ready vs loading card (both
     * branches render, with different children).
     */
    @Test
    void schematicPicker_previewCardVsLoadingCard() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, ready -> {
                if (Boolean.TRUE.equals(ready)) {
                    return sizedColumn(160f).growX().children(() -> new Leaf("preview"));
                }
                return sizedColumn(160f).growX().children(() -> new Leaf("loading"));
            });
        });
        newSide.parent.children(() -> {
            When.of(newSignal)
                    .thenDo(() -> sizedColumn(160f).growX().children(() -> new Leaf("preview")))
                    .elseDo(() -> sizedColumn(160f).growX().children(() -> new Leaf("loading")));
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("loading", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("ready", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S2: boolean then-only (false collapses).
    // ------------------------------------------------------------------

    /**
     * Covers GodModeHudView, TimeControlHudView, QuickAccessHudView and
     * QuickSchematicGridHudView hide-drag-handle buttons plus
     * JoystickHudView show-handle button.
     */
    @Test
    void dragHandles_thenOnlyCollapses() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, hide -> {
                if (!Boolean.TRUE.equals(hide)) {
                    return sizedRow(44f);
                }
                return null;
            });
        });
        newSide.parent.children(() -> {
            When.of(newSignal).elseDo(() -> sizedRow(44f));
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("shown", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("hidden-collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertTrue(oldSide.parentTable.getHeight() < 0.5f, "collapsed old must take no space");
        assertTrue(newSide.parentTable.getHeight() < 0.5f, "collapsed new must take no space");

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("shown-again", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers MusicSettingsView playing progress, HostRoomDialog showAddress
     * text, BrowserSearchHeader selected tags row and SchematicPickerDialog
     * remaining-count button plus QuickSchematicGridSettingsView clear-icon
     * button.
     */
    @Test
    void optionalContent_thenOnlyCollapsesWithOuterGrow() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, playing -> {
                if (Boolean.TRUE.equals(playing)) {
                    return sizedRow(16f).growX();
                }
                return null;
            }).growX();
        });
        newSide.parent.children(() -> {
            When.of(newSignal).thenDo(() -> sizedRow(16f).growX()).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("absent", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("present", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S3: boolean else-only (true collapses).
    // ------------------------------------------------------------------

    /**
     * Covers ChatOverlayHudView channel/user side panels.
     */
    @Test
    void chatOverlay_sidePanelsElseOnlyCollapse() {
        Signal<Boolean> oldSignal = Signal.of(false);
        Signal<Boolean> newSignal = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return null;
                }
                return sizedRow(320f).growX();
            }).growX();
        });
        newSide.parent.children(() -> {
            When.of(newSignal).elseDo(() -> sizedRow(320f).growX()).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("open", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(true);
        newSignal.set(true);
        SignalDispatcher.flush();
        assertParity("collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S4: nullable value source (null collapses / method-ref always root).
    // ------------------------------------------------------------------

    /**
     * Covers ChatMessageListView translated text and translating indicator
     * (string source, empty collapses).
     */
    @Test
    void chatMessage_translatedTextCollapsesWhenEmpty() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, translated -> {
                if (translated == null || translated.isEmpty()) {
                    return null;
                }
                return sizedColumn(36f).growX();
            });
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, translated -> {
                if (translated != null && !translated.isEmpty()) {
                    sizedColumn(36f).growX();
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("absent", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("hola");
        newSignal.set("hola");
        SignalDispatcher.flush();
        assertParity("present", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("");
        newSignal.set("");
        SignalDispatcher.flush();
        assertParity("empty-collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers JoinApprovalHudView request banner (method reference, always a
     * single row root) and PrettyChatSettingsView card list (always a
     * column root).
     */
    @Test
    void methodRefFactories_alwaysSingleRoot() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, request -> {
                if (request == null) {
                    return new Row();
                }
                return sizedRow(40f);
            });
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, request -> {
                if (request == null) {
                    new Row();
                } else {
                    sizedRow(40f);
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("null-request", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);

        oldSignal.set("req-1");
        newSignal.set("req-1");
        SignalDispatcher.flush();
        assertParity("request", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers TranslationSettingsView provider api-key rows (per-value column
     * branches, unknown provider collapses) and TimeControlHudView mode
     * content (always a root, branch per mode).
     */
    @Test
    void valueBranches_perValueRoots() {
        Signal<String> oldSignal = Signal.of("other");
        Signal<String> newSignal = Signal.of("other");
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, provider -> {
                if ("a".equals(provider)) {
                    return sizedColumn(60f).growX();
                }
                if ("b".equals(provider)) {
                    return sizedColumn(80f).growX();
                }
                return null;
            }).growX();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, provider -> {
                if ("a".equals(provider)) {
                    sizedColumn(60f).growX();
                } else if ("b".equals(provider)) {
                    sizedColumn(80f).growX();
                }
            }).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("unknown-collapsed", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("a");
        newSignal.set("a");
        SignalDispatcher.flush();
        assertParity("provider-a", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("b");
        newSignal.set("b");
        SignalDispatcher.flush();
        assertParity("provider-b", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S5: value-based multi-way branches (never null).
    // ------------------------------------------------------------------

    /**
     * Covers ChatOverlayHudView desktop vs mobile body (width threshold).
     */
    @Test
    void chatOverlay_widthThresholdBranches() {
        Signal<Float> oldSignal = Signal.of(800f);
        Signal<Float> newSignal = Signal.of(800f);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, width -> {
                if (width < 1200f) {
                    return sizedColumn(300f);
                }
                return sizedRow(300f);
            });
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, width -> {
                if (width < 1200f) {
                    sizedColumn(300f);
                } else {
                    sizedRow(300f);
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("mobile", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(1600f);
        newSignal.set(1600f);
        SignalDispatcher.flush();
        assertParity("desktop", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers ChatMessageListView invite card (room vs fallback, named root
     * with height and growX).
     */
    @Test
    void chatMessage_roomCardVsFallback() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, room -> {
                if (room != null) {
                    return sizedColumn(120f).growX();
                }
                return sizedColumn(60f).growX();
            }).growX();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, room -> {
                if (room != null) {
                    sizedColumn(120f).growX();
                } else {
                    sizedColumn(60f).growX();
                }
            }).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("fallback", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("room-1");
        newSignal.set("room-1");
        SignalDispatcher.flush();
        assertParity("room", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers ChatMessageListView player-connect buttons (both branches
     * render sized buttons).
     */
    @Test
    void chatMessage_playerConnectButtons() {
        Signal<Boolean> oldSignal = Signal.of(true);
        Signal<Boolean> newSignal = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, enabled -> {
                if (Boolean.TRUE.equals(enabled)) {
                    return sizedRow(44f).growX();
                }
                return sizedRow(44f).growX().children(() -> new Leaf("enable-pc"));
            }).growX();
        });
        newSide.parent.children(() -> {
            When.of(newSignal)
                    .thenDo(() -> sizedRow(44f).growX())
                    .elseDo(() -> sizedRow(44f).growX().children(() -> new Leaf("enable-pc")))
                    .growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("enabled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("disabled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers GodModeHudView provider tools vs unavailable content.
     */
    @Test
    void godMode_providerActiveVsUnavailable() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, provider -> provider != null ? sizedRow(200f) : sizedRow(60f));
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, provider -> {
                if (provider != null) {
                    sizedRow(200f);
                } else {
                    sizedRow(60f);
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("unavailable", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("p");
        newSignal.set("p");
        SignalDispatcher.flush();
        assertParity("active", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers AuthOverlay session states. Regression test: the migration
     * dropped early returns here and stacked loading/error/login/card rows
     * as siblings. Exactly one branch must mount per state on both sides.
     */
    @Test
    void authOverlay_sessionStatesExclusive() {
        Signal<String> oldSignal = Signal.of("loading");
        Signal<String> newSignal = Signal.of("loading");
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, state -> {
                if ("loading".equals(state)) {
                    return sizedRow(24f);
                }
                if ("error".equals(state)) {
                    return sizedRow(48f);
                }
                if ("logged-out".equals(state)) {
                    return sizedRow(36f);
                }
                return sizedColumn(72f);
            });
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, state -> {
                if ("loading".equals(state)) {
                    sizedRow(24f);
                } else if ("error".equals(state)) {
                    sizedRow(48f);
                } else if ("logged-out".equals(state)) {
                    sizedRow(36f);
                } else {
                    sizedColumn(72f);
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        String[] states = {"loading", "error", "logged-out", "user"};
        for (String state : states) {
            oldSignal.set(state);
            newSignal.set(state);
            SignalDispatcher.flush();
            assertParity(state, oldBox, oldSide.parentTable, newBox, newSide.parentTable);
            assertEquals(1, oldBox.getChildren().size, state + ": old mounts exactly one root");
            assertEquals(1, newBox.getChildren().size, state + ": new mounts exactly one root");
        }
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S6: collection-driven single roots (loops, null-safe, early return).
    // ------------------------------------------------------------------

    /**
     * Covers EmojiDialog empty row vs chip wrap and both
     * SchematicIconPickerDialog glyph/content lists.
     */
    @Test
    void emoji_emptyRowVsChipWrap() {
        Signal<List<String>> oldSignal = Signal.of(new ArrayList<String>());
        Signal<List<String>> newSignal = Signal.of(new ArrayList<String>());
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, entries -> {
                if (entries == null || entries.isEmpty()) {
                    return sizedRow(32f).growX();
                }
                return sizedColumn(96f).children(() -> {
                    for (String entry : entries) {
                        new Leaf(entry);
                    }
                });
            }).growX();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, entries -> {
                if (entries == null || entries.isEmpty()) {
                    sizedRow(32f).growX();
                } else {
                    sizedColumn(96f).children(() -> {
                        for (String entry : entries) {
                            new Leaf(entry);
                        }
                    });
                }
            }).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        List<String> filled = new ArrayList<>();
        filled.add("a");
        filled.add("b");
        filled.add("c");
        oldSignal.set(filled);
        newSignal.set(filled);
        SignalDispatcher.flush();
        assertParity("filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers GodModeEffectsDialog/GodModeItemsDialog/GodModeUnitsDialog wraps,
     * QuickSchematicGridSettingsView pages wrap, TeamResourceAllTeamsDialog
     * grid and TeamResourceHudView team chips row (null-safe loops that
     * always mount exactly one root).
     */
    @Test
    void collectionWraps_alwaysSingleRoot() {
        Signal<List<String>> oldSignal = Signal.of(null);
        Signal<List<String>> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, items -> sizedColumn(64f).children(() -> {
                if (items != null) {
                    for (String item : items) {
                        new Leaf(item);
                    }
                }
            }));
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, items -> sizedColumn(64f).children(() -> {
                if (items != null) {
                    for (String item : items) {
                        new Leaf(item);
                    }
                }
            }));
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("null", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);

        List<String> filled = new ArrayList<>();
        filled.add("a");
        filled.add("b");
        oldSignal.set(filled);
        newSignal.set(filled);
        SignalDispatcher.flush();
        assertParity("filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers BrowserFilterDialog block filter (empty text vs chip wrap) and
     * QuickSchematicGridSlotDialog icon text variants.
     */
    @Test
    void filterBlocks_emptyTextVsWrap() {
        Signal<Boolean> oldSignal = Signal.of(true);
        Signal<Boolean> newSignal = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, empty -> {
                if (Boolean.TRUE.equals(empty)) {
                    return new Leaf("empty-hint");
                }
                return sizedColumn(80f).children(() -> {
                    new Leaf("chip-1");
                    new Leaf("chip-2");
                });
            });
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, empty -> {
                if (Boolean.TRUE.equals(empty)) {
                    new Leaf("empty-hint");
                } else {
                    sizedColumn(80f).children(() -> {
                        new Leaf("chip-1");
                        new Leaf("chip-2");
                    });
                }
            });
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set(false);
        newSignal.set(false);
        SignalDispatcher.flush();
        assertParity("filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers BrowserFilterDialog tag categories and WavePreviewPanelView wave
     * sections (single column root with an early return on empty input).
     */
    @Test
    void earlyReturnColumn_emptyVsLoop() {
        Signal<List<String>> oldSignal = Signal.of(new ArrayList<String>());
        Signal<List<String>> newSignal = Signal.of(new ArrayList<String>());
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, items -> sizedColumn(72f).growX().children(() -> {
                if (items == null || items.isEmpty()) {
                    new Leaf("empty-hint");
                    return;
                }
                for (String item : items) {
                    new Leaf(item);
                }
            }));
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, items -> sizedColumn(72f).growX().children(() -> {
                if (items == null || items.isEmpty()) {
                    new Leaf("empty-hint");
                    return;
                }
                for (String item : items) {
                    new Leaf(item);
                }
            }));
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);

        List<String> filled = new ArrayList<>();
        filled.add("w1");
        filled.add("w2");
        oldSignal.set(filled);
        newSignal.set(filled);
        SignalDispatcher.flush();
        assertParity("filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);
        finish(oldBox, oldSide, newBox, newSide);
    }

    // ------------------------------------------------------------------
    // S7/S8: nested dynamics.
    // ------------------------------------------------------------------

    /**
     * Covers ChatMessageListView message area: hasMessages list vs the nested
     * initial-loading / error / empty chain.
     */
    @Test
    void chatList_nestedLoadingErrorEmpty() {
        Signal<String> oldMode = Signal.of("list");
        Signal<String> newMode = Signal.of("list");
        Signal<String> oldInner = Signal.of("empty");
        Signal<String> newInner = Signal.of("empty");
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldMode, mode -> {
                if ("list".equals(mode)) {
                    return sizedColumn(400f).grow();
                }
                return new LegacyDynamic<>(oldInner, inner -> {
                    if ("loading".equals(inner)) {
                        return sizedRow(48f);
                    }
                    if ("error".equals(inner)) {
                        return sizedColumn(120f);
                    }
                    return sizedColumn(40f);
                }).grow();
            }).grow();
        });
        newSide.parent.children(() -> {
            When.of(newMode.map(m -> "list".equals(m)))
                    .thenDo(() -> sizedColumn(400f).grow())
                    .elseDo(() -> {
                        When.of(newInner.map(i -> "loading".equals(i)))
                                .thenDo(() -> sizedRow(48f))
                                .elseDo(() -> {
                                    Dynamic.of(newInner, inner -> {
                                        if ("error".equals(inner)) {
                                            sizedColumn(120f);
                                        } else {
                                            sizedColumn(40f);
                                        }
                                    });
                                })
                                .grow();
                    })
                    .grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("list", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        for (String inner : new String[]{"loading", "error", "empty"}) {
            oldMode.set("other");
            newMode.set("other");
            oldInner.set(inner);
            newInner.set(inner);
            SignalDispatcher.flush();
            assertParity("nested-" + inner, oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        }

        oldMode.set("list");
        newMode.set("list");
        SignalDispatcher.flush();
        assertParity("list-again", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers ChatUserListView no-channel branch: loading vs nested
     * error/empty chain (post-fix exclusive shape).
     */
    @Test
    void chatUsers_loadingVsErrorVsEmpty() {
        Signal<Boolean> oldLoading = Signal.of(true);
        Signal<Boolean> newLoading = Signal.of(true);
        Signal<String> oldError = Signal.of(null);
        Signal<String> newError = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldLoading, loading -> {
                if (Boolean.TRUE.equals(loading)) {
                    return sizedRow(48f);
                }
                return new LegacyDynamic<>(oldError, err -> {
                    if (err != null && !err.trim().isEmpty()) {
                        return sizedColumn(120f).grow();
                    }
                    return sizedColumn(40f);
                }).grow();
            }).grow();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newLoading, loading -> {
                if (Boolean.TRUE.equals(loading)) {
                    sizedRow(48f);
                } else {
                    Dynamic.of(newError, err -> {
                        if (err != null && !err.trim().isEmpty()) {
                            sizedColumn(120f).grow();
                        } else {
                            sizedColumn(40f);
                        }
                    }).grow();
                }
            }).grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("loading", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);

        oldLoading.set(false);
        newLoading.set(false);
        SignalDispatcher.flush();
        assertParity("empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);

        oldError.set("boom");
        newError.set("boom");
        SignalDispatcher.flush();
        assertParity("error", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        assertEquals(1, oldBox.getChildren().size);
        assertEquals(1, newBox.getChildren().size);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers TeamResourceHudView item/unit sections: outer show toggle with
     * an inner used-items dynamic (empty row vs card column).
     */
    @Test
    void teamResources_sectionWithInnerItems() {
        Signal<Boolean> oldShow = Signal.of(true);
        Signal<Boolean> newShow = Signal.of(true);
        Signal<Boolean> oldEmpty = Signal.of(true);
        Signal<Boolean> newEmpty = Signal.of(true);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldShow, show -> {
                if (!Boolean.TRUE.equals(show)) {
                    return new Row();
                }
                return sizedColumn(90f).growX().children(() -> {
                    new LegacyDynamic<>(oldEmpty, empty -> {
                        if (Boolean.TRUE.equals(empty)) {
                            return sizedRow(24f);
                        }
                        return sizedColumn(64f);
                    }).growX();
                });
            }).growX();
        });
        newSide.parent.children(() -> {
            When.of(newShow)
                    .thenDo(() -> sizedColumn(90f).growX().children(() -> {
                        Dynamic.of(newEmpty, empty -> {
                            if (Boolean.TRUE.equals(empty)) {
                                sizedRow(24f);
                            } else {
                                sizedColumn(64f);
                            }
                        }).growX();
                    }))
                    .elseDo(Row::new)
                    .growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("shown-empty", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldEmpty.set(false);
        newEmpty.set(false);
        SignalDispatcher.flush();
        assertParity("shown-filled", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldShow.set(false);
        newShow.set(false);
        SignalDispatcher.flush();
        assertParity("hidden", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers ChatMessageListView channel switch and ChatUserListView channel
     * switch (query/content column vs no-channel column) plus the three
     * then-only banners (refresh-failed, end-of-history, loading-older).
     */
    @Test
    void channelSwitch_andThenOnlyBanners() {
        Signal<Boolean> oldChannel = Signal.of(false);
        Signal<Boolean> newChannel = Signal.of(false);
        Signal<Boolean> oldBanner = Signal.of(false);
        Signal<Boolean> newBanner = Signal.of(false);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldChannel, hasChannel -> {
                if (!Boolean.TRUE.equals(hasChannel)) {
                    return sizedColumn(100f).grow();
                }
                return sizedColumn(300f).grow().children(() -> {
                    new LegacyDynamic<>(oldBanner, show -> {
                        if (Boolean.TRUE.equals(show)) {
                            return sizedRow(28f).growX();
                        }
                        return null;
                    });
                    sizedRow(200f).growX().children(() -> new Leaf("messages"));
                });
            }).grow();
        });
        newSide.parent.children(() -> {
            When.of(newChannel)
                    .elseDo(() -> sizedColumn(100f).grow())
                    .thenDo(() -> sizedColumn(300f).grow().children(() -> {
                        When.of(newBanner).thenDo(() -> sizedRow(28f).growX());
                        sizedRow(200f).growX().children(() -> new Leaf("messages"));
                    }))
                    .grow();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("no-channel", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldChannel.set(true);
        newChannel.set(true);
        SignalDispatcher.flush();
        assertParity("channel-no-banner", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldBanner.set(true);
        newBanner.set(true);
        SignalDispatcher.flush();
        assertParity("channel-banner", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        // Banner collapses take no space on either side.
        oldBanner.set(false);
        newBanner.set(false);
        SignalDispatcher.flush();
        assertParity("banner-cleared", oldBox, oldSide.parentTable, newBox, newSide.parentTable);
        finish(oldBox, oldSide, newBox, newSide);
    }

    /**
     * Covers QuickSchematicGridSettingsView page-icon text variants and
     * QuickSchematicGridSlotDialog icon text variants.
     */
    @Test
    void iconText_emptyVsValue() {
        Signal<String> oldSignal = Signal.of(null);
        Signal<String> newSignal = Signal.of(null);
        Side oldSide = new Side();
        Side newSide = new Side();

        oldSide.parent.children(() -> {
            new LegacyDynamic<>(oldSignal, icon -> {
                if (icon == null || icon.trim().isEmpty()) {
                    return new Leaf("none");
                }
                return new Leaf("icon:" + icon);
            }).growX();
        });
        newSide.parent.children(() -> {
            Dynamic.of(newSignal, icon -> {
                if (icon == null || icon.trim().isEmpty()) {
                    new Leaf("none");
                } else {
                    new Leaf("icon:" + icon);
                }
            }).growX();
        });
        SignalDispatcher.flush();

        Table oldBox = (Table) oldSide.parentTable.getChildren().first();
        Table newBox = (Table) newSide.parentTable.getChildren().first();
        assertParity("none", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        oldSignal.set("star");
        newSignal.set("star");
        SignalDispatcher.flush();
        assertParity("icon", oldBox, oldSide.parentTable, newBox, newSide.parentTable);

        assertTrue(structure(oldBox).equals(structure(newBox)), "leaf structures match");
        finish(oldBox, oldSide, newBox, newSide);
    }
}
