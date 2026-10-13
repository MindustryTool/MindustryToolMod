package solim.layout;

import solim.modifier.CellConfig;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.Units;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.runtime.AttachmentStack;
import solim.runtime.OwnershipScope;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;

public final class Tabs extends Component implements CellConfig<Tabs>, ElementConfig<Tabs>, TableConfig<Tabs> {

    private final Table root;
    private final Row headerBar;
    private final SolimStack contentStack;
    private final Signal<Integer> activeTab;
    private @Nullable ButtonStyle tabButtonStyle;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Table> tabContents = new ArrayList<>();
    private final PendingCellConfig constraints = new PendingCellConfig();

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

        this.headerBar = new Row().gap(4f);
        own(this.headerBar);
        this.headerBar.name("solim-tabs-headerBar");
        this.headerBar.top().left();
        this.headerBar.height(Units.unit(14));
        this.root.add(headerBar.element()).growX().row();

        this.contentStack = new SolimStack();
        own(this.contentStack);
        this.root.add(contentStack.element()).grow();
    }

    public static Tabs of(Signal<Integer> activeTab) {
        return new Tabs(activeTab);
    }

    public Tabs headerGap(float gap) {
        this.headerBar.gap(gap);
        return this;
    }

    public Tabs gap(float gap) {
        return headerGap(gap);
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

        Button btn = new Button(tabButtonStyle);
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
        btn.grow();

        btn.children(() -> {
            btn.icon(icon).text(title);
        });
        tabButtons.add(btn);
        own(btn);
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
        own(Effect.of(() -> {
            Integer cur = activeTab.get();
            boolean isActive = (cur != null && cur == index);
            if (isActive) {
                mountContent.run();
            }
            contentContainer.visible = isActive;
            contentContainer.setLayoutEnabled(isActive);
        }));

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
