package mindustrytool.plugins.voicechat;

import arc.util.Log;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Android microphone implementation that receives audio from VoiceChatCompanion
 * APK.
 * The companion app captures mic audio and sends it via TCP socket.
 * Uses a queue-based buffer for smoother audio playback.
 */
public class AndroidMicrophone {

    private static final String TAG = "[AndroidMic]";
    private static final int PORT = 25566;
    private static final int MAX_QUEUE_SIZE = 10; // Max buffered frames

    private final int sampleRate;
    private final int bufferSize;
    private boolean isOpen = false;
    private boolean isRecording = false;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStream inputStream;
    private Thread serverThread;

    // Use a queue for smoother audio buffering
    private final LinkedBlockingQueue<short[]> audioQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private int packetsReceived = 0;
    private int packetsDropped = 0;

    public AndroidMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    public void open() {
        if (isOpen)
            return;

        try {
            serverSocket = new ServerSocket(PORT);
            isOpen = true;
            Log.info("@ Microphone server started on port @", TAG, PORT);
            Log.info("@ Waiting for VoiceChatCompanion app to connect...", TAG);

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

                // Close previous connection if any
                closeClient();

                clientSocket = socket;
                clientSocket.setTcpNoDelay(true); // Disable Nagle for lower latency
                clientSocket.setSoTimeout(5000); // 5 second timeout
                inputStream = clientSocket.getInputStream();

                // Read audio data continuously
                readAudioLoop();

            } catch (Exception e) {
                if (isOpen) {
                    Log.warn("@ Connection error: @", TAG, e.getMessage());
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

                    // Log stats periodically
                    if (packetsReceived % 500 == 0) {
                        Log.info("@ Received @ packets, dropped @ (queue: @)",
                                TAG, packetsReceived, packetsDropped, audioQueue.size());
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
        try {
            if (inputStream != null)
                inputStream.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (Exception e) {
            // Ignore
        }
        inputStream = null;
        clientSocket = null;
        audioQueue.clear();
    }

    public void start() {
        isRecording = true;
        audioQueue.clear();
        Log.info("@ Recording started (waiting for companion app)", TAG);
    }

    public void stop() {
        isRecording = false;
        audioQueue.clear();
        Log.info("@ Recording stopped", TAG);
    }

    public void close() {
        isOpen = false;
        isRecording = false;

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
