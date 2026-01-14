package mindustrytool.playerconnect.gui;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
// Import PlayerConnect từ net package
import mindustrytool.playerconnect.net.PlayerConnect;

/**
 * UI for a single server row (button + label + ping display).
 * Extracted to reduce duplication and allow reuse.
 */
public final class ServerRow {
    private ServerRow(){}

    public static void addTo(Table table, Server server, ServerListPanel panel){
        Button button = new Button();
        button.getStyle().checkedOver = button.getStyle().checked = button.getStyle().over;
        button.setProgrammaticChangeEvents(true);
        button.clicked(() -> panel.setSelection(server, button));

        table.add(button).checked(b -> panel.getSelectedButton() == b).growX().padTop(5).padBottom(5).row();

        Stack stack = new Stack();
        Table inner = new Table();
        inner.setColor(Pal.gray);
        Draw.reset();

        button.clearChildren();
        button.add(stack).growX().row();

        Table ping = inner.table(t -> {})
                .margin(0)
                .pad(0)
                .left()
                .fillX()
                .get();

        inner.add().expandX();
        Table label = new Table().center();
        label.add(server.name).pad(5,5,0,5).expandX().row();
        stack.add(label);
        stack.add(inner);

        ping.label(() -> Strings.animated(Time.time, 4, 11, "."))
                .pad(2)
                .color(Pal.accent)
                .left();

        PlayerConnect.pingHost(server.ip, server.port, ms -> {
            ping.clear();
            ping.image(Icon.ok).color(Color.green).padLeft(5).padRight(5).left();
            if (Vars.mobile) {
                ping.row().add(ms + "ms").color(Color.lightGray).padLeft(5).padRight(5).left();
            } else {
                ping.add(ms + "ms").color(Color.lightGray).padRight(5).left();
            }
        }, e -> {
            ping.clear();
            ping.image(Icon.cancel).color(Color.red).padLeft(5).padRight(5).left();
        });
    }
}
