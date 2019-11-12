package fr.trinoma.myogesture.interfaces.signal;

public interface AccelerometerSignalType extends SampledSignalType {
    /**
     * @return Full scale measurement range in units of gravity (g)
     */
    int getAccFullScaleRange();
}
