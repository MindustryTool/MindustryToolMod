package mindustrytool.plugins.voicechat;

import arc.util.Log;

/**
 * Android microphone implementation using android.media.AudioRecord.
 * This class should only be loaded on Android platforms.
 * Uses reflection to avoid compile-time dependency on Android SDK.
 */
public class AndroidMicrophone {

    private static final String TAG = "[AndroidMic]";

    private Object audioRecord; // android.media.AudioRecord
    private final int sampleRate;
    private final int bufferSize;
    private int minBufferSize;
    private boolean isRecording = false;

    // Android AudioRecord constants (via reflection to avoid compile dependency)
    private static final int CHANNEL_IN_MONO = 16; // AudioFormat.CHANNEL_IN_MONO
    private static final int ENCODING_PCM_16BIT = 2; // AudioFormat.ENCODING_PCM_16BIT
    private static final int SOURCE_MIC = 1; // MediaRecorder.AudioSource.MIC
    private static final int STATE_INITIALIZED = 1; // AudioRecord.STATE_INITIALIZED
    private static final int RECORDSTATE_RECORDING = 3; // AudioRecord.RECORDSTATE_RECORDING

    public AndroidMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    public void open() {
        try {
            // Check and request RECORD_AUDIO permission first
            if (!hasRecordAudioPermission()) {
                requestRecordAudioPermission();
                // Note: Permission result is async, may need to retry opening
                Log.warn("@ RECORD_AUDIO permission not granted. Please allow in Settings.", TAG);
                throw new IllegalStateException("RECORD_AUDIO permission not granted");
            }

            // Get minimum buffer size
            Class<?> audioRecordClass = Class.forName("android.media.AudioRecord");
            minBufferSize = (int) audioRecordClass.getMethod("getMinBufferSize", int.class, int.class, int.class)
                    .invoke(null, sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT);

            if (minBufferSize <= 0) {
                throw new IllegalStateException("Invalid min buffer size: " + minBufferSize);
            }

            // Use larger buffer to avoid underruns
            int actualBufferSize = Math.max(minBufferSize, bufferSize * 2 * 2); // bufferSize samples * 2 bytes * 2

            // Create AudioRecord instance
            audioRecord = audioRecordClass.getConstructor(int.class, int.class, int.class, int.class, int.class)
                    .newInstance(SOURCE_MIC, sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT, actualBufferSize);

            // Check if initialized properly
            int state = (int) audioRecordClass.getMethod("getState").invoke(audioRecord);
            if (state != STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord not initialized. State: " + state);
            }

            Log.info("@ Microphone opened (Android)", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open microphone: @", TAG, e.getMessage());
            throw new RuntimeException("Failed to open Android microphone", e);
        }
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    private boolean hasRecordAudioPermission() {
        try {
            // Get Android context from Mindustry
            Object context = Class.forName("mindustry.android.AndroidLauncher")
                    .getField("context").get(null);

            if (context == null) {
                // Fallback: try to get from Core.app
                Object app = arc.Core.app;
                // AndroidApplication has getContext() method
                context = app.getClass().getMethod("getContext").invoke(app);
            }

            if (context == null) {
                Log.warn("@ Cannot get Android context", TAG);
                return false;
            }

            // ContextCompat.checkSelfPermission(context, RECORD_AUDIO)
            Class<?> contextCompatClass = Class.forName("androidx.core.content.ContextCompat");
            int result = (int) contextCompatClass.getMethod("checkSelfPermission",
                    Class.forName("android.content.Context"), String.class)
                    .invoke(null, context, "android.permission.RECORD_AUDIO");

            // PackageManager.PERMISSION_GRANTED = 0
            return result == 0;
        } catch (Exception e) {
            Log.err("@ Error checking permission: @", TAG, e.getMessage());
            // Try alternative approach - check via ContextWrapper
            return hasRecordAudioPermissionAlt();
        }
    }

    /**
     * Alternative permission check without ContextCompat.
     */
    private boolean hasRecordAudioPermissionAlt() {
        try {
            Object app = arc.Core.app;
            // AndroidApplication has checkSelfPermission via Context
            Object context = app.getClass().getMethod("getContext").invoke(app);

            int result = (int) context.getClass()
                    .getMethod("checkSelfPermission", String.class)
                    .invoke(context, "android.permission.RECORD_AUDIO");

            return result == 0;
        } catch (Exception e) {
            Log.err("@ Alt permission check failed: @", TAG, e.getMessage());
            return false;
        }
    }

    /**
     * Request RECORD_AUDIO permission from user.
     */
    private void requestRecordAudioPermission() {
        try {
            Object app = arc.Core.app;
            // AndroidApplication is Activity subclass
            Object activity = app;

            // ActivityCompat.requestPermissions(activity, permissions, requestCode)
            Class<?> activityCompatClass = Class.forName("androidx.core.app.ActivityCompat");
            activityCompatClass.getMethod("requestPermissions",
                    Class.forName("android.app.Activity"),
                    String[].class,
                    int.class)
                    .invoke(null, activity, new String[] { "android.permission.RECORD_AUDIO" }, 1001);

            Log.info("@ Permission request sent", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to request permission: @", TAG, e.getMessage());
            // Try alternative approach
            requestPermissionViaIntent();
        }
    }

    /**
     * Open app settings as fallback for permission.
     */
    private void requestPermissionViaIntent() {
        try {
            Object app = arc.Core.app;
            Object context = app.getClass().getMethod("getContext").invoke(app);

            // Create intent to app settings
            Class<?> intentClass = Class.forName("android.content.Intent");
            Class<?> settingsClass = Class.forName("android.provider.Settings");
            Class<?> uriClass = Class.forName("android.net.Uri");

            String settingsAction = (String) settingsClass.getField("ACTION_APPLICATION_DETAILS_SETTINGS").get(null);
            Object intent = intentClass.getConstructor(String.class).newInstance(settingsAction);

            // Get package name
            String packageName = (String) context.getClass().getMethod("getPackageName").invoke(context);
            Object uri = uriClass.getMethod("parse", String.class).invoke(null, "package:" + packageName);

            intentClass.getMethod("setData", uriClass).invoke(intent, uri);
            intentClass.getMethod("addFlags", int.class).invoke(intent, 0x10000000); // FLAG_ACTIVITY_NEW_TASK

            context.getClass().getMethod("startActivity", intentClass).invoke(context, intent);

            Log.info("@ Opened app settings for permission", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open settings: @", TAG, e.getMessage());
        }
    }

    public void start() {
        if (audioRecord == null)
            return;
        try {
            audioRecord.getClass().getMethod("startRecording").invoke(audioRecord);
            isRecording = true;
            Log.info("@ Recording started", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to start recording: @", TAG, e.getMessage());
        }
    }

    public void stop() {
        if (audioRecord == null)
            return;
        try {
            audioRecord.getClass().getMethod("stop").invoke(audioRecord);
            isRecording = false;
            Log.info("@ Recording stopped", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to stop recording: @", TAG, e.getMessage());
        }
    }

    public void close() {
        if (audioRecord == null)
            return;
        try {
            stop();
            audioRecord.getClass().getMethod("release").invoke(audioRecord);
            audioRecord = null;
            Log.info("@ Microphone closed", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to close microphone: @", TAG, e.getMessage());
        }
    }

    public boolean isOpen() {
        if (audioRecord == null)
            return false;
        try {
            int state = (int) audioRecord.getClass().getMethod("getState").invoke(audioRecord);
            return state == STATE_INITIALIZED;
        } catch (Exception e) {
            return false;
        }
    }

    public int available() {
        // Android AudioRecord doesn't have an "available" method like javax.sound
        // Return buffer size if recording
        return isRecording ? bufferSize : 0;
    }

    public short[] read() {
        if (audioRecord == null || !isRecording) {
            throw new IllegalStateException("Microphone is not recording");
        }

        try {
            short[] buffer = new short[bufferSize];
            int samplesRead = (int) audioRecord.getClass()
                    .getMethod("read", short[].class, int.class, int.class)
                    .invoke(audioRecord, buffer, 0, bufferSize);

            if (samplesRead < 0) {
                Log.err("@ Read error: @", TAG, samplesRead);
                return new short[0];
            }

            if (samplesRead < bufferSize) {
                // Return only what we got
                short[] result = new short[samplesRead];
                System.arraycopy(buffer, 0, result, 0, samplesRead);
                return result;
            }

            return buffer;
        } catch (Exception e) {
            Log.err("@ Failed to read audio: @", TAG, e.getMessage());
            return new short[0];
        }
    }
}
