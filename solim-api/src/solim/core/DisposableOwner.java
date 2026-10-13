package solim.core;

/**
 * A {@link Disposable} that can take responsibility for other disposables.
 *
 * <p>
 * Implemented by the objects that keep a disposal list — components and
 * {@code Effect} — so an {@code OwnershipScope} can hand the resources a
 * factory
 * created to the owner that factory returned, instead of dropping them.
 */
public interface DisposableOwner extends Disposable {

    void adopt(Disposable resource);
}
