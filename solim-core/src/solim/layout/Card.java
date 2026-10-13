package solim.layout;
import solim.modifier.CellConfig;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;

/**
 * Clickable card container built on a single Arc Table with lazy click
 * handling borrowed from Arc Button semantics.
 */
public final class Card extends Component implements CellConfig<Card>, ElementConfig<Card>, TableConfig<Card>, GapContainer {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        if (SolimToken.isExpandingChild(child)) {
            cell.growY();
        }
        cell.row();
        GapContainer gc = GapContainer.find(table);
        if (gc != null) {
            GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;
    private boolean attached = false;

    public Card() {
        this(new Table());
    }

    private Card(Table table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-card-table";
        this.table.top().left();
        this.table.defaults().top().left();
    }

    public Card(@Nullable Drawable background) {
        this();
        if (background != null) {
            background(background);
        }
    }

    public Table container() {
        return table;
    }

    @Override
    public Table table() {
        return table;
    }


    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public Card color(@Nullable Color color) {
        if (color != null) {
            table.setColor(color);
        }
        return this;
    }

    public Card color(@Nullable Readable<Color> color) {
        if (color != null) {
        own(Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    table.setColor(c);
                }
            }));
        }
        return this;
    }

    public Card children(@Nullable Runnable r) {
        OwnershipContext.push(this);
        AttachmentStack.push(table, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        attachSelf();
        respace();
        return this;
    }

    private void attachSelf() {
        if (!attached) {
            attached = true;
            AttachmentStack.attachToParent(table);
        }
    }

    public Card gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    @Override
    public Direction direction() {
        return Direction.VERTICAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.VERTICAL, gap);
    }



    @Override
    public Card align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        table.defaults().align(align);
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.align(align);
            }
        }
        return this;
    }

    @Override
    public Card center() {
        TableConfig.super.center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.center();
            }
        }
        return this;
    }

    @Override
    public Card top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.top();
            }
        }
        return this;
    }

    @Override
    public Card bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.bottom();
            }
        }
        return this;
    }

    @Override
    public Card left() {
        TableConfig.super.left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.left();
            }
        }
        return this;
    }

    @Override
    public Card right() {
        TableConfig.super.right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.right();
            }
        }
        return this;
    }

    @Override
    public Card name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Card self() {
        return this;
    }
}
