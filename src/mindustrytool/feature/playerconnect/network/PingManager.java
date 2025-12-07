package mindustrytool.feature.playerconnect.network;

import java.util.concurrent.ExecutorService;
import arc.func.Cons;
import arc.net.Client;
import arc.util.*;

public class PingManager {
    private static Client pinger;
    private static Thread pingerThread;
    private static ExecutorService worker = Threads.unboundedExecutor("Worker", 1);
    private static arc.net.NetSerializer tmpSerializer;

    public static void ping(String ip, int port, Cons<Long> onOk, Cons<Exception> onFail) {
        if (tmpSerializer == null) tmpSerializer = new ProxySerializer();
        if (pinger == null || pingerThread == null || !pingerThread.isAlive())
            pingerThread = Threads.daemon("Pinger", pinger = new Client(8192, 8192, tmpSerializer));
        worker.submit(() -> {
            synchronized (pingerThread) {
                long time = Time.millis();
                try {
                    pinger.connect(2000, ip, port);
                    time = Time.timeSinceMillis(time);
                    pinger.close();
                    onOk.get(time);
                } catch (Exception e) { onFail.get(e); }
            }
        });
    }

    public static void dispose() {
        if (pinger != null) {
            pinger.stop();
            if (pingerThread != null) {
                try { pingerThread.join(1000); } catch (InterruptedException ignored) {}
            }
            try { pinger.dispose(); } catch (Exception ignored) {}
            pingerThread = null; pinger = null;
        }
    }
}
