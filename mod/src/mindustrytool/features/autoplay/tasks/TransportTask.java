package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class TransportTask implements AutoplayTask {

    public static final String ID = "transport";

    public enum SelectionMode {
        NONE,
        TARGET,
        DESTINATION
    }

    private final AutoplayFeature feature;
    private final ConfigValue<String> savedStateConfig;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final Signal<SelectionMode> selectionModeSignal = Signal.of(SelectionMode.NONE);
    private final Signal<Seq<TransportNode>> nodesSignal = Signal.of(new Seq<>());
    private final ObjectMap<Integer, TransportNode> nodeMap = new ObjectMap<>();
    private final TransportAI ai = new TransportAI();

    public TransportTask(AutoplayFeature feature) {
        this.feature = feature;
        this.savedStateConfig = feature.configGroup().stringValue("transport.saved-state", "");

        Events.on(TapEvent.class, this::handleTap);
        Events.on(WorldLoadEvent.class, e -> loadCurrentMapState());
        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                setSelectionMode(SelectionMode.NONE);
                clearNodesInMemory();
            }
        });
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.transport");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("upload.png", Icon.move);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    public Signal<SelectionMode> selectionMode() {
        return selectionModeSignal;
    }

    public Readable<Seq<TransportNode>> nodes() {
        return nodesSignal;
    }

    public void setSelectionMode(SelectionMode mode) {
        selectionModeSignal.set(mode);
    }

    public void toggleSelectionMode(SelectionMode mode) {
        if (selectionModeSignal.peek() == mode) {
            selectionModeSignal.set(SelectionMode.NONE);
        } else {
            selectionModeSignal.set(mode);
        }
    }

    public void clearAll() {
        clearNodesInMemory();
        persistState();
    }

    private void clearNodesInMemory() {
        nodeMap.clear();
        nodesSignal.set(new Seq<>());
    }

    public void removeNode(TransportNode node) {
        if (node == null) {
            return;
        }
        nodeMap.remove(node.pos);
        Seq<TransportNode> current = new Seq<>(nodesSignal.peek());
        current.remove(node);
        nodesSignal.set(current);
        persistState();
    }

    private void persistState() {
        String key = TransportStorageSerializer.computeMapKey();
        if (key.isEmpty()) {
            return;
        }
        Seq<TransportNode> current = nodesSignal.peek();
        if (current == null || current.isEmpty()) {
            savedStateConfig.set("");
            return;
        }
        String serialized = TransportStorageSerializer.serialize(key, current);
        savedStateConfig.set(serialized);
    }

    public void loadCurrentMapState() {
        setSelectionMode(SelectionMode.NONE);
        clearNodesInMemory();
        String currentKey = TransportStorageSerializer.computeMapKey();
        if (currentKey.isEmpty()) {
            return;
        }
        String raw = savedStateConfig.get();
        Seq<TransportNode> loaded = TransportStorageSerializer.deserialize(raw, currentKey);
        if (loaded != null && !loaded.isEmpty()) {
            for (TransportNode node : loaded) {
                nodeMap.put(node.pos, node);
            }
            nodesSignal.set(loaded);
        }
    }

    private void handleTap(TapEvent e) {
        SelectionMode mode = selectionModeSignal.peek();
        if (mode == SelectionMode.NONE || e.tile == null || Vars.player == null) {
            return;
        }
        Building build = e.tile.build;
        if (build == null || build.team != Vars.player.team() || build.block == null || !build.block.hasItems) {
            return;
        }

        int pos = build.pos();
        TransportNode node = nodeMap.get(pos);
        boolean isNew = false;
        if (node == null) {
            node = new TransportNode(build.tileX(), build.tileY());
            nodeMap.put(pos, node);
            isNew = true;
        }

        boolean isGeneral = TransportItemHelper.isGeneralStorage(build.block);
        if (mode == SelectionMode.TARGET) {
            node.isTarget.set(true);
            if (isNew || node.transferItems.peek() == null || node.transferItems.peek().isEmpty()) {
                if (isGeneral) {
                    node.transferItems.set(new Seq<>());
                    node.expanded.set(true);
                } else {
                    Seq<String> items = new Seq<>();
                    for (Item item : TransportItemHelper.validTransferItems(build.block)) {
                        items.add(item.name);
                    }
                    node.transferItems.set(items);
                    node.expanded.set(false);
                }
            }
        } else if (mode == SelectionMode.DESTINATION) {
            node.isDestination.set(true);
            if (isNew || node.receiveItems.peek() == null || node.receiveItems.peek().isEmpty()) {
                if (isGeneral) {
                    node.receiveItems.set(new Seq<>());
                    node.expanded.set(true);
                } else {
                    Seq<String> items = new Seq<>();
                    for (Item item : TransportItemHelper.validReceiveItems(build.block)) {
                        items.add(item.name);
                    }
                    node.receiveItems.set(items);
                    node.expanded.set(false);
                }
            }
        }

        if (isNew) {
            Seq<TransportNode> current = new Seq<>(nodesSignal.peek());
            current.add(node);
            nodesSignal.set(current);
        }

        persistState();
    }

    @Override
    public boolean update(Unit unit) {
        if (Core.input != null && (Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.back))) {
            setSelectionMode(SelectionMode.NONE);
        }

        if (unit == null || !unit.isValid() || unit.type == null || unit.type.itemCapacity <= 0) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-carry"));
            return false;
        }

        Seq<TransportNode> currentNodes = nodesSignal.peek();
        if (currentNodes == null || currentNodes.isEmpty()) {
            status.set(Core.bundle.get("feature.autoplay.status.no-transport-nodes"));
            return false;
        }

        Seq<TransportNode> validTargets = new Seq<>();
        Seq<TransportNode> validDests = new Seq<>();
        for (int i = 0; i < currentNodes.size; i++) {
            TransportNode n = currentNodes.get(i);
            Building b = n.building();
            if (b == null || !b.isValid() || b.dead() || b.team != unit.team) {
                continue;
            }
            if (n.isConfiguredTarget()) {
                validTargets.add(n);
            }
            if (n.isConfiguredDestination()) {
                validDests.add(n);
            }
        }

        if (unit.stack.amount > 0 && unit.stack.item != null) {
            Item held = unit.stack.item;
            Building bestDest = null;
            int lowestStock = Integer.MAX_VALUE;
            float closestDst = Float.MAX_VALUE;

            for (int i = 0; i < validDests.size; i++) {
                TransportNode n = validDests.get(i);
                if (!n.allowsReceive(held.name)) {
                    continue;
                }
                Building b = n.building();
                if (b == null || b.acceptStack(held, 1, unit) <= 0) {
                    continue;
                }
                int stock = b.items != null ? b.items.get(held) : 0;
                float dst = unit.dst2(b);
                if (stock < lowestStock || (stock == lowestStock && dst < closestDst)) {
                    lowestStock = stock;
                    closestDst = dst;
                    bestDest = b;
                }
            }

            if (bestDest != null) {
                ai.setTaskTarget(TransportAI.Action.DELIVER, bestDest, held, unit.stack.amount);
                status.set(Core.bundle.format("feature.autoplay.status.delivering", held.localizedName));
                return true;
            }

            Building core = unit.closestCore();
            if (core != null && core.acceptStack(held, 1, unit) > 0) {
                ai.setTaskTarget(TransportAI.Action.DEPOSIT_CORE, core, held, unit.stack.amount);
                status.set(Core.bundle.format("feature.autoplay.status.clearing-held", held.localizedName));
                return true;
            }

            ai.setTaskTarget(TransportAI.Action.CLEAR_HELD, null, held, unit.stack.amount);
            status.set(Core.bundle.get("feature.autoplay.status.dropping"));
            return true;
        }

        Building bestTarget = null;
        Building bestDest = null;
        Item bestItem = null;
        int lowestDestStock = Integer.MAX_VALUE;
        float bestDistance = Float.MAX_VALUE;

        if (Vars.content != null && Vars.content.items() != null) {
            for (Item item : Vars.content.items()) {
                if (item == null) {
                    continue;
                }
                for (int d = 0; d < validDests.size; d++) {
                    TransportNode destNode = validDests.get(d);
                    if (!destNode.allowsReceive(item.name)) {
                        continue;
                    }
                    Building destBuild = destNode.building();
                    if (destBuild == null || destBuild.acceptStack(item, 1, unit) <= 0) {
                        continue;
                    }

                    int destStock = destBuild.items != null ? destBuild.items.get(item) : 0;

                    for (int t = 0; t < validTargets.size; t++) {
                        TransportNode targetNode = validTargets.get(t);
                        if (targetNode.pos == destNode.pos || !targetNode.allowsTransfer(item.name)) {
                            continue;
                        }
                        Building targetBuild = targetNode.building();
                        if (targetBuild == null || targetBuild.items == null || targetBuild.items.get(item) <= 0) {
                            continue;
                        }

                        float combinedDst = unit.dst2(targetBuild) + targetBuild.dst2(destBuild);
                        if (destStock < lowestDestStock || (destStock == lowestDestStock && combinedDst < bestDistance)) {
                            lowestDestStock = destStock;
                            bestDistance = combinedDst;
                            bestTarget = targetBuild;
                            bestDest = destBuild;
                            bestItem = item;
                        }
                    }
                }
            }
        }

        if (bestTarget != null && bestDest != null && bestItem != null) {
            int targetAvail = bestTarget.items.get(bestItem);
            int destSpace = bestDest.acceptStack(bestItem, unit.type.itemCapacity, unit);
            int requestAmount = Math.max(1, Math.min(unit.type.itemCapacity, Math.min(targetAvail, destSpace)));

            ai.setTaskTarget(TransportAI.Action.PICKUP, bestTarget, bestItem, requestAmount);
            status.set(Core.bundle.format("feature.autoplay.status.picking-up", bestItem.localizedName));
            return true;
        }

        status.set(Core.bundle.get("feature.autoplay.status.waiting-transport"));
        return false;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().growX().gap(unit(1.5f)).children(() -> {
            text(Core.bundle.get("feature.autoplay.settings.transport.description"))
                    .growX().left().wrap(true).color(WebStyles.Colors.GHOST_FG);

            row().growX().gap(unit(1)).children(() -> {
                Readable<Boolean> targetArmed = selectionModeSignal.map(m -> m == SelectionMode.TARGET);
                Readable<Boolean> destArmed = selectionModeSignal.map(m -> m == SelectionMode.DESTINATION);

                button(() -> toggleSelectionMode(SelectionMode.TARGET))
                        .style(WebStyles.secondary())
                        .checked(targetArmed)
                        .height(unit(9))
                        .growX()
                        .children(() -> {
                            icon(Icon.upload).size(unit(4));
                            text(Core.bundle.get("feature.autoplay.settings.transport.select-targets"));
                        });

                button(() -> toggleSelectionMode(SelectionMode.DESTINATION))
                        .style(WebStyles.secondary())
                        .checked(destArmed)
                        .height(unit(9))
                        .growX()
                        .children(() -> {
                            icon(Icon.download).size(unit(4));
                            text(Core.bundle.get("feature.autoplay.settings.transport.select-dests"));
                        });

                button(this::clearAll)
                        .style(WebStyles.secondary())
                        .height(unit(9))
                        .growX()
                        .children(() -> {
                            icon(Icon.trash).size(unit(4));
                            text(Core.bundle.get("feature.autoplay.settings.transport.clear-all"));
                        });
            });
        });
    }
}
