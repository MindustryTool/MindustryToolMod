package mindustrytool.gui;

import arc.Core;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.config.Config;
import mindustrytool.data.SchematicDetailData;
import mindustrytool.data.SchematicDetailData.SchematicRequirement;
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

        cont.add(new ImageHandler(data.id(), ImageHandler.ImageType.SCHEMATIC)).maxWidth(Core.graphics.getWidth() * 2 / 3).row();

        cont.table(card -> {
            card.left();
            card.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(card, data.createdBy());
        })//
                .fillX()//
                .left();

        cont.row();
        cont.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads()))// 
                .fillX()//
                .left();
        cont.row();
        cont.table(container -> TagContainer.draw(container, data.tags()))//
                .fillX()//
                .left()//
                .row();
        cont.row();
        ItemSeq arr = toItemSeq(data.meta().requirements());
        cont.table(r -> {
            int i = 0;
            for (ItemStack s : arr) {
                r.image(s.item.uiIcon).left().size(iconMed);
                r.label(() -> {
                    Building core = player.core();
                    if (core == null || state.isMenu() || state.rules.infiniteResources
                            || core.items.has(s.item, s.amount))
                        return "[lightgray]" + s.amount;

                    return (core.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]")
                            + Math.min(core.items.get(s.item), s.amount) + "[lightgray]/" + s.amount;
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
        
        buttons.button("@back", Icon.left, this::hide).pad(4);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/schematics/" + data.id()));
        show();
    }

    public ItemSeq toItemSeq(Seq<SchematicRequirement> requirement) {
        Seq<ItemStack> seq = new Seq<>();

        if (requirement == null) {
            return new ItemSeq(seq);
        }

        for (var req : requirement) {
            if (req.name() == null)
                continue;

            var item = Vars.content.items().find(i -> i.name.toLowerCase().equals(req.name().toLowerCase()));

            if (item != null) {
                seq.add(new ItemStack(item, req.amount()));
            }
        }

        return new ItemSeq(seq);
    }
}
