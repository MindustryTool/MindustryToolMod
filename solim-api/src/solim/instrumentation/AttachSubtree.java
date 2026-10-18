package solim.instrumentation;

import arc.util.Nullable;

public final class AttachSubtree implements InstrumentationEvent {

    public final boolean begin;
    public final @Nullable String owner;
    public final int depth;
    public final int childCount;
    public final long timestampNs;

    private AttachSubtree(boolean begin, @Nullable String owner, int depth, int childCount, long timestampNs) {
        this.begin = begin;
        this.owner = owner;
        this.depth = depth < 0 ? 0 : depth;
        this.childCount = childCount;
        this.timestampNs = timestampNs;
    }

    public static AttachSubtree opened(@Nullable String owner, int depth, long timestampNs) {
        return new AttachSubtree(true, owner, depth, -1, timestampNs);
    }

    public static AttachSubtree closed(@Nullable String owner, int depth, int childCount, long timestampNs) {
        return new AttachSubtree(false, owner, depth, childCount, timestampNs);
    }
}