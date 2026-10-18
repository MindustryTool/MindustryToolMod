package solim.display.video;

import arc.files.Fi;
import arc.func.Cons;

@FunctionalInterface
public interface VideoLoader {

	void load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError);
}
