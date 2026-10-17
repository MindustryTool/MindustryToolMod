package solim.modifier;

import arc.util.Nullable;
import solim.core.HostCapability;

public interface ContainerCapability<SELF extends ContainerCapability<SELF>> extends HostCapability {
    SELF children(@Nullable Runnable block);
}