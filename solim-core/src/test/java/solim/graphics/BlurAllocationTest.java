package solim.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import arc.scene.ui.layout.Table;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

public class BlurAllocationTest extends SolimEnv {

    @Test
    void testZeroAllocationDuringSteadyState() {
        Object baseBean = ManagementFactory.getThreadMXBean();
        if (!(baseBean instanceof ThreadMXBean)) {
            return;
        }
        ThreadMXBean threadBean = (ThreadMXBean) baseBean;
        if (!threadBean.isThreadAllocatedMemorySupported()) {
            return;
        }
        if (!threadBean.isThreadAllocatedMemoryEnabled()) {
            threadBean.setThreadAllocatedMemoryEnabled(true);
        }
        long threadId = Thread.currentThread().getId();

        BlurRenderer renderer = BlurRenderer.get();
        Table table = new Table();
        table.setSize(200f, 100f);

        for (int i = 0; i < 20; i++) {
            renderer.drawBackdropBlur(table, 0f, 0f, 200f, 100f, 8f, 1f);
            renderer.beginContentBlur(table);
            renderer.endContentBlur(table, 5f);
        }

        long before = threadBean.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < 50; i++) {
            renderer.drawBackdropBlur(table, 0f, 0f, 200f, 100f, 8f, 1f);
            renderer.beginContentBlur(table);
            renderer.endContentBlur(table, 5f);
        }
        long after = threadBean.getThreadAllocatedBytes(threadId);

        assertEquals(0L, after - before);
    }
}
