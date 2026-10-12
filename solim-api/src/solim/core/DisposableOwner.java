package solim.core;

/**
 * A {@link Disposable} that can take responsibility for other disposables.
 *
 * <p>Implemented by the objects that keep a disposal list — components and
 * {@code Effect} — so an {@code OwnershipScope} can hand the resources a factory
 * created to the owner that factory returned, instead of dropping them.
 */
public interface DisposableOwner extends Disposable {
	/** Takes responsibility for {@code resource}; it is disposed together with this owner. */
	void adopt(Disposable resource);
}
