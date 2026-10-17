package solim.layout;


import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.core.Units;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.runtime.AttachmentStack;
import solim.runtime.OwnershipScope;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.Values;

public final class Tabs extends Component implements Configurable<Tabs>, GapHost {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        if (SolimToken.isExpandingChild(child)) {
            cell.growY();
        }
        cell.row();
        return cell;
    };

    private final Table root;
    private final Row headerBar;
    private final SolimStack contentStack;
    private final Signal<Integer> activeTab;
    private @Nullable ButtonStyle tabButtonStyle;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Table> tabContents = new ArrayList<>();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Tabs> pending;

    public Tabs(Signal<Integer> activeTab) {
        this(activeTab, new Table());
    }

    private Tabs(Signal<Integer> activeTab, Table root) {
        super(root);
        this.activeTab = activeTab;
        this.root = root;
        SolimToken.bind(this.root, this, constraints);
        this.root.name = "solim-tabs-root";
        this.root.top().left();

        OwnershipContext.push(this);
        try {
            this.headerBar = new Row();
            this.headerBar.name("solim-tabs-headerBar");
            this.headerBar.props(Values.gap(4f), Values.top(), Values.left(), Values.height(Units.unit(14)), Values.prefHeight(Units.unit(14)));
            this.root.add(headerBar.element()).growX().row();

            this.contentStack = new SolimStack();
            this.root.add(contentStack.element()).grow();
        } finally {
            OwnershipContext.pop();
        }
    }

    public static Tabs of(Signal<Integer> activeTab) {
        return new Tabs(activeTab);
    }

    @Override
    public void setGap(float gap) {
        headerGap(gap);
    }

    public Tabs headerGap(float gap) {
        this.headerBar.setGap(gap);
        return this;
    }

    public Tabs tabStyle(@Nullable ButtonStyle style) {
        this.tabButtonStyle = style;
        return this;
    }

    public Tabs tab(String title, Runnable contentBuilder) {
        return tab(Readable.of(title), null, contentBuilder);
    }

    public Tabs tab(Readable<String> title, Runnable contentBuilder) {
        return tab(title, null, contentBuilder);
    }

    public Tabs tab(String title, @Nullable Drawable icon, Runnable contentBuilder) {
        return tab(Readable.of(title), icon, contentBuilder);
    }

    public Tabs tab(Readable<String> title, @Nullable Drawable icon, Runnable contentBuilder) {
        int index = tabButtons.size();

        OwnershipContext.push(this);
        Button btn;
        try {
            btn = new Button(tabButtonStyle);
            if (tabButtonStyle == null) {
                btn.rounded(10, Color.clear).border(2f, Color.gray);
                ButtonStyle s = btn.button().getStyle();
                if (s != null) {
                    s.checked = RoundedDrawable.of(10, new Color(1f, 1f, 1f, 0.12f), 2f, Color.white);
                    s.over = RoundedDrawable.of(10, new Color(1f, 1f, 1f, 0.06f), 2f, Color.lightGray);
                    s.down = RoundedDrawable.of(10, new Color(1f, 1f, 1f, 0.18f), 2f, Color.white);
                }
            }
            btn.onClick(() -> activeTab.set(index));
            btn.checked(activeTab.map(idx -> idx != null && idx == index));
        } finally {
            OwnershipContext.pop();
        }
        btn.props(Values.grow());

        btn.children(() -> {
            btn.icon(icon).text(title);
        });
        tabButtons.add(btn);
        headerBar.table().add(btn.element()).growX();
        headerBar.respace();

        Table contentContainer = new Table();
        contentContainer.top().left();
        SolimToken.setExpanding(contentContainer, true);
        tabContents.add(contentContainer);
        contentStack.add(contentContainer);

        boolean[] built = new boolean[] { false };
        Runnable mountContent = () -> {
            if (!built[0] && contentBuilder != null) {
                built[0] = true;
                OwnershipScope scope = OwnershipScope.open();
                AttachmentStack.push(contentContainer, Column.ATTACHER);
                try {
                    contentBuilder.run();
                } finally {
                    AttachmentStack.pop();
                    scope.adoptInto(this);
                    scope.close();
                }
            }
        };
        OwnershipContext.push(this);
        try {
            Effect.of(() -> {
                Integer cur = activeTab.get();
                boolean isActive = (cur != null && cur == index);
                if (isActive) {
                    mountContent.run();
                }
                contentContainer.visible = isActive;
                contentContainer.setLayoutEnabled(isActive);
            });
        } finally {
            OwnershipContext.pop();
        }

        return this;
    }

    public List<Button> buttons() {
        return tabButtons;
    }

    public List<Table> contents() {
        return tabContents;
    }

    public Table root() {
        return root;
    }

    public Table headerBar() {
        return headerBar.table();
    }

    @Override
    public Table table() {
        return root;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public Tabs children(@Nullable Runnable r) {
        applyPendingProps();
        OwnershipContext.push(this);
        AttachmentStack.push(root, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        AttachmentStack.attachToParent(root);
        return this;
    }

    @Override
    public @Nullable Props<Tabs> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Tabs> props) {
        this.pending = props;
    }

    @Override
    public Tabs name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Tabs self() {
        return this;
    }
}
