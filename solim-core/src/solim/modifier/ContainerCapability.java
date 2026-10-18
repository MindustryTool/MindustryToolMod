package solim.modifier;

import arc.util.Nullable;

public interface ContainerCapability<SELF extends ContainerCapability<SELF>> extends ElementCapability {
    SELF children(@Nullable Runnable block);
}