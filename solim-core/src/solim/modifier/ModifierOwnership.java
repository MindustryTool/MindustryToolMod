package solim.modifier;

import solim.core.Disposable;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;

final class ModifierOwnership {

    private ModifierOwnership() {
    }

    static <T extends Disposable> T own(Object component, T resource) {
        if (resource != null && OwnershipContext.current() == null) {
            ((DisposableOwner) component).adopt(resource);
        }
        return resource;
    }
}
