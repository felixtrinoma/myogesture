package fr.trinoma.myogesture.delsys;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.signal.BufferedSignalType;
import fr.trinoma.myogesture.interfaces.signal.EmgSignalType;
import fr.trinoma.myogesture.interfaces.signal.RmsEmgSignalType;
import fr.trinoma.myogesture.interfaces.signal.SignalType;

public class DelsysModeTest {

    @Test
    public void handlesAllAvantiModes() {
        // TODO tests for

        for (String modeDescription : AVANTI_MODES) {
            Mode mode = DelsysMode.get(modeDescription);
        }
    }

    @Test
    public void modesCanBeCompared() {
        Assert.assertEquals(DelsysMode.get("EMG RMS"), DelsysMode.get("EMG RMS"));
        Assert.assertNotEquals(DelsysMode.get("EMG RMS"), DelsysMode.get("EMG (1000 Hz)"));
    }

    @Test
    public void modesCanBeUsedToCheckCapabilities() {
        SignalType signalType = DelsysMode.get("EMG RMS").getSignalTypes().get(0);
        assertTrue(signalType instanceof RmsEmgSignalType);
        assertFalse(signalType instanceof EmgSignalType);

        Assert.assertEquals(5, ((BufferedSignalType) signalType).getSamplesPerFrame());
    }

    @Test
    public void modesCanBeConvertedBackToDescription() {
        SignalType signalType = DelsysMode.get("EMG RMS").getSignalTypes().get(0);
        assertTrue(signalType instanceof RmsEmgSignalType);
        assertFalse(signalType instanceof EmgSignalType);

        assertEquals(5, ((BufferedSignalType) signalType).getSamplesPerFrame());
    }

    public final static String[] AVANTI_MODES = new String[]{
            "EMG (2000 Hz)",
            "EMG (1000 Hz)",
            "EMG plus Orientation (1000Hz)",
            "EMG+IMU,ACC:+/-2g,GYRO:+/-250dps",
            "EMG+IMU,ACC:+/-2g,GYRO:+/-500dps",
            "EMG+IMU,ACC:+/-2g,GYRO:+/-1000dps",
            "EMG+IMU,ACC:+/-2g,GYRO:+/-2000dps",
            "EMG+IMU,ACC:+/-4g,GYRO:+/-250dps",
            "EMG+IMU,ACC:+/-4g,GYRO:+/-500dps",
            "EMG+IMU,ACC:+/-4g,GYRO:+/-1000dps",
            "EMG+IMU,ACC:+/-4g,GYRO:+/-2000dps",
            "EMG+IMU,ACC:+/-8g,GYRO:+/-250dps",
            "EMG+IMU,ACC:+/-8g,GYRO:+/-500dps",
            "EMG+IMU,ACC:+/-8g,GYRO:+/-1000dps",
            "EMG+IMU,ACC:+/-8g,GYRO:+/-2000dps",
            "EMG+IMU,ACC:+/-16g,GYRO:+/-250dps",
            "EMG+IMU,ACC:+/-16g,GYRO:+/-500dps",
            "EMG+IMU,ACC:+/-16g,GYRO:+/-1000dps",
            "EMG+IMU,ACC:+/-16g,GYRO:+/-2000dps",
            "EMG+ACC,ACC:+/-2g",
            "EMG+ACC,ACC:+/-4g",
            "EMG+ACC,ACC:+/-8g",
            "EMG+ACC,ACC:+/-16g",
            "EMG+GYRO,GYRO:+/-250dps",
            "EMG+GYRO,GYRO:+/-500dps",
            "EMG+GYRO,GYRO:+/-1000dps",
            "EMG+GYRO,GYRO:+/-2000dps",
            "EMG RMS",
            "EMG RMS plus Orientation",
            "EMG RMS+IMU,ACC:+/-2g,GYRO:+/-250dps",
            "EMG RMS+IMU,ACC:+/-2g,GYRO:+/-500dps",
            "EMG RMS+IMU,ACC:+/-2g,GYRO:+/-1000dps",
            "EMG RMS+IMU,ACC:+/-2g,GYRO:+/-2000dps",
            "EMG RMS+IMU,ACC:+/-4g,GYRO:+/-250dps",
            "EMG RMS+IMU,ACC:+/-4g,GYRO:+/-500dps",
            "EMG RMS+IMU,ACC:+/-4g,GYRO:+/-1000dps",
            "EMG RMS+IMU,ACC:+/-4g,GYRO:+/-2000dps",
            "EMG RMS+IMU,ACC:+/-8g,GYRO:+/-250dps",
            "EMG RMS+IMU,ACC:+/-8g,GYRO:+/-500dps",
            "EMG RMS+IMU,ACC:+/-8g,GYRO:+/-1000dps",
            "EMG RMS+IMU,ACC:+/-8g,GYRO:+/-2000dps",
            "EMG RMS+IMU,ACC:+/-16g,GYRO:+/-250dps",
            "EMG RMS+IMU,ACC:+/-16g,GYRO:+/-500dps",
            "EMG RMS+IMU,ACC:+/-16g,GYRO:+/-1000dps",
            "EMG RMS+IMU,ACC:+/-16g,GYRO:+/-2000dps",
            "EMG RMS+ACC,ACC:+/-2g",
            "EMG RMS+ACC,ACC:+/-4g",
            "EMG RMS+ACC,ACC:+/-8g",
            "EMG RMS+ACC,ACC:+/-16g",
            "EMG RMS+GYRO,GYRO:+/-250dps",
            "EMG RMS+GYRO,GYRO:+/-500dps",
            "EMG RMS+GYRO,GYRO:+/-1000dps",
            "EMG RMS+GYRO,GYRO:+/-2000dps",
            "Orientation",
            "IMU,ACC:+/-2g,GYRO:+/-250dps",
            "IMU,ACC:+/-2g,GYRO:+/-500dps",
            "IMU,ACC:+/-2g,GYRO:+/-1000dps",
            "IMU,ACC:+/-2g,GYRO:+/-2000dps",
            "IMU,ACC:+/-4g,GYRO:+/-250dps",
            "IMU,ACC:+/-4g,GYRO:+/-500dps",
            "IMU,ACC:+/-4g,GYRO:+/-1000dps",
            "IMU,ACC:+/-4g,GYRO:+/-2000dps",
            "IMU,ACC:+/-8g,GYRO:+/-250dps",
            "IMU,ACC:+/-8g,GYRO:+/-500dps",
            "IMU,ACC:+/-8g,GYRO:+/-1000dps",
            "IMU,ACC:+/-8g,GYRO:+/-2000dps",
            "IMU,ACC:+/-16g,GYRO:+/-250dps",
            "IMU,ACC:+/-16g,GYRO:+/-500dps",
            "IMU,ACC:+/-16g,GYRO:+/-1000dps",
            "IMU,ACC:+/-16g,GYRO:+/-2000dps"
    };
}