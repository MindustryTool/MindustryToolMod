package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.List;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Readable;

/**
 * Floating draggable HUD rendering the configured schematic grid with
 * orientation-aware position persistence and screen clamping.
 */
public class QuickSchematicGridHudView extends BaseComponent {

    private final QuickSchematicGridFeature feature;
    private @Nullable Hud hud;

    public QuickSchematicGridHudView(QuickSchematicGridFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        Readable<Float> dragIconSize = buttonSize.map(s -> (s != null ? s : 48f) * 0.45f);

        hud = hud(() -> {
            dynamic(feature.hideDragHandleConfig.signal(), hide -> {
                if (!Boolean.TRUE.equals(hide)) {
                    return button()
                            .style(Styles.clearNonei)
                            .background(Styles.black6)
                            .size(buttonSize)
                            .children(() -> icon(Icon.move).size(dragIconSize))
                            .draggable(feature.xSignal, feature.ySignal);
                }
                return null;
            });

            buildGrid(feature, null);
        }).gap(unit(1));

        hud.position(feature.xSignal, feature.ySignal)
                .rounded(unit(2))
                .background(Styles.black6);

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            buttonSize.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildGrid(QuickSchematicGridFeature feature, @Nullable Runnable onBeforeActivate) {
        Readable<List<QuickSchematicEntry>> entries = feature.entries();
        return reactiveGrid(
                feature.colsConfig.signal(),
                entries,
                entry -> entry != null && entry.id != null ? entry.id : "",
                entry -> schematicButton(feature, entry, onBeforeActivate))
                .gap(feature.buttonGapConfig.signal());
    }

    static Component schematicButton(
            QuickSchematicGridFeature feature,
            @Nullable QuickSchematicEntry entry,
            @Nullable Runnable onBeforeActivate) {
        if (entry == null) {
            return row();
        }
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        Schematic schematic = feature.resolveSchematic(entry);
        if (schematic == null) {
            String missingLabel = entry.displayName() != null && !entry.displayName().trim().isEmpty()
                    ? entry.displayName()
                    : entry.schematicName;
            return button(() -> {
                if (onBeforeActivate != null) {
                    onBeforeActivate.run();
                }
                feature.useEntry(entry);
            })
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.format("feature.quick-schematic-grid.tooltip.missing",
                            missingLabel != null ? missingLabel : ""))
                    .children(() -> icon(Icon.warning).size(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f))
                            .color(Color.scarlet));
        }
        String tooltip = Core.bundle.format("feature.quick-schematic-grid.tooltip.use", schematic.name());
        return button(() -> {
            if (onBeforeActivate != null) {
                onBeforeActivate.run();
            }
            feature.useSchematic(schematic);
        })
                .style(WebStyles.ghost())
                .size(buttonSize)
                .tooltip(tooltip)
                .children(() -> {
                    // FillParent makes the image track the fixed-size button bounds so
                    // Scaling.fit centers the preview instead of anchoring top-left
                    // at its native preferred size.
                    SchematicImage image = new SchematicImage(schematic);
                    image.setScaling(Scaling.fit);
                    image.setFillParent(true);
                    arc(image);
                });
    }

    public @Nullable Hud getHud() {
        return hud;
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
