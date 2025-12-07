package mindustrytool.presentation.builder;

import arc.scene.ui.layout.Table;
import mindustrytool.core.model.ServerHost;

public class ServerFieldBuilder {
    public static void build(Table cont, String[] edit, ServerHost temp) {
        cont.table(t -> {
            t.add("@message.manage-room.server-name").padRight(5f).right();
            t.field(edit[0], x -> edit[0] = x).size(320f, 54f).maxTextLength(100).left();
            t.row().add("@joingame.ip").padRight(5f).right();
            t.field(edit[1], x -> { edit[1] = x; temp.set(x); }).size(320f, 54f).valid(x -> temp.set(edit[1] = x)).maxTextLength(100).left();
            t.row().add();
            t.label(() -> temp.error).width(320f).left().row();
        }).row();
    }
}
