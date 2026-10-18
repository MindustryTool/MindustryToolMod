package solim.core;

public interface DisposableOwner extends Disposable {

    void adopt(Disposable resource);
}
