package fr.trinoma.myogesture.interfaces;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Implemented by the Trinoma gesture recognition algorithm.
 */
public interface GestureRecognition {

    /**
     * Starts a calibration sequence.
     * TODO Behavior and callbacks to be defined.
     */
    void calibrate();

    /**
     * Get notified when new gestures are detected.
     *
     * Detection might not be running if no listeners are registered.
     */
    void registerGestureListener(GestureListener listener);

    void removeGestureListener(GestureListener listener);
}
