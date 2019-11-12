package fr.trinoma.myogesture.interfaces.signal;

public interface SampledSignalType extends SignalType {
    /**
     * @return Size of each sample in bytes
     */
    int getSizeOfSample(); // size of sample in bytes

    /**
     * @return Interval between two samples
     */
    float getSampleInterval();
}
