package mindustrytool.presentation.builder;

import arc.scene.ui.layout.*;
import mindustry.Vars;

public class ServerLabelBuilder {
    public static void build(Stack st, String name, String val) {
        Table label = new Table().center();
        if (Vars.mobile || (name + " (" + val + ')').length() > 54) {
            label.add(name).pad(5, 5, 0, 5).expandX().row();
            label.add(" [lightgray](" + val + ')').pad(5, 0, 5, 5).expandX();
        } else label.add(name + " [lightgray](" + val + ')').pad(5).expandX();
        st.add(label);
    }
}
