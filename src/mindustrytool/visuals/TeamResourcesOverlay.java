package mindustrytool.visuals;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.util.Interval;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.modules.ItemModule;

/**
 * Overlay displaying team resources (items) and power statistics.
 * Heavily inspired by scheme-size CoreInfoFragment for consistent behavior.
 */
public class TeamResourcesOverlay extends Table {

    // Settings keys
    private static final String PREFIX = "teamresources.";

    // Persistent settings
    private float opacity = Core.settings.getFloat(PREFIX + "opacity", 1f);
    private float scale = Core.settings.getFloat(PREFIX + "scale", 1f);
    private float overlayWidth = Core.settings.getFloat(PREFIX + "overlayWidth", 0.3f); // 30% default
    private boolean showItems = Core.settings.getBool(PREFIX + "showItems", true);
    private boolean showUnits = Core.settings.getBool(PREFIX + "showUnits", false);
    private boolean showPower = Core.settings.getBool(PREFIX + "showPower", true);
    private boolean showStoredPower = Core.settings.getBool(PREFIX + "showStoredPower", false);
    private boolean hideBackground = Core.settings.getBool(PREFIX + "hideBackground", false);

    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<mindustry.type.UnitType> usedUnits = new ObjectSet<>();
    private Team selectedTeam;
    private ItemModule coreItems;
    private ItemModule lastSnapshot = new ItemModule();
    private ItemModule rateDisplay = new ItemModule();

    // Stats viewing state
    private boolean viewingStats = false;
    private boolean holdingForStats = false; // Mobile touch support

    private final Interval timer = new Interval(2);

    // Power
    private Building powerNode;
    private PowerGraph powerGraph = new PowerGraph();

    // Singleton instance
    private static TeamResourcesOverlay instance;

    public TeamResourcesOverlay() {
        super();
        Log.info("[TeamResources] Initializing overlay...");

        selectedTeam = Vars.player.team();

        // Setup events
        Events.run(WorldLoadEvent.class, () -> {
            selectedTeam = Vars.player.team();
            usedItems.clear();
            lastSnapshot.clear();
            rebuild();
            refreshPowerNode();
        });

        Events.run(BlockBuildEndEvent.class, this::refreshPowerNode);
        Events.run(BlockDestroyEvent.class, this::refreshPowerNode);
        Events.run(ResetEvent.class, () -> {
            usedItems.clear();
            clear();
        });

        // Add self to HUD
        Core.app.post(() -> {
            if (Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(this);
                Log.info("[TeamResources] Added to hudGroup");
            }
        });

        setup();

        // If already in game, force build
        if (Vars.state.isGame()) {
            rebuild();
            refreshPowerNode();
        }

        instance = this;
    }

    public static TeamResourcesOverlay getInstance() {
        return instance;
    }

    // State for power node selection
    private boolean choosesNode = false;

    public void cleanup() {
        remove();
        Log.info("[TeamResources] Cleaned up");
    }

    private void setup() {
        name = "team-resources-overlay";

        // Remove ANY existing overlay with this name to ensure singleton behavior
        if (Vars.ui.hudGroup.find("team-resources-overlay") != null) {
            Vars.ui.hudGroup.find("team-resources-overlay").remove();
        }

        setFillParent(true);
        top();

        // Ensure we don't block input on empty space
        touchable = arc.scene.event.Touchable.childrenOnly;

        // Visibility provider
        visible(() -> {
            boolean enabled = Core.settings.getBool("lazy.Team Resources.enabled", false);
            return enabled && Vars.ui.hudfrag.shown && Vars.state.isGame();
        });

        // Update loop
        update(() -> {
            if (!visible)
                return; // Optimization

            // Robust Input Handling for Node Selection
            if (choosesNode && Core.input.justTouched()) {
                if (Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) == null) {
                    float wx = Core.input.mouseWorld().x;
                    float wy = Core.input.mouseWorld().y;

                    mindustry.world.Tile t = Vars.world.tileWorld(wx, wy);
                    if (t != null && t.build != null && t.build.power != null) {
                        powerNode = t.build;
                        powerGraph = t.build.power.graph;
                        choosesNode = false;
                        rebuild();
                    }
                }
            }

            coreItems = (selectedTeam.data().hasCore() && selectedTeam.core() != null)
                    ? selectedTeam.core().items
                    : null;

            if (coreItems == null)
                return;

            // Auto-recover if empty
            if (getChildren().isEmpty())
                rebuild();

            // Discover new items
            if (Vars.content.items().contains(item -> coreItems.get(item) > 0 && usedItems.add(item))) {
                rebuild();
            }

            // Update stats rate
            if (viewingStats && timer.get(0, 30f)) {
                updateRates();
            }
        });
    }

    private boolean showTeamSelector = false;

    // Listener for stats holding & tapping for team toggle
    private final arc.scene.event.ClickListener statsListener = new arc.scene.event.ClickListener() {
        @Override
        public void clicked(arc.scene.event.InputEvent event, float x, float y) {
            // Toggle Team Selector on Click
            showTeamSelector = !showTeamSelector;
            rebuild();
        }

        @Override
        public boolean touchDown(arc.scene.event.InputEvent event, float x, float y, int pointer,
                arc.input.KeyCode button) {
            holdingForStats = true;
            viewingStats = true;
            lastSnapshot.clear();
            return super.touchDown(event, x, y, pointer, button);
        }

        @Override
        public void touchUp(arc.scene.event.InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
            holdingForStats = false;
            if (!Vars.mobile) {
                if (!hovered)
                    viewingStats = false;
            } else {
                viewingStats = false;
            }
            lastSnapshot.clear();
            super.touchUp(event, x, y, pointer, button);
        }

        @Override
        public void enter(arc.scene.event.InputEvent event, float x, float y, int pointer,
                arc.scene.Element fromActor) {
            if (pointer == -1 && !holdingForStats && !choosesNode) {
                viewingStats = true;
                lastSnapshot.clear();
            }
            super.enter(event, x, y, pointer, fromActor);
        }

        @Override
        public void exit(arc.scene.event.InputEvent event, float x, float y, int pointer, arc.scene.Element toActor) {
            if (pointer == -1 && !holdingForStats) {
                viewingStats = false;
                lastSnapshot.clear();
            }
            super.exit(event, x, y, pointer, toActor);
        }
    };

    private boolean hovered = false; // Track hover state manually if needed, or use listener

    public void rebuild() {
        clear();
        top();

        // Apply opacity
        this.color.a = opacity;

        // Choose background based on settings
        arc.scene.style.Drawable bg = hideBackground ? null : Styles.black3;

        // Calculate max width based on overlayWidth setting, enforce min width (200px)
        // for very small screens
        boolean mobile = Vars.mobile;
        float screenW = Core.graphics.getWidth();
        float calculatedWidth = screenW * overlayWidth;
        // On mobile portrait, ensure we use at least reasonable width
        float minWidth = mobile && Core.graphics.getHeight() > screenW ? screenW * 0.8f : 240f;

        float widthToUse;
        if (!mobile) {
            widthToUse = Math.max(calculatedWidth, 120f);
        } else {
            widthToUse = Math.max(calculatedWidth, minWidth);
        }
        final float finalWidth = widthToUse;

        // Main Layout - Auto-expanding (No Scroll)
        table(bg, t -> {
            // Apply scale to margin (formerly in scroll pane)
            t.margin(Scl.scl(8f * scale));

            // --- TOP: Team Selector ---
            t.collapser(c -> {
                c.pane(p -> {
                    p.left();

                    // Filter for teams that actually have a core (real teams)
                    arc.struct.Seq<Team> validTeams = new arc.struct.Seq<>();
                    for (Team team : Team.all) {
                        if (team.active() && team.data().hasCore()) {
                            validTeams.add(team);
                        }
                    }

                    for (Team team : validTeams) {
                        p.button(b -> {
                            b.image().size(24f).color(team.color);
                        }, Styles.clearTogglei, () -> {
                            selectedTeam = team;
                            powerNode = null;
                            usedUnits.clear();
                            rebuild();
                        }).checked(sel -> selectedTeam == team).size(32f).pad(2f);
                    }

                    if (validTeams.size > 5) {
                        p.button("...", Styles.flatBordert, this::showAllTeamsDialog).size(32f).pad(2f);
                    }

                }).scrollX(true).scrollY(false).height(40f).growX();
            }, true, () -> showTeamSelector).growX().row();

            // --- Items Grid ---
            if (showItems) {
                t.table(itemsTable -> {
                    itemsTable.left();
                    itemsTable.touchable = arc.scene.event.Touchable.enabled;
                    itemsTable.addListener(statsListener);

                    // Calculate actual cell width: icon + label + padding
                    float iconSize = Scl.scl(Vars.iconSmall * scale);
                    float labelWidth = Scl.scl(40f * scale);
                    float padding = Scl.scl(6f * scale);
                    float cellWidth = iconSize + labelWidth + padding;

                    // Calculate columns to fill overlay width
                    int cols = Math.max(2, (int) (finalWidth / cellWidth));

                    int i = 0;
                    for (Item item : Vars.content.items()) {
                        if (!usedItems.contains(item))
                            continue;

                        itemsTable.image(item.uiIcon).size(iconSize).padRight(Scl.scl(3f * scale));
                        itemsTable.label(() -> formatItem(item)).fontScale(0.8f * scale)
                                .padRight(Scl.scl(3f * scale))
                                .minWidth(labelWidth).left();

                        if (++i % cols == 0)
                            itemsTable.row();
                    }
                }).growX().row();
            }

            // --- Units Grid ---
            if (showUnits) {
                t.table(unitsTable -> {
                    unitsTable.left();

                    // Update used units
                    for (mindustry.type.UnitType type : Vars.content.units()) {
                        int count = selectedTeam.data().countType(type);
                        if (count > 0)
                            usedUnits.add(type);
                    }

                    // Calculate unit cell size (icon + padding)
                    float unitSize = (Vars.iconSmall + 8f) * scale;
                    float unitPad = Scl.scl(4f * scale);
                    float cellWidth = Scl.scl(unitSize) + unitPad;

                    // Calculate columns to fill overlay width
                    int cols = Math.max(2, (int) (finalWidth / cellWidth));

                    int i = 0;
                    for (mindustry.type.UnitType type : Vars.content.units()) {
                        if (!usedUnits.contains(type))
                            continue;

                        unitsTable.stack(
                                new arc.scene.ui.Image(type.uiIcon).setScaling(arc.util.Scaling.fit),
                                new Table(lab -> lab.label(() -> {
                                    int c = selectedTeam.data().countType(type);
                                    return c > 0 ? UI.formatAmount(c) : "";
                                }).fontScale(0.72f * scale).style(Styles.outlineLabel)).right().bottom())
                                .size(unitSize)
                                .pad(Scl.scl(2f * scale));

                        if (++i % cols == 0)
                            unitsTable.row();
                    }
                }).growX().padTop(Scl.scl(6f * scale)).row();
            }

            // --- BOTTOM: Power Info (Fixed) ---
            if (showPower) {
                t.table(power -> {
                    // Apply scale to bar height
                    float barHeight = Scl.scl(18f * scale);
                    power.defaults().height(barHeight).growX();
                    power.touchable = arc.scene.event.Touchable.enabled;
                    power.addListener(new arc.scene.event.ClickListener() {
                        @Override
                        public void clicked(arc.scene.event.InputEvent event, float x, float y) {
                            choosesNode = !choosesNode;
                            rebuild();
                        }
                    });

                    refreshPowerNode();

                    if (choosesNode) {
                        power.add(Core.bundle.format("Select Power Node")).fontScale(scale).color(Pal.accent)
                                .height(barHeight).row();
                    }

                    // Power Balance Bar with scalable text
                    power.add(new ScaledBar(
                            () -> Core.bundle.format("bar.powerbalance",
                                    (powerGraph.getPowerBalance() >= 0 ? "+" : "")
                                            + UI.formatAmount((long) (powerGraph.getPowerBalance() * 60))),
                            () -> Pal.powerBar,
                            () -> Mathf.clamp(powerGraph.getSatisfaction()),
                            scale)).row();

                    // Power Stored Bar with scalable text
                    if (showStoredPower) {
                        power.add(new ScaledBar(
                                () -> Core.bundle.format("bar.powerstored",
                                        UI.formatAmount((long) powerGraph.getLastPowerStored()),
                                        UI.formatAmount((long) powerGraph.getLastCapacity())),
                                () -> Pal.powerBar,
                                () -> powerGraph.getLastCapacity() > 0
                                        ? powerGraph.getLastPowerStored() / powerGraph.getLastCapacity()
                                        : 0,
                                scale)).padTop(Scl.scl(4f * scale));
                    }
                }).growX().padTop(Scl.scl(6f * scale));
            }
        }).width(finalWidth);
    }

    // Custom Bar with fontScale support
    private static class ScaledBar extends Bar {
        private final float fontScale;

        public ScaledBar(arc.func.Prov<CharSequence> name, arc.func.Prov<arc.graphics.Color> color,
                arc.func.Floatp fraction,
                float fontScale) {
            super(name, color, fraction);
            this.fontScale = fontScale;
        }

        @Override
        public void draw() {
            // Bar uses Fonts.outline for text rendering (verified from Mindustry source)
            arc.graphics.g2d.Font font = mindustry.ui.Fonts.outline;
            float originalScaleX = font.getScaleX();
            float originalScaleY = font.getScaleY();
            arc.graphics.Color originalColor = new arc.graphics.Color(font.getColor());

            // Apply fontScale
            font.getData().setScale(originalScaleX * fontScale, originalScaleY * fontScale);

            super.draw();

            // Restore original scale and color to prevent global state pollution
            font.getData().setScale(originalScaleX, originalScaleY);
            font.setColor(originalColor);
        }
    }

    private void showAllTeamsDialog() {
        new mindustry.ui.dialogs.BaseDialog("All Teams") {
            {
                addCloseButton();
                cont.pane(p -> {
                    p.defaults().width(150f).height(50f).pad(5f);
                    int i = 0;
                    for (Team team : Team.all) {
                        p.button(b -> {
                            b.image().color(team.color).margin(4f).size(24f).padRight(10f);
                            b.add(team.localized()).color(team.color);
                        }, Styles.flatTogglet, () -> {
                            selectedTeam = team;
                            powerNode = null; // Force reset
                            rebuild();
                            hide();
                        }).checked(team == selectedTeam);

                        if (++i % 3 == 0)
                            p.row();
                    }
                }).grow();
            }
        }.show();
    }

    private void updateRates() {
        if (coreItems == null)
            return;
        if (lastSnapshot.any()) {
            // Rate per second = (Diff) * 60 / (Timer=30 frames?)
            // Scheme-size uses: (amount - last) and formats as /s.
            // But timer runs every 30 ticks (0.5s).
            // So diff is amount gained in 0.5s.
            // Per second = Diff * 2.
            coreItems.each((item, amount) -> rateDisplay.set(item, (amount - lastSnapshot.get(item)) * 2));
        }
        lastSnapshot.set(coreItems);
    }

    private String formatItem(Item item) {
        if (coreItems == null)
            return "0";
        int amount = coreItems.get(item);

        if (viewingStats) {
            int rate = rateDisplay.get(item);
            // Scheme-size logic: 0 -> "", >0 -> [lime]+, else [scarlet]
            if (rate == 0)
                return "[gray]0/s";
            String prefix = rate > 0 ? "[lime]+" : "[scarlet]";
            return prefix + rate + "[gray]/s";
        }
        return UI.formatAmount(amount);
    }

    private void refreshPowerNode() {
        // Inspecting enemy power is allowed for consistency with similar tools.
        // Team ownership is not strictly enforced during inspection.

        if (powerNode != null && powerNode.isValid() && powerNode.team == selectedTeam) {
            // Check connectivity?
            if (powerNode.power != null) {
                powerGraph = powerNode.power.graph;
                return; // Keep existing selection!
            }
        }

        // Default to Core
        if (selectedTeam.data().hasCore() && selectedTeam.core() != null && selectedTeam.core().power != null) {
            powerNode = selectedTeam.core();
            powerGraph = selectedTeam.core().power.graph;
        } else {
            powerNode = null;
            powerGraph = new PowerGraph();
        }
    }

    public void showDialog() {
        new mindustry.ui.dialogs.BaseDialog(Core.bundle.get("team-resources.settings", "Team Resources Settings")) {
            {
                // addCloseButton();

                buttons.button("@back", mindustry.gen.Icon.left, this::hide).size(210f, 64f);

                buttons.button("Reset to Defaults", mindustry.gen.Icon.refresh, () -> {
                    // Reset values
                    opacity = 1f;
                    scale = 1f;
                    overlayWidth = 0.3f;
                    showItems = true;
                    showUnits = false;
                    showPower = true;
                    showStoredPower = false;
                    hideBackground = false;

                    // Save settings
                    Core.settings.put(PREFIX + "opacity", opacity);
                    Core.settings.put(PREFIX + "scale", scale);
                    Core.settings.put(PREFIX + "overlayWidth", overlayWidth);
                    Core.settings.put(PREFIX + "showItems", showItems);
                    Core.settings.put(PREFIX + "showUnits", showUnits);
                    Core.settings.put(PREFIX + "showPower", showPower);
                    Core.settings.put(PREFIX + "showStoredPower", showStoredPower);
                    Core.settings.put(PREFIX + "hideBackground", hideBackground);

                    // Rebuild overlay
                    rebuild();

                    // Re-open dialog to refresh UI elements (sliders, checkboxes)
                    hide();
                    showDialog();
                }).size(210f, 64f);

                Table cont = this.cont;
                cont.clear();
                cont.defaults().pad(6).left();

                float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

                // --- Opacity Slider ---
                arc.scene.ui.Slider opacitySlider = new arc.scene.ui.Slider(0.1f, 1f, 0.05f, false);
                opacitySlider.setValue(opacity);

                arc.scene.ui.Label opacityValue = new arc.scene.ui.Label(Math.round(opacity * 100) + "%",
                        Styles.outlineLabel);
                opacityValue.setColor(arc.graphics.Color.lightGray);

                Table opacityContent = new Table();
                opacityContent.touchable = arc.scene.event.Touchable.disabled;
                opacityContent.margin(3f, 33f, 3f, 33f);
                opacityContent.add("Opacity", Styles.outlineLabel).left().growX();
                opacityContent.add(opacityValue).padLeft(10f).right();

                opacitySlider.changed(() -> {
                    opacity = opacitySlider.getValue();
                    opacityValue.setText(Math.round(opacity * 100) + "%");
                    Core.settings.put(PREFIX + "opacity", opacity);
                    rebuild();
                });

                cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

                // --- Size Slider ---
                arc.scene.ui.Slider sizeSlider = new arc.scene.ui.Slider(0.5f, 2f, 0.1f, false);
                sizeSlider.setValue(scale);

                arc.scene.ui.Label sizeValue = new arc.scene.ui.Label(Math.round(scale * 100) + "%",
                        Styles.outlineLabel);
                sizeValue.setColor(arc.graphics.Color.lightGray);

                Table sizeContent = new Table();
                sizeContent.touchable = arc.scene.event.Touchable.disabled;
                sizeContent.margin(3f, 33f, 3f, 33f);
                sizeContent.add("Size", Styles.outlineLabel).left().growX();
                sizeContent.add(sizeValue).padLeft(10f).right();

                sizeSlider.changed(() -> {
                    scale = sizeSlider.getValue();
                    sizeValue.setText(Math.round(scale * 100) + "%");
                    Core.settings.put(PREFIX + "scale", scale);
                    rebuild();
                });

                cont.stack(sizeSlider, sizeContent).width(width).left().padTop(4f).row();

                // --- Width Slider ---
                arc.scene.ui.Slider widthSlider = new arc.scene.ui.Slider(0.3f, 1f, 0.05f, false);
                widthSlider.setValue(overlayWidth);

                arc.scene.ui.Label widthValue = new arc.scene.ui.Label(Math.round(overlayWidth * 100) + "%",
                        Styles.outlineLabel);
                widthValue.setColor(arc.graphics.Color.lightGray);

                Table widthContent = new Table();
                widthContent.touchable = arc.scene.event.Touchable.disabled;
                widthContent.margin(3f, 33f, 3f, 33f);
                widthContent.add("Width", Styles.outlineLabel).left().growX();
                widthContent.add(widthValue).padLeft(10f).right();

                widthSlider.changed(() -> {
                    overlayWidth = widthSlider.getValue();
                    widthValue.setText(Math.round(overlayWidth * 100) + "%");
                    Core.settings.put(PREFIX + "overlayWidth", overlayWidth);
                    rebuild();
                });

                cont.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();

                // --- Checkboxes ---
                cont.check("Show Items", showItems, b -> {
                    showItems = b;
                    Core.settings.put(PREFIX + "showItems", b);
                    rebuild();
                }).left().padTop(4).row();

                cont.check("Show Units", showUnits, b -> {
                    showUnits = b;
                    Core.settings.put(PREFIX + "showUnits", b);
                    rebuild();
                }).left().padTop(4).row();

                cont.check("Show Power", showPower, b -> {
                    showPower = b;
                    Core.settings.put(PREFIX + "showPower", b);
                    rebuild();
                }).left().padTop(4).row();

                cont.check("Show Stored Power", showStoredPower, b -> {
                    showStoredPower = b;
                    Core.settings.put(PREFIX + "showStoredPower", b);
                    rebuild();
                }).left().padTop(4).row();

                cont.check("Hide Background", hideBackground, b -> {
                    hideBackground = b;
                    Core.settings.put(PREFIX + "hideBackground", b);
                    rebuild();
                }).left().padTop(4).row();
            }
        }.show();
    }
}
