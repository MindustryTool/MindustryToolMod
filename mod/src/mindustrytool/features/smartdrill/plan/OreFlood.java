package mindustrytool.features.smartdrill.plan;

import arc.struct.Seq;

public final class OreFlood {

    public static final class Result {
        public final Rect rect;
        public final int tiles;
        public final boolean capped;

        public Result(Rect rect, int tiles, boolean capped) {
            this.rect = rect;
            this.tiles = tiles;
            this.capped = capped;
        }
    }

    private OreFlood() {
    }

    public static Result flood(DrillWorldView world, int startX, int startY, OreKind kind, String ore,
            int maxTiles, int maxSize) {
        TileInfo start = world.tile(startX, startY);
        if (start == null || !ore.equals(start.ore(kind))) {
            return new Result(new Rect(startX, startY, 1, 1), 0, false);
        }

        boolean[] visited = new boolean[world.width() * world.height()];
        Seq<Integer> queueX = new Seq<>();
        Seq<Integer> queueY = new Seq<>();
        queueX.add(startX);
        queueY.add(startY);
        visited[index(world, startX, startY)] = true;

        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        int count = 0;
        boolean capped = false;

        int head = 0;
        while (head < queueX.size) {
            int x = queueX.get(head);
            int y = queueY.get(head);
            head++;

            count++;
            if (count > maxTiles) {
                capped = true;
                break;
            }

            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }

            visitNeighbor(world, kind, ore, visited, queueX, queueY, x + 1, y);
            visitNeighbor(world, kind, ore, visited, queueX, queueY, x - 1, y);
            visitNeighbor(world, kind, ore, visited, queueX, queueY, x, y + 1);
            visitNeighbor(world, kind, ore, visited, queueX, queueY, x, y - 1);
        }

        Rect raw = new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
        return new Result(raw.clampTo(maxSize, maxSize), count, capped);
    }

    public static Result floodDiagonal(DrillWorldView world, int startX, int startY, OreKind kind, String ore,
            int maxTiles, int maxSize) {
        TileInfo start = world.tile(startX, startY);
        if (start == null || !ore.equals(start.ore(kind))) {
            return new Result(new Rect(startX, startY, 1, 1), 0, false);
        }

        boolean[] visited = new boolean[world.width() * world.height()];
        Seq<Integer> queueX = new Seq<>();
        Seq<Integer> queueY = new Seq<>();
        queueX.add(startX);
        queueY.add(startY);
        visited[index(world, startX, startY)] = true;

        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;
        int count = 0;
        boolean capped = false;

        int head = 0;
        while (head < queueX.size) {
            int x = queueX.get(head);
            int y = queueY.get(head);
            head++;

            count++;
            if (count > maxTiles) {
                capped = true;
                break;
            }

            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    visitNeighbor(world, kind, ore, visited, queueX, queueY, x + dx, y + dy);
                }
            }
        }

        Rect raw = new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
        return new Result(raw.clampTo(maxSize, maxSize), count, capped);
    }

    private static void visitNeighbor(DrillWorldView world, OreKind kind, String ore, boolean[] visited,
            Seq<Integer> queueX, Seq<Integer> queueY, int x, int y) {
        if (x < 0 || y < 0 || x >= world.width() || y >= world.height()) {
            return;
        }
        int index = index(world, x, y);
        if (visited[index]) {
            return;
        }
        TileInfo info = world.tile(x, y);
        if (info == null || !ore.equals(info.ore(kind))) {
            return;
        }
        visited[index] = true;
        queueX.add(x);
        queueY.add(y);
    }

    private static int index(DrillWorldView world, int x, int y) {
        return y * world.width() + x;
    }
}