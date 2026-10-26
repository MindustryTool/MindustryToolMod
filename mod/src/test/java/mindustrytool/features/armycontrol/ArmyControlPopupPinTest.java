package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Scene;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.type.UnitType;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.Test;
import solim.overlay.Popup;
import solim.test.SolimEnv;

class ArmyControlPopupPinTest extends MindustryTestEnv {

    @Test
    void barInUpperHalf_pinsTop() {
        Scene scene = new Scene();
        scene.resize(800, 600);
        Core.scene = scene;
        Table bar = new Table();
        bar.setBounds(100f, 500f, 200f, 40f);
        assertEquals(Popup.Edge.TOP, ArmyControlPopup.pinnedEdgeFor(bar));
    }

    @Test
    void barInLowerHalf_pinsBottom() {
        Scene scene = new Scene();
        scene.resize(800, 600);
        Core.scene = scene;
        Table bar = new Table();
        bar.setBounds(100f, 50f, 200f, 40f);
        assertEquals(Popup.Edge.BOTTOM, ArmyControlPopup.pinnedEdgeFor(bar));
    }

    @Test
    void nullBar_pinsBottomAsLegacy() {
        assertEquals(Popup.Edge.BOTTOM, ArmyControlPopup.pinnedEdgeFor(null));
    }

    @Test
    void popupRemainsAboveBarWhenManyUnitsDiscovered() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        Scene scene = new Scene();
        scene.resize(800, 600);
        Core.scene = scene;
        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        Font font = new Font(fontData, new TextureRegion(), false);
        Core.scene.addStyle(LabelStyle.class, new LabelStyle(font, null));
        Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());

        Table bar = new Table();
        bar.setBounds(100f, 50f, 200f, 40f);
        scene.root.addChild(bar);

        ArmyControlFeature feature = new ArmyControlFeature();
        ArmyControlPopup.show(feature, bar);

        float barTop = bar.y + bar.getHeight();
        Element popupTable = scene.root.find("solim-popup-menu");
        assertNotNull(popupTable);
        assertTrue(popupTable.y >= barTop);
        float bottomBefore = popupTable.y;

        ObjectIntMap<UnitType> types = new ObjectIntMap<>();
        for (int i = 0; i < 30; i++) {
            UnitType ut = Vars.content.unit("pin-unit-" + i);
            types.put(ut != null ? ut : new UnitType("pin-unit-" + i), 5);
        }
        feature.discoveredTypesSignal().set(types);
        SolimEnv.flushEffects();
        popupTable.validate();

        assertEquals(bottomBefore, popupTable.y, 0.5f, "Bottom edge must remain fixed when pinned to BOTTOM");
        assertTrue(popupTable.y >= barTop);
        assertTrue(popupTable.y + popupTable.getHeight() <= 600f);

        ObjectIntMap<UnitType> empty = new ObjectIntMap<>();
        feature.discoveredTypesSignal().set(empty);
        SolimEnv.flushEffects();
        popupTable.validate();

        assertEquals(bottomBefore, popupTable.y, 0.5f, "Bottom edge must remain fixed when shrinking");
        ArmyControlPopup.hide();
    }

    @Test
    void popupRemainsBelowBarWhenInUpperHalfAndManyUnitsDiscovered() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        Scene scene = new Scene();
        scene.resize(800, 600);
        Core.scene = scene;
        FontData fontData = new FontData() {
            @Override
            public boolean hasGlyph(char ch) {
                return true;
            }
        };
        Font font = new Font(fontData, new TextureRegion(), false);
        Core.scene.addStyle(LabelStyle.class, new LabelStyle(font, null));
        Core.scene.addStyle(ButtonStyle.class, new ButtonStyle());

        Table bar = new Table();
        bar.setBounds(100f, 500f, 200f, 40f);
        scene.root.addChild(bar);

        ArmyControlFeature feature = new ArmyControlFeature();
        ArmyControlPopup.show(feature, bar);

        Element popupTable = scene.root.find("solim-popup-menu");
        assertNotNull(popupTable);
        assertTrue(popupTable.y + popupTable.getHeight() <= bar.y);
        float topBefore = popupTable.y + popupTable.getHeight();

        ObjectIntMap<UnitType> types = new ObjectIntMap<>();
        for (int i = 0; i < 30; i++) {
            UnitType ut = Vars.content.unit("pin-unit-top-" + i);
            types.put(ut != null ? ut : new UnitType("pin-unit-top-" + i), 5);
        }
        feature.discoveredTypesSignal().set(types);
        SolimEnv.flushEffects();
        popupTable.validate();

        float topAfter = popupTable.y + popupTable.getHeight();
        assertEquals(topBefore, topAfter, 0.5f, "Top edge must remain fixed when pinned to TOP");

        ObjectIntMap<UnitType> empty = new ObjectIntMap<>();
        feature.discoveredTypesSignal().set(empty);
        SolimEnv.flushEffects();
        popupTable.validate();

        float topShrunk = popupTable.y + popupTable.getHeight();
        assertEquals(topBefore, topShrunk, 0.5f, "Top edge must remain fixed when shrinking");

        assertTrue(popupTable.y + popupTable.getHeight() <= bar.y);
        assertTrue(popupTable.y >= 0f);
        ArmyControlPopup.hide();
    }
}
