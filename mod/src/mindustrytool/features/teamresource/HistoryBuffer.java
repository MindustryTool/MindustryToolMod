package mindustrytool.features.teamresource;

public class HistoryBuffer {

    public static final int CAPACITY = 60;

    private final float[] samples = new float[CAPACITY];
    private int count = 0;
    private int head = 0;
    private float min = 0f;
    private float max = 0f;

    public void add(float value) {
        samples[head] = value;
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
        recalculateBounds();
    }

    public void clear() {
        count = 0;
        head = 0;
        min = 0f;
        max = 0f;
    }

    public int size() {
        return count;
    }

    public float get(int index) {
        if (index < 0 || index >= count) {
            return 0f;
        }
        int actualIndex = (head - count + index + CAPACITY) % CAPACITY;
        return samples[actualIndex];
    }

    public float latest() {
        return count > 0 ? get(count - 1) : 0f;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    private void recalculateBounds() {
        if (count == 0) {
            min = 0f;
            max = 0f;
            return;
        }
        float currentMin = samples[0];
        float currentMax = samples[0];
        for (int i = 1; i < count; i++) {
            float val = samples[i];
            if (val < currentMin) {
                currentMin = val;
            }
            if (val > currentMax) {
                currentMax = val;
            }
        }
        min = currentMin;
        max = currentMax;
    }
}
