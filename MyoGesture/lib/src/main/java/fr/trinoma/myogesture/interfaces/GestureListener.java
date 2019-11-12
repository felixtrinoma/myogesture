package fr.trinoma.myogesture.interfaces;

import java.time.Instant;

public interface GestureListener {
    void onDetection(long nanoStamp, int gestureId, float[] scores);
}
