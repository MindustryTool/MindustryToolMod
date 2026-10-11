package mindustrytool.features.autoplay.tasks;

import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Building;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Reactive data model for a player-configured transport node (Target source or Destination sink).
 */
public class TransportNode {

    public final int pos;
    public final int x;
    public final int y;

    public final Signal<Boolean> isTarget = Signal.of(false);
    public final Signal<Boolean> isDestination = Signal.of(false);
    public final Signal<Seq<String>> transferItems = Signal.of(new Seq<>());
    public final Signal<Seq<String>> receiveItems = Signal.of(new Seq<>());
    public final Signal<Boolean> expanded = Signal.of(false);

    public TransportNode(int x, int y) {
        this.x = x;
        this.y = y;
        this.pos = (x & 0xFFFF) | ((y & 0xFFFF) << 16);
    }

    public TransportNode(int pos) {
        this.pos = pos;
        this.x = (short) (pos & 0xFFFF);
        this.y = (short) ((pos >>> 16) & 0xFFFF);
    }

    public @Nullable Building building() {
        if (Vars.world == null) {
            return null;
        }
        return Vars.world.build(x, y);
    }

    public boolean isConfiguredTarget() {
        return Boolean.TRUE.equals(isTarget.peek());
    }

    public boolean isConfiguredDestination() {
        return Boolean.TRUE.equals(isDestination.peek());
    }

    public boolean allowsTransfer(String itemName) {
        if (!isConfiguredTarget()) {
            return false;
        }
        Seq<String> items = transferItems.peek();
        return items != null && items.contains(itemName);
    }

    public boolean allowsReceive(String itemName) {
        if (!isConfiguredDestination()) {
            return false;
        }
        Seq<String> items = receiveItems.peek();
        return items != null && items.contains(itemName);
    }

    public void toggleTransferItem(String itemName, boolean selected) {
        Seq<String> current = new Seq<>();
        Seq<String> stored = transferItems.peek();
        if (stored != null) {
            current.addAll(stored);
        }
        if (selected) {
            if (!current.contains(itemName)) {
                current.add(itemName);
            }
        } else {
            current.remove(itemName);
        }
        transferItems.set(current);
    }

    public void toggleReceiveItem(String itemName, boolean selected) {
        Seq<String> current = new Seq<>();
        Seq<String> stored = receiveItems.peek();
        if (stored != null) {
            current.addAll(stored);
        }
        if (selected) {
            if (!current.contains(itemName)) {
                current.add(itemName);
            }
        } else {
            current.remove(itemName);
        }
        receiveItems.set(current);
    }
}
