package mindustrytool.plugins.voicechat;

import arc.util.Log;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Android microphone implementation that receives audio from VoiceChatCompanion
 * APK.
 * The companion app captures mic audio and sends it via TCP socket.
 */
public class AndroidMicrophone {

    private static final String TAG = "[AndroidMic]";
    private static final int PORT = 25566;

    private final int sampleRate;
    private final int bufferSize;
    private boolean isOpen = false;
    private boolean isRecording = false;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InputStream inputStream;
    private Thread serverThread;
    private short[] lastBuffer;
    private final Object bufferLock = new Object();

    public AndroidMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.lastBuffer = new short[0];
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
                inputStream = clientSocket.getInputStream();

                // Read audio data
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

        while (isOpen && isRecording && inputStream != null && clientSocket != null && clientSocket.isConnected()) {
            try {
                // Read length prefix (2 bytes, big endian)
                int bytesRead = inputStream.read(lengthBuffer);
                if (bytesRead != 2)
                    break;

                int length = ((lengthBuffer[0] & 0xFF) << 8) | (lengthBuffer[1] & 0xFF);
                if (length <= 0 || length > bufferSize * 4)
                    continue;

                // Read audio data
                byte[] audioData = new byte[length];
                int totalRead = 0;
                while (totalRead < length) {
                    int read = inputStream.read(audioData, totalRead, length - totalRead);
                    if (read < 0)
                        break;
                    totalRead += read;
                }

                if (totalRead == length) {
                    // Convert bytes to shorts (little endian)
                    short[] samples = new short[length / 2];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = (short) ((audioData[i * 2] & 0xFF) | ((audioData[i * 2 + 1] & 0xFF) << 8));
                    }

                    synchronized (bufferLock) {
                        lastBuffer = samples;
                    }
                }
            } catch (Exception e) {
                Log.warn("@ Read error: @", TAG, e.getMessage());
                break;
            }
        }

        closeClient();
        Log.info("@ VoiceChatCompanion disconnected", TAG);
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
    }

    public void start() {
        isRecording = true;
        Log.info("@ Recording started (waiting for companion app)", TAG);
    }

    public void stop() {
        isRecording = false;
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
        synchronized (bufferLock) {
            return lastBuffer.length;
        }
    }

    public short[] read() {
        synchronized (bufferLock) {
            short[] result = lastBuffer;
            lastBuffer = new short[0];
            return result;
        }
    }

    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }
}
