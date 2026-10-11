package mindustrytool.features.armycontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import arc.Core;
import arc.scene.Scene;
import arc.scene.ui.layout.Table;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.Test;
import solim.overlay.Popup;

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
}
