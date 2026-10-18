package solim.display;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.SpacingAware;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.LabelCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.TextCapability;
import solim.modifier.WrappableCapability;
import solim.reactive.Effect;
import solim.reactive.Readable;

public final class Text extends Component
        implements SpacingAware, Configurable<Text>, ElementCapability, CellCapability, AlignmentCapability, TextCapability, LabelCapability, WrappableCapability {

    private final Label label;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Text> pending;

    private float padTop;
    private float padLeft;
    private float padBottom;
    private float padRight;

    private float marginTop;
    private float marginLeft;
    private float marginBottom;
    private float marginRight;

    private boolean wrap = false;
    private boolean growX = false;
    private boolean growY = false;

    public Text() {
        this("");
    }

    public Text(String text) {
        this(text, (Label.LabelStyle) null);
    }

    private static @Nullable Label.LabelStyle defaultHeadlessStyle;

    private static Label.LabelStyle getDefaultStyle() {
        if (defaultHeadlessStyle == null) {
            FontData fontData = new FontData() {
                @Override
                public boolean hasGlyph(char ch) {
                    return true;
                }
            };
            Font font = new Font(fontData, new TextureRegion(), false);
            defaultHeadlessStyle = new Label.LabelStyle(font, Color.white);
        }
        return defaultHeadlessStyle;
    }

    public Text(String text, Label.LabelStyle style) {
        this(createLabel(text, style));
    }

    private Text(Label label) {
        super(label);
        this.label = label;
        label.name = "solim-text-label";
        SolimToken.bind(label, this, constraints);
    }

    private static Label createLabel(String text, @Nullable Label.LabelStyle style) {
        String content = text != null ? text : "";
        if (style != null) {
            return new Label(content, style);
        }
        return Core.scene != null ? new Label(content) : new Label(content, getDefaultStyle());
    }

    public Text(Readable<String> readable) {
        this("");
        setText(readable);
    }

    public static Text of(String text) {
        return new Text(text);
    }

    public static Text of(Readable<String> readable) {
        return new Text(readable);
    }

    public void setText(String text) {
        label.setText(text != null ? text : "");
    }

    public void setText(Readable<String> text) {
        if (text != null) {
            withinOwnership(() -> Effect.of(() -> label.setText(text.get() != null ? text.get() : "")));
        }
    }

    public void setFontScale(float scale) {
        label.setFontScale(scale);
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
        label.setWrap(wrap);
        if (wrap) {
            this.growX = true;
            SolimToken.setExpanding(label, true);
            if (label.parent instanceof Table) {
                Cell<?> cell = ((Table) label.parent).getCell(label);
                if (cell != null) {
                    cell.growX();
                    cell.minWidth(0f);
                    ((Table) label.parent).invalidateHierarchy();
                }
            }
        }
    }

    public void setEllipsis(boolean ellipsis) {
        label.setEllipsis(ellipsis);
    }

    public void setStyle(Label.LabelStyle style) {
        if (style != null) {
            label.setStyle(style);
        }
    }

    public void applySpacing() {
        if (label.parent instanceof Table) {
            Cell<?> cell = ((Table) label.parent).getCell(label);
            if (cell != null) {
                cell.pad(padTop + marginTop, padLeft + marginLeft, padBottom + marginBottom, padRight + marginRight);
                if (growX || wrap) {
                    cell.growX();
                    cell.minWidth(0f);
                }
                if (growY) {
                    cell.growY();
                }
            }
        }
    }

    public Label label() {
        applySpacing();
        return label;
    }

    public boolean isWrap() {
        return wrap;
    }


    @Override
    public Table table() {
        return null;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<Text> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Text> props) {
        this.pending = props;
    }

    @Override
    public Text name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public void alignCoordinated(@Nullable Integer explicit, int named) {
        AlignmentCapability.coordinateCell(constraints, label, explicit, named);
        int a = explicit != null ? explicit : constraints.align != null ? constraints.align : named;
        label.setAlignment(a);
    }

    @Override
    public Text self() {
        return this;
    }
}
