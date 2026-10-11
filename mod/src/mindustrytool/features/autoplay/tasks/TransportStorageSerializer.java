package mindustrytool.features.autoplay.tasks;

import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;

/**
 * Handles single-slot map-keyed persistence for transport nodes.
 * Format:
 * line 0: mapKey
 * line 1..N: x,y,targetFlag,destFlag,expandedFlag,transferItemsJoined,receiveItemsJoined
 */
public final class TransportStorageSerializer {

    private TransportStorageSerializer() {
    }

    public static String computeMapKey() {
        if (Vars.state == null || Vars.state.map == null || Vars.world == null) {
            return "";
        }
        String mapName = Vars.state.map.name() != null ? Vars.state.map.name() : "unknown";
        int w = Vars.world.width();
        int h = Vars.world.height();
        int sectorId = -1;
        if (Vars.state.rules != null && Vars.state.rules.sector != null) {
            sectorId = Vars.state.rules.sector.id;
        }
        return mapName + ":" + w + "x" + h + ":" + sectorId;
    }

    public static String serialize(String mapKey, Seq<TransportNode> nodes) {
        StringBuilder sb = new StringBuilder();
        sb.append(mapKey).append("\n");
        for (int i = 0; i < nodes.size; i++) {
            TransportNode node = nodes.get(i);
            sb.append(node.x).append(",")
              .append(node.y).append(",")
              .append(node.isConfiguredTarget() ? "1" : "0").append(",")
              .append(node.isConfiguredDestination() ? "1" : "0").append(",")
              .append(Boolean.TRUE.equals(node.expanded.peek()) ? "1" : "0").append(",");

            Seq<String> tItems = node.transferItems.peek();
            if (tItems != null && !tItems.isEmpty()) {
                sb.append(tItems.toString("|"));
            }
            sb.append(",");

            Seq<String> rItems = node.receiveItems.peek();
            if (rItems != null && !rItems.isEmpty()) {
                sb.append(rItems.toString("|"));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static @Nullable Seq<TransportNode> deserialize(String data, String expectedMapKey) {
        if (data == null || data.trim().isEmpty() || expectedMapKey == null || expectedMapKey.isEmpty()) {
            return null;
        }
        String[] lines = data.split("\n");
        if (lines.length == 0) {
            return null;
        }
        String storedKey = lines[0].trim();
        if (!expectedMapKey.equals(storedKey)) {
            return null;
        }

        Seq<TransportNode> nodes = new Seq<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 5) {
                continue;
            }
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                boolean isTarget = "1".equals(parts[2].trim());
                boolean isDest = "1".equals(parts[3].trim());
                boolean expanded = "1".equals(parts[4].trim());

                TransportNode node = new TransportNode(x, y);
                node.isTarget.set(isTarget);
                node.isDestination.set(isDest);
                node.expanded.set(expanded);

                if (parts.length > 5 && !parts[5].trim().isEmpty()) {
                    Seq<String> tList = new Seq<>();
                    for (String item : parts[5].trim().split("\\|")) {
                        if (!item.isEmpty()) {
                            tList.add(item);
                        }
                    }
                    node.transferItems.set(tList);
                }

                if (parts.length > 6 && !parts[6].trim().isEmpty()) {
                    Seq<String> rList = new Seq<>();
                    for (String item : parts[6].trim().split("\\|")) {
                        if (!item.isEmpty()) {
                            rList.add(item);
                        }
                    }
                    node.receiveItems.set(rList);
                }

                nodes.add(node);
            } catch (Throwable ignored) {
            }
        }
        return nodes;
    }
}
