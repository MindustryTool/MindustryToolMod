package mindustrytool.ui.dock;

import arc.Core;
import arc.func.Prov;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import solim.core.Units;
import solim.overlay.DockEngine;
import solim.overlay.DockSide;
import solim.overlay.DockTarget;

public final class MindustryDockRegistry {

    public static final String TARGET_MINIMAP = "minimap";
    public static final String TARGET_WAVES = "waves/editor";
    public static final String TARGET_BLOCKS = "blocks";
    public static final String TARGET_CORE_ITEMS = "coreitems";

    public static final String TARGET_SCREEN_BOTTOM = "screen-bottom";
    public static final String TARGET_SCREEN_TOP = "screen-top";
    public static final String TARGET_SCREEN_LEFT = "screen-left";
    public static final String TARGET_SCREEN_RIGHT = "screen-right";
    public static final String TARGET_SCREEN_CORNER_BOTTOM_LEFT = "screen-corner-bottom-left";
    public static final String TARGET_SCREEN_CORNER_BOTTOM_RIGHT = "screen-corner-bottom-right";
    public static final String TARGET_SCREEN_CORNER_TOP_LEFT = "screen-corner-top-left";
    public static final String TARGET_SCREEN_CORNER_TOP_RIGHT = "screen-corner-top-right";

    private static final MindustryDockRegistry INSTANCE = new MindustryDockRegistry();

    private final DockEngine engine = new DockEngine();
    private final Seq<ElementDockTarget> hudTargets = new Seq<>();

    private MindustryDockRegistry() {
        registerScreenTargets();
        registerVanillaTargets();
    }

    public static MindustryDockRegistry get() {
        return INSTANCE;
    }

    public DockEngine engine() {
        return engine;
    }

    public void registerHud(String id, Element element) {
        unregisterHud(id);
        ElementDockTarget target = new ElementDockTarget(id, () -> element);
        hudTargets.add(target);
        engine.register(target);
    }

    public boolean unregisterHud(@Nullable String id) {
        if (id == null) {
            return false;
        }
        for (int i = hudTargets.size - 1; i >= 0; i--) {
            if (id.equals(hudTargets.get(i).id())) {
                hudTargets.remove(i);
            }
        }
        return engine.unregister(id);
    }

    public @Nullable DockTarget resolve(@Nullable String id) {
        return engine.find(id);
    }

    private void registerScreenTargets() {
        engine.register(new ScreenEdgeTarget(TARGET_SCREEN_BOTTOM, DockSide.TOP, false));
        engine.register(new ScreenEdgeTarget(TARGET_SCREEN_TOP, DockSide.BOTTOM, true));
        engine.register(new ScreenEdgeTarget(TARGET_SCREEN_LEFT, DockSide.RIGHT, false));
        engine.register(new ScreenEdgeTarget(TARGET_SCREEN_RIGHT, DockSide.LEFT, true));
        engine.register(new ScreenCornerTarget(TARGET_SCREEN_CORNER_BOTTOM_LEFT, DockSide.TOP_RIGHT, false, false));
        engine.register(new ScreenCornerTarget(TARGET_SCREEN_CORNER_BOTTOM_RIGHT, DockSide.TOP_LEFT, true, false));
        engine.register(new ScreenCornerTarget(TARGET_SCREEN_CORNER_TOP_LEFT, DockSide.BOTTOM_RIGHT, false, true));
        engine.register(new ScreenCornerTarget(TARGET_SCREEN_CORNER_TOP_RIGHT, DockSide.BOTTOM_LEFT, true, true));
    }

    private void registerVanillaTargets() {
        engine.register(new ElementDockTarget(TARGET_MINIMAP, () -> findHudElement(TARGET_MINIMAP)));
        engine.register(new ElementDockTarget(TARGET_WAVES, () -> findHudElement(TARGET_WAVES)));
        engine.register(new ElementDockTarget(TARGET_BLOCKS, this::resolveBlockSelect));
        engine.register(new ElementDockTarget(TARGET_CORE_ITEMS, () -> findHudElement(TARGET_CORE_ITEMS)));
    }

    private static @Nullable Element findHudElement(String lookupName) {
        return Vars.ui != null && Vars.ui.hudGroup != null ? Vars.ui.hudGroup.find(lookupName) : null;
    }

    private @Nullable Element resolveBlockSelect() {
        return Vars.ui != null && Vars.ui.hudGroup != null ? resolveBlockSelect(Vars.ui.hudGroup) : null;
    }

    static @Nullable Element resolveBlockSelect(Group hudGroup) {
        Element inputTable = hudGroup.findVisible("inputTable");
        Element blockSelect = inputTable != null ? inputTable.parent : null;
        return blockSelect != null ? blockSelect.parent : null;
    }

    private static float screenWidth() {
        if (Core.scene != null && Core.scene.getWidth() > 0f) {
            return Core.scene.getWidth();
        }
        return Units.screenWidth();
    }

    private static float screenHeight() {
        if (Core.scene != null && Core.scene.getHeight() > 0f) {
            return Core.scene.getHeight();
        }
        return Units.screenHeight();
    }

    private static final class ScreenEdgeTarget implements DockTarget {
        private final String id;
        private final DockSide side;
        private final boolean vertical;
        private final boolean high;

        private ScreenEdgeTarget(String id, DockSide side, boolean high) {
            this.id = id;
            this.side = side;
            this.vertical = side == DockSide.LEFT || side == DockSide.RIGHT;
            this.high = high;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float x() {
            return vertical && high ? screenWidth() : 0f;
        }

        @Override
        public float y() {
            return !vertical && high ? screenHeight() : 0f;
        }

        @Override
        public float width() {
            return vertical ? 0f : screenWidth();
        }

        @Override
        public float height() {
            return vertical ? screenHeight() : 0f;
        }

        @Override
        public boolean active() {
            return screenWidth() > 0f && screenHeight() > 0f;
        }

        @Override
        public boolean supports(DockSide other) {
            return other == side;
        }
    }

    private static final class ScreenCornerTarget implements DockTarget {
        private final String id;
        private final DockSide side;
        private final boolean right;
        private final boolean top;

        private ScreenCornerTarget(String id, DockSide side, boolean right, boolean top) {
            this.id = id;
            this.side = side;
            this.right = right;
            this.top = top;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float x() {
            return right ? screenWidth() : 0f;
        }

        @Override
        public float y() {
            return top ? screenHeight() : 0f;
        }

        @Override
        public float width() {
            return 0f;
        }

        @Override
        public float height() {
            return 0f;
        }

        @Override
        public boolean active() {
            return screenWidth() > 0f && screenHeight() > 0f;
        }

        @Override
        public boolean supports(DockSide other) {
            return other == side;
        }
    }

    private static final class ElementDockTarget implements DockTarget {
        private final String id;
        private final Prov<Element> resolver;
        private final Vec2 scratch = new Vec2();

        private ElementDockTarget(String id, Prov<Element> resolver) {
            this.id = id;
            this.resolver = resolver;
        }

        private @Nullable Element resolveElement() {
            return resolver.get();
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float x() {
            Element resolved = resolveElement();
            return resolved != null ? resolved.localToStageCoordinates(scratch.set(0f, 0f)).x : 0f;
        }

        @Override
        public float y() {
            Element resolved = resolveElement();
            return resolved != null ? resolved.localToStageCoordinates(scratch.set(0f, 0f)).y : 0f;
        }

        @Override
        public float width() {
            Element resolved = resolveElement();
            return resolved != null ? resolved.getWidth() : 0f;
        }

        @Override
        public float height() {
            Element resolved = resolveElement();
            return resolved != null ? resolved.getHeight() : 0f;
        }

        @Override
        public boolean active() {
            Element resolved = resolveElement();
            return resolved != null
                    && resolved.getScene() != null
                    && resolved.visible
                    && resolved.getWidth() > 0f
                    && resolved.getHeight() > 0f;
        }
    }
}
