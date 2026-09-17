package solim.perf;

import arc.scene.ui.layout.Table;

/**
 * Instrumented {@link Table} subclass that times {@link #act(float)},
 * {@link #validate()}, and {@link #draw()} when {@link UiProfiler#enabled} is true.
 */
public class PerfTable extends Table {

    public PerfTable() {
        super();
    }

    @Override
    public void act(float delta) {
        if (!UiProfiler.enabled) {
            super.act(delta);
            return;
        }
        long t0 = System.nanoTime();
        super.act(delta);
        long elapsed = System.nanoTime() - t0;
        UiProfiler.recordAct(elapsed / 1_000_000f);
    }

    @Override
    public void validate() {
        if (!UiProfiler.enabled) {
            super.validate();
            return;
        }
        UiProfiler.recordLayoutPass();
        long t0 = System.nanoTime();
        super.validate();
        long elapsed = System.nanoTime() - t0;
        UiProfiler.recordLayout(elapsed / 1_000_000f);
    }

    private boolean autoCommit = false;

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    @Override
    public void draw() {
        if (!UiProfiler.enabled) {
            super.draw();
            return;
        }
        long t0 = System.nanoTime();
        super.draw();
        long elapsed = System.nanoTime() - t0;
        UiProfiler.recordDraw(elapsed / 1_000_000f, getChildren().size);
        if (autoCommit) {
            UiProfiler.commitFrame(getScene() != null ? getScene().root : null);
        }
    }
}
