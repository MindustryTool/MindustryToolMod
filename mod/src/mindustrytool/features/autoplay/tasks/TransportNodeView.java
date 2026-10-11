package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class TransportNodeView extends BaseComponent {

    private final TransportTask task;
    private final TransportNode node;
    private final Vec2 screenPos = new Vec2();
    private @Nullable Table rootTable;

    public TransportNodeView(TransportTask task, TransportNode node) {
        this.task = task;
        this.node = node;
    }

    @Override
    protected Element build() {
        Table table = new Table();
        this.rootTable = table;
        table.touchable = Touchable.childrenOnly;

        table.visible(() -> {
            if (Vars.renderer == null || Vars.renderer.getScale() < 0.5f) {
                return false;
            }
            if (Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
                return false;
            }
            if (Vars.state == null || !Vars.state.isGame()) {
                return false;
            }
            Building b = node.building();
            return b != null && b.isValid() && !b.dead();
        });

        table.update(() -> {
            Building b = node.building();
            if (b == null || !b.isValid() || Core.camera == null) {
                return;
            }
            float worldX = b.x;
            float worldY = b.y + (b.block.size * Vars.tilesize / 2f);
            screenPos.set(Core.camera.project(worldX, worldY));
            table.setPosition(screenPos.x, screenPos.y, Align.bottom | Align.center);
        });

        Element content = card()
                .padding(unit(1))
                .gap(unit(1))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.BORDER)
                .children(() -> {
                    column().gap(unit(1)).center().children(() -> {
                        when(node.isTarget).thenDo(() -> roleRow(true));
                        when(node.isDestination).thenDo(() -> roleRow(false));
                        when(node.expanded).thenDo(this::itemPicker);
                    });
                }).element();

        table.add(content);
        table.pack();
        return table;
    }

    private Component roleRow(boolean isTarget) {
        Signal<Seq<String>> itemsSignal = isTarget ? node.transferItems : node.receiveItems;
        String titleKey = isTarget ? "feature.autoplay.transport.transfer" : "feature.autoplay.transport.receive";
        TextureRegionDrawable roleIcon = isTarget ? new TextureRegionDrawable(Icon.upload.getRegion()) : new TextureRegionDrawable(Icon.download.getRegion());

        return row().gap(unit(1)).center().children(() -> {
            icon(roleIcon).size(unit(4)).color(isTarget ? Color.gold : Color.cyan);
            text(Core.bundle.get(titleKey)).color(WebStyles.Colors.GHOST_FG);

            reactiveGrid(itemsSignal)
                    .columns(4)
                    .gap(unit(0.5f))
                    .children(name -> {
                        Item item = Vars.content != null ? Vars.content.item(name) : null;
                        if (item == null) {
                            return row();
                        }
                        return icon(new TextureRegionDrawable(item.uiIcon)).size(unit(4));
                    });

            button(() -> node.expanded.set(!Boolean.TRUE.equals(node.expanded.peek())))
                    .style(WebStyles.ghost())
                    .size(unit(6))
                    .tooltip(Core.bundle.get("feature.autoplay.tooltip.configure"))
                    .children(() -> icon(FileIcon.of("wrench.png", Icon.wrench)).size(unit(4)));

            button(() -> {
                if (isTarget) {
                    node.isTarget.set(false);
                } else {
                    node.isDestination.set(false);
                }
                if (!node.isConfiguredTarget() && !node.isConfiguredDestination()) {
                    task.removeNode(node);
                }
            })
                    .style(WebStyles.ghost())
                    .size(unit(6))
                    .tooltip(Core.bundle.get("feature.autoplay.transport.remove"))
                    .children(() -> icon(Icon.cancel).size(unit(4)).color(Color.scarlet));
        });
    }

    private Component itemPicker() {
        Building b = node.building();
        if (b == null) {
            return row();
        }

        return column().gap(unit(1)).center().children(() -> {
            divider().color(WebStyles.Colors.SECTION_BORDER);

            if (node.isConfiguredTarget()) {
                text(Core.bundle.get("feature.autoplay.transport.transfer-items")).color(WebStyles.Colors.GHOST_FG);
                wrap().gap(unit(0.5f)).children(() -> {
                    for (Item item : TransportItemHelper.validTransferItems(b.block)) {
                        Readable<Boolean> checked = node.transferItems.map(seq -> seq != null && seq.contains(item.name));
                        button(() -> node.toggleTransferItem(item.name, !Boolean.TRUE.equals(checked.peek())))
                                .style(WebStyles.filterChipText())
                                .checked(checked)
                                .paddingX(unit(1.5f))
                                .height(unit(6))
                                .children(() -> {
                                    icon(new TextureRegionDrawable(item.uiIcon)).size(unit(3.5f));
                                    text(item.localizedName).color(checked.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray));
                                });
                    }
                });
            }

            if (node.isConfiguredDestination()) {
                text(Core.bundle.get("feature.autoplay.transport.receive-items")).color(WebStyles.Colors.GHOST_FG);
                wrap().gap(unit(0.5f)).children(() -> {
                    for (Item item : TransportItemHelper.validReceiveItems(b.block)) {
                        Readable<Boolean> checked = node.receiveItems.map(seq -> seq != null && seq.contains(item.name));
                        button(() -> node.toggleReceiveItem(item.name, !Boolean.TRUE.equals(checked.peek())))
                                .style(WebStyles.filterChipText())
                                .checked(checked)
                                .paddingX(unit(1.5f))
                                .height(unit(6))
                                .children(() -> {
                                    icon(new TextureRegionDrawable(item.uiIcon)).size(unit(3.5f));
                                    text(item.localizedName).color(checked.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray));
                                });
                    }
                });
            }
        });
    }
}
