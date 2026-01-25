package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.dto.SchematicDetailData;
import mindustrytool.dto.SchematicDetailData.SchematicRequirement;
import mindustrytool.ui.DetailStats;
import mindustrytool.ui.TagContainer;
import mindustrytool.ui.UserCard;

import static mindustry.Vars.*;

import java.util.List;

public class SchematicInfoDialog extends BaseDialog {

    public SchematicInfoDialog() {
        super("");

        setFillParent(true);
        addCloseListener();
    }

    public void show(SchematicDetailData data) {
        cont.clear();
        cont.top().left();

        title.setText("[[" + Core.bundle.get("schematic") + "] " + data.getName());

        boolean portrait = Core.graphics.isPortrait();

        if (portrait) {
            cont.add(new SchematicImage(data.getId())).scaling(Scaling.fit)
                    .maxHeight(Core.graphics.getHeight() * 0.45f)
                    .growX()
                    .pad(10f)
                    .top()
                    .row();

            cont.pane(t -> buildDetails(t, data)).top().left().grow().scrollX(false).pad(10);
        } else {
            cont.table(main -> {
                main.top().left();

                var size = Math.max(Core.graphics.getHeight() * 0.5f, Core.graphics.getWidth() * 0.5f);

                main.add(new SchematicImage(data.getId())).scaling(Scaling.fit)
                        .height(size)
                        .width(size)
                        .pad(10f)
                        .top();

                main.pane(t -> buildDetails(t, data)).grow().scrollX(false).pad(10);
            }).grow();
        }

        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/schematics/" + data.getId())).pad(4);
        buttons.button("@back", Icon.left, this::hide);

        show();
    }

    private void buildDetails(Table card, SchematicDetailData data) {
        card.top().left().defaults().top().left();

        // Author
        card.table(t -> {
            t.left();
            t.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(t, data.getCreatedBy());
        }).fillX().padBottom(4).top().left().row();

        // Stats
        card.table(stats -> DetailStats.draw(stats, data.getLikes(), data.getComments(), data.getDownloads()))
                .fillX().padBottom(4).top().left().row();

        // Tags
        if (data.getTags() != null && data.getTags().size() > 0) {
            card.table(container -> TagContainer.draw(container, data.getTags()))
                    .fillX().padBottom(4).top().left().row();
        }

        // Requirements
        ItemSeq arr = toItemSeq(data.getMeta().getRequirements());
        card.table(r -> {
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
                }).padLeft(2).left().padRight(4).top().left();

                if (++i % 4 == 0) {
                    r.row();
                }
            }
        }).padBottom(10).top().left().row();

        // Description
        card.add(data.getDescription())
                .left()
                .wrap()
                .wrapLabel(true)
                .growX()
                .labelAlign(Align.topLeft)
                .top().left();
    }

    public ItemSeq toItemSeq(List<SchematicRequirement> requirement) {
        Seq<ItemStack> seq = new Seq<>();

        if (requirement == null) {
            return new ItemSeq(seq);
        }

        for (var req : requirement) {
            if (req.getName() == null)
                continue;

            var item = Vars.content.items().find(i -> i.name.toLowerCase().equals(req.getName().toLowerCase()));

            if (item != null) {
                seq.add(new ItemStack(item, req.getAmount()));
            }
        }

        return new ItemSeq(seq);
    }
}
