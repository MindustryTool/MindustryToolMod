package solim.reactive;

import solim.core.Disposable;

public interface Subscription extends Disposable {
    Subscription EMPTY = new Subscription() {
        @Override
        public void dispose() {
        }

        @Override
        public boolean isDisposed() {
            return true;
        }
    };
}
