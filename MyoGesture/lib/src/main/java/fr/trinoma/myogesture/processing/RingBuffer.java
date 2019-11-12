package fr.trinoma.myogesture.processing;

public class RingBuffer {
    private int oldestId = 0;
    private final float[] buffer;

    RingBuffer(int size) {
        buffer = new float[size];
    }

    void add(float value) {
        buffer[oldestId] = value;
        oldestId = (oldestId + 1) % buffer.length;
    }

    void flattenTo(float[] dst, int at) {
        for (int i = 0; i < buffer.length; ++i) {
            int ri = (oldestId + i) % buffer.length;
            dst[at + i] = buffer[ri];
        }
    }

    int size() {
        return buffer.length;
    }
}
