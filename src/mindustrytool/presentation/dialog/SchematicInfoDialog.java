package mindustrytool.presentation.dialog;

import arc.Core;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.SchematicDetailData;
import mindustrytool.presentation.component.*;
import mindustrytool.presentation.builder.ImageHandler;
import mindustrytool.domain.service.RequirementConverter;
import static mindustry.Vars.*;

public class SchematicInfoDialog extends BaseDialog {
    public SchematicInfoDialog() { super(""); setFillParent(true); addCloseListener(); }

    public void show(SchematicDetailData data) {
        cont.clear();
        title.setText("[[" + Core.bundle.get("schematic") + "] " + data.name());
        cont.add(new ImageHandler(data.id(), ImageHandler.ImageType.SCHEMATIC)).maxWidth(Core.graphics.getWidth() * 2 / 3).row();
        cont.table(card -> { card.left(); card.add(Core.bundle.format("message.author")).marginRight(4).padRight(4); UserCard.draw(card, data.createdBy()); }).fillX().left();
        cont.row();
        cont.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads())).fillX().left();
        cont.row();
        cont.table(container -> TagContainer.draw(container, data.tags())).fillX().left().row();
        cont.row();
        ItemSeq arr = RequirementConverter.toItemSeq(data.meta().requirements());
        cont.table(r -> {
            int i = 0;
            for (ItemStack s : arr) {
                r.image(s.item.uiIcon).left().size(iconMed);
                r.label(() -> { Building core = player.core(); if (core == null || state.isMenu() || state.rules.infiniteResources || core.items.has(s.item, s.amount)) return "[lightgray]" + s.amount; return (core.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]") + Math.min(core.items.get(s.item), s.amount) + "[lightgray]/" + s.amount; }).padLeft(2).left().padRight(4);
                if (++i % 4 == 0) r.row();
            }
        });
        cont.row();
        cont.add(data.description()).left().wrap().wrapLabel(true).fillX();
        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@back", Icon.left, this::hide).pad(4);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/schematics/" + data.id()));
        show();
    }
}
