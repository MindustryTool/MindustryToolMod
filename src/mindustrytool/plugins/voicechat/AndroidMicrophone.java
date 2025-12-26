package mindustrytool.plugins.voicechat;

import arc.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Android microphone implementation that receives audio from VoiceChatCompanion
 * APK.
 * The companion app captures mic audio and sends it via TCP socket.
 * Uses a queue-based buffer for smoother audio playback.
 * 
 * Now supports remote control of Companion App's mic:
 * - CMD_START_MIC: Tell Companion to start recording
 * - CMD_STOP_MIC: Tell Companion to stop recording (privacy)
 * - CMD_SHUTDOWN: Tell Companion to close the app
 */
public class AndroidMicrophone {

    private static final String TAG = "[AndroidMic]";
    private static final int PORT = 25566;
    private static final int MAX_QUEUE_SIZE = 10; // Max buffered frames

    // Control commands to send to Companion App
    public static final byte CMD_START_MIC = 0x01;
    public static final byte CMD_STOP_MIC = 0x02;
    public static final byte CMD_SHUTDOWN = 0x03;

    private final int sampleRate;
    private final int bufferSize;
    private boolean isOpen = false;
    private boolean isRecording = false;
    private boolean launchAttempted = false; // Only try once per session

    // Companion App package name for auto-launch
    private static final String COMPANION_PACKAGE = "com.mindustrytool.voicechat";

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread serverThread;

    // Use a queue for smoother audio buffering
    private final LinkedBlockingQueue<short[]> audioQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private int packetsReceived = 0;
    private int packetsDropped = 0;

    public AndroidMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    /**
     * Try to launch VoiceChatCompanion app via Android Intent.
     * Uses reflection to access Android Context from Arc/libGDX.
     * 
     * @return true if launch was attempted (not necessarily successful)
     */
    public boolean launchCompanionApp() {
        if (launchAttempted)
            return false;
        launchAttempted = true;

        if (mindustry.Vars.ui != null) {
            arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Launching Companion App..."));
        }

        try {
            // Get Android Context via reflection from Arc's Core.app
            Object app = arc.Core.app;
            if (app == null) {
                Log.warn("@ Cannot launch: Core.app is null", TAG);
                return false;
            }

            // Try to get Context - Arc's AndroidApplication has getContext() or similar
            Object context = null;

            // Method 1: Try getContext()
            try {
                java.lang.reflect.Method getContext = app.getClass().getMethod("getContext");
                context = getContext.invoke(app);
            } catch (Exception e1) {
                // Method 2: Try getApplicationContext()
                try {
                    java.lang.reflect.Method getAppContext = app.getClass().getMethod("getApplicationContext");
                    context = getAppContext.invoke(app);
                } catch (Exception e2) {
                    // Method 3: The app object itself might be a Context
                    if (app.getClass().getName().contains("android")) {
                        context = app;
                    }
                }
            }

            if (context == null) {
                Log.warn("@ Cannot launch: Unable to get Android Context", TAG);
                if (mindustry.Vars.ui != null)
                    arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Launch Failed: No Context"));
                return false;
            }

            // Method: Use PackageManager.getLaunchIntentForPackage(packageName)
            // This is cleaner and handles the Intent creation for us.

            // 1. Get PackageManager: context.getPackageManager()
            java.lang.reflect.Method getPackageManager = context.getClass().getMethod("getPackageManager");
            Object packageManager = getPackageManager.invoke(context);

            if (packageManager == null) {
                Log.warn("@ Cannot launch: PackageManager is null", TAG);
                if (mindustry.Vars.ui != null)
                    arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Launch Fail: No PackageManager"));
                return false;
            }

            // Direct Launch of Activity with Auto-Launch Flag
            // This bypasses background service restrictions by launching a foreground UI,
            // which then starts the service and minimizes itself.

            // 1. Create Explicit Intent for MainActivity
            Class<?> intentClass = Class.forName("android.content.Intent");
            Object intent = intentClass.getConstructor().newInstance();

            Class<?> cnClass = Class.forName("android.content.ComponentName");
            Object cn = cnClass.getConstructor(String.class, String.class)
                    .newInstance(COMPANION_PACKAGE, COMPANION_PACKAGE + ".MainActivity");

            intentClass.getMethod("setComponent", cnClass).invoke(intent, cn);
            intentClass.getMethod("setAction", String.class).invoke(intent, "android.intent.action.MAIN");
            intentClass.getMethod("addCategory", String.class).invoke(intent, "android.intent.category.LAUNCHER");

            // 2. Add Extra: intent.putExtra("EXTRA_AUTO_LAUNCH", true)
            intentClass.getMethod("putExtra", String.class, boolean.class).invoke(intent, "EXTRA_AUTO_LAUNCH", true);

            // 3. Add Flag: FLAG_ACTIVITY_NEW_TASK (Required for context launch)
            java.lang.reflect.Method addFlags = intentClass.getMethod("addFlags", int.class);
            int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
            addFlags.invoke(intent, FLAG_ACTIVITY_NEW_TASK);

            // 4. Start Activity
            java.lang.reflect.Method startActivity = context.getClass().getMethod("startActivity", intentClass);
            startActivity.invoke(context, intent);

            Log.info("@ Launched Companion App (Auto Mode)", TAG);
            if (mindustry.Vars.ui != null)
                arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("App Auto-Launched!"));

            return true;

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String exceptionType = cause.getClass().getSimpleName();
            String message = cause.getMessage();
            String fullError = exceptionType + ": " + (message != null ? message : "null");

            Log.warn("@ Failed to launch Companion App: @", TAG, fullError);
            e.printStackTrace();

            if (mindustry.Vars.ui != null) {
                arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Launch Err: " + fullError));
            }
        }
        return false;
    }

    public void open() {
        if (isOpen)
            return;

        try {
            serverSocket = new ServerSocket(PORT);
            isOpen = true;
            Log.info("@ Microphone server started on port @", TAG, PORT);
            Log.info("@ Waiting for VoiceChatCompanion app to connect...", TAG);

            // Try to auto-launch companion app on Android
            if (launchCompanionApp()) {
                Log.info("@ Companion app launch requested", TAG);
            }

            // Start server thread to accept connections
            serverThread = new Thread(this::serverLoop);
            serverThread.setDaemon(true);
            serverThread.start();
        } catch (Exception e) {
            Log.err("@ Failed to start microphone server: @", TAG, e.getMessage());
            throw new RuntimeException("Failed to start Android microphone server", e);
        }
    }

    private void serverLoop() {
        while (isOpen && serverSocket != null) {
            try {
                // Accept connection from VoiceChatCompanion app
                Socket socket = serverSocket.accept();
                Log.info("@ VoiceChatCompanion connected!", TAG);

                // Debug UI: Notify user on screen
                if (mindustry.Vars.ui != null && mindustry.Vars.ui.hudfrag != null) {
                    arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Mic Connected!"));
                }

                // Close previous connection if any
                closeClient();

                clientSocket = socket;
                clientSocket.setTcpNoDelay(true); // Disable Nagle for lower latency
                clientSocket.setSoTimeout(5000); // 5 second timeout
                inputStream = clientSocket.getInputStream();
                outputStream = clientSocket.getOutputStream();

                // If already recording, send START command immediately
                if (isRecording) {
                    sendCommand(CMD_START_MIC);
                    // Debug UI
                    if (mindustry.Vars.ui != null) {
                        arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Sending START_MIC..."));
                    }
                }

                // Read audio data continuously
                readAudioLoop();

            } catch (Exception e) {
                if (isOpen) {
                    Log.warn("@ Connection error: @", TAG, e.getMessage());
                    final String errorMsg = e.getMessage();
                    if (mindustry.Vars.ui != null) {
                        arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Mic Error: " + errorMsg));
                    }
                }
            }
        }
    }

    private void readAudioLoop() {
        byte[] lengthBuffer = new byte[2];
        packetsReceived = 0;
        packetsDropped = 0;

        // Read continuously even if not recording - just discard data
        while (isOpen && inputStream != null && clientSocket != null && clientSocket.isConnected()) {
            try {
                // Read length prefix (2 bytes, big endian)
                int bytesRead = 0;
                while (bytesRead < 2) {
                    int read = inputStream.read(lengthBuffer, bytesRead, 2 - bytesRead);
                    if (read < 0) {
                        Log.warn("@ Connection closed by companion", TAG);
                        arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Mic Disconnected (EOF)"));
                        launchAttempted = false; // Reset so we can try relaunching
                        return;
                    }
                    bytesRead += read;
                }

                int length = ((lengthBuffer[0] & 0xFF) << 8) | (lengthBuffer[1] & 0xFF);

                // Validate length
                if (length <= 0 || length > bufferSize * 4) {
                    Log.warn("@ Invalid packet length: @", TAG, length);
                    continue;
                }

                // Read audio data
                byte[] audioData = new byte[length];
                int totalRead = 0;
                while (totalRead < length) {
                    int read = inputStream.read(audioData, totalRead, length - totalRead);
                    if (read < 0) {
                        Log.warn("@ Connection closed during read", TAG);
                        launchAttempted = false; // Reset so we can try relaunching
                        return;
                    }
                    totalRead += read;
                }

                // Only process if recording is enabled
                if (isRecording && totalRead == length) {
                    // Convert bytes to shorts (little endian)
                    short[] samples = new short[length / 2];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = (short) ((audioData[i * 2] & 0xFF) |
                                ((audioData[i * 2 + 1] & 0xFF) << 8));
                    }

                    // Try to add to queue, drop oldest if full
                    if (!audioQueue.offer(samples)) {
                        audioQueue.poll(); // Remove oldest
                        audioQueue.offer(samples);
                        packetsDropped++;
                    }
                    packetsReceived++;

                    if (packetsReceived == 1) {
                        arc.Core.app.post(() -> mindustry.Vars.ui.hudfrag.showToast("Mic Data Receiving!"));
                    }

                    // Log stats periodically
                    if (packetsReceived % 100 == 0) { // Notify less frequently (every ~2s)
                        Log.info("@ Received @ packets, dropped @ (queue: @)",
                                TAG, packetsReceived, packetsDropped, audioQueue.size());
                        arc.Core.app
                                .post(() -> mindustry.Vars.ui.showLabel("Mic Data: " + packetsReceived, 1f, 0f, 0f));
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                // Timeout is OK, just continue
            } catch (Exception e) {
                Log.warn("@ Read error: @", TAG, e.getMessage());
                break;
            }
        }

        closeClient();
        Log.info("@ VoiceChatCompanion disconnected (received @ packets, dropped @)",
                TAG, packetsReceived, packetsDropped);
    }

    private void closeClient() {
        sendCommand(CMD_STOP_MIC); // Tell companion to stop recording
        try {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (Exception e) {
            // Ignore
        }
        inputStream = null;
        outputStream = null;
        clientSocket = null;
        audioQueue.clear();
    }

    /**
     * Send a control command to the Companion App.
     */
    private void sendCommand(byte cmd) {
        if (outputStream == null)
            return;
        try {
            outputStream.write(cmd);
            outputStream.flush();
            Log.info("@ Sent command: @", TAG,
                    cmd == CMD_START_MIC ? "START_MIC" : cmd == CMD_STOP_MIC ? "STOP_MIC" : "SHUTDOWN");
        } catch (Exception e) {
            Log.warn("@ Failed to send command: @", TAG, e.getMessage());
        }
    }

    public void start() {
        isRecording = true;
        audioQueue.clear();
        sendCommand(CMD_START_MIC); // Tell companion to start recording
        Log.info("@ Recording started (companion mic enabled)", TAG);
    }

    public void stop() {
        isRecording = false;
        sendCommand(CMD_STOP_MIC); // Tell companion to stop recording
        audioQueue.clear();
        Log.info("@ Recording stopped (companion mic disabled)", TAG);
    }

    public void close() {
        isOpen = false;
        isRecording = false;

        // Send SHUTDOWN command to kill the Companion App Service completely
        if (clientSocket != null && clientSocket.isConnected()) {
            Log.info("@ Sending SHUTDOWN command to Companion App...", TAG);
            sendCommand(CMD_SHUTDOWN);
            // Give it a moment to receive the command before we cut the cord
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }

        closeClient();

        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
            // Ignore
        }
        serverSocket = null;

        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }

        launchAttempted = false; // Allow re-launching if enabled again
        Log.info("@ Microphone closed", TAG);
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int available() {
        if (!isRecording)
            return 0;
        return audioQueue.isEmpty() ? 0 : bufferSize;
    }

    public short[] read() {
        short[] samples = audioQueue.poll();
        if (samples == null) {
            return new short[0];
        }
        return samples;
    }

    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }

    public int getQueueSize() {
        return audioQueue.size();
    }
}
