package mindustrytool.features.quickaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QuickAccessPopupAlignTest {

    @Test
    void leftHalf_alignsLeftEdge() {
        float desired = QuickAccessPopupHelper.computeDesiredX(100f, 200f, 300f, 800f);
        assertEquals(100f, desired, 0.001f);
    }

    @Test
    void rightHalf_alignsRightEdge() {
        float desired = QuickAccessPopupHelper.computeDesiredX(500f, 200f, 300f, 800f);
        assertEquals(400f, desired, 0.001f);
    }

    @Test
    void midpoint_deterministicRightAlign() {
        float desired = QuickAccessPopupHelper.computeDesiredX(300f, 200f, 300f, 800f);
        assertEquals(200f, desired, 0.001f);
    }

    @Test
    void overflowRight_clampsToStage() {
        float desired = QuickAccessPopupHelper.computeDesiredX(750f, 100f, 200f, 800f);
        assertEquals(650f, desired, 0.001f);
        assertEquals(600f, QuickAccessPopupHelper.clampX(desired, 200f, 800f), 0.001f);
    }

    @Test
    void popupWiderThanStage_clampsToZero() {
        float desired = QuickAccessPopupHelper.computeDesiredX(0f, 100f, 500f, 400f);
        assertEquals(0f, desired, 0.001f);
        assertEquals(0f, QuickAccessPopupHelper.clampX(desired, 500f, 400f), 0.001f);
    }
}
