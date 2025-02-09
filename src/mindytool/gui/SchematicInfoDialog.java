package mindytool.gui;

import mindytool.config.Config;
import mindytool.data.SchematicDetailData;
import mindytool.data.SchematicDetailData.SchematicRequirement;
import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

public class SchematicInfoDialog extends BaseDialog {

    public SchematicInfoDialog() {
        super("");

        setFillParent(true);
        addCloseListener();
    }

    public void show(SchematicDetailData data) {
        cont.clear();

        title.setText("[[" + Core.bundle.get("schematic") + "] " + data.name());

        cont.add(new SchematicImage(data.id())).maxSize(800).row();

        cont.table(card -> {
            card.left();
            card.add(Core.bundle.format("@author")).marginRight(4).padRight(4);
            UserCard.draw(card, data.userId());
        })//
                .fillX()//
                .left();

        cont.row();
        cont.table(stats -> DetailStats.draw(stats, data.likes(), data.dislikes(), data.downloadCount()))//
                .fillX()//
                .left();
        cont.row();
        cont.table(container -> TagContainer.draw(container, data.tags()))//
                .fillX()//
                .left()//
                .row();
        cont.row();
        ItemSeq arr = toItemSeq(data.metadata().requirements());
        cont.table(r -> {
            int i = 0;
            for (ItemStack s : arr) {
                r.image(s.item.uiIcon).left().size(iconMed);
                r.label(() -> {
                    Building core = player.core();
                    if (core == null || state.isMenu() || state.rules.infiniteResources || core.items.has(s.item, s.amount))
                        return "[lightgray]" + s.amount;

                    return (core.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]") + Math.min(core.items.get(s.item), s.amount) + "[lightgray]/" + s.amount;
                }).padLeft(2).left().padRight(4);

                if (++i % 4 == 0) {
                    r.row();
                }
            }
        });
        cont.row();
        cont.add(data.description())//
                .left()//
                .wrap()//
                .wrapLabel(true)//
                .fillX();

        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/schematics/" + data.id())).pad(4);
        buttons.button("@back", Icon.left, this::hide);
        // buttons.button("@editor.export", Icon.upload, () -> showExport(schem));
        // buttons.button("@edit", Icon.edit, () -> showEdit(schem));

        show();
    }

    public ItemSeq toItemSeq(Seq<SchematicRequirement> requirement) {
        Seq<ItemStack> seq = new Seq<>();

        if (requirement == null) {
            return new ItemSeq(seq);
        }

        for (var req : requirement) {
            var item = Vars.content.items().find(i -> i.name.toLowerCase().equals(req.name().toLowerCase()));

            if (item != null) {
                seq.add(new ItemStack(item, req.amount()));
            }
        }

        return new ItemSeq(seq);
    }
}
