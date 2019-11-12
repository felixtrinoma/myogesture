package fr.trinoma.myogesture.interfaces.signal;

public interface GyrometerSignalType extends SampledSignalType {
    /**
     * @return Full scale measurement range in degrees per second (dps)
     */
    int getGyroFullScaleRange();
}
