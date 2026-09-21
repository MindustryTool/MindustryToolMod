package solim.overlay;

import arc.Core;
import arc.scene.Scene;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;

public class PopupDebug {
    public static void main(String[] args) throws Exception {
        Core.app = new arc.mock.MockApplication();
        Core.graphics = new arc.mock.MockGraphics();
        Scene scene = new Scene();
        Core.scene = scene;
        Popup<String> menu = new Popup<>();
        menu.children(data -> new solim.core.BaseComponent() {
            @Override
            protected Element build() {
                Table content = new Table();
                content.add(new Table()).size(100f, 80f);
                return content;
            }
        });
        menu.show("data", 10f, 10f);
        Table t = menu.table();
        System.out.println("table w=" + t.getWidth() + " h=" + t.getHeight() + " pos=" + t.x + "," + t.y);
        System.out.println("cells=" + t.getCells().size);
        for (Cell<?> c : t.getCells()) {
            Element a = c.get();
            System.out.println("cell actor=" + a + " pos=" + a.x + "," + a.y + " " + a.getWidth() + "x" + a.getHeight());
        }
        Element hit = scene.root.hit(10f, 10f, true);
        System.out.println("hit=" + hit);
        menu.dispose();
    }
}
