package fr.trinoma.myogesture.interfaces;

import java.time.Instant;

public interface GestureListener {
    void onDetection(Instant timestamp, int gestureId);
}
