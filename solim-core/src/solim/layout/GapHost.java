package solim.layout;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;

public interface GapHost {

    void setGap(float gap);

    static @Nullable GapHost find(@Nullable Table table) {
        if (table == null)
            return null;
        Component comp = SolimToken.getComponent(table);
        return comp instanceof GapHost ? (GapHost) comp : null;
    }
}
