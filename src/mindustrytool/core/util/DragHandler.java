package mindustrytool.core.util;

import arc.input.KeyCode;
import arc.scene.event.InputListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Table;

/** Adds drag functionality to a table. */
public class DragHandler extends InputListener {
    private final Table target;
    private boolean isDragging = false;
    private float dragStartX, dragStartY;

    public DragHandler(Table target) { this.target = target; }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
        if (button == KeyCode.mouseLeft) { isDragging = true; dragStartX = x; dragStartY = y; return true; }
        return false;
    }

    @Override
    public void touchDragged(InputEvent event, float x, float y, int pointer) {
        if (isDragging) target.setPosition(event.stageX - dragStartX, event.stageY - dragStartY);
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) { isDragging = false; }
}
