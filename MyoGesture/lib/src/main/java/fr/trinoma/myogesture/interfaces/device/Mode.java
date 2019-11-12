package fr.trinoma.myogesture.interfaces.device;

import java.util.List;

import fr.trinoma.myogesture.interfaces.signal.SignalType;

/**
 * Used by a {@link Device} to list signals that can be captured together.
 *
 * For instance a Trigno mode could be
 * [EmgRmsSignal(333 Hz), OrientationSignalType(100 Hz)], a Quattro would list 4 times
 * the EMG signal.
 */

public interface Mode {

    List<SignalType> getSignalTypes();

    /**
     * @return Description to be used in UI
     */
    String getDescription();
}
