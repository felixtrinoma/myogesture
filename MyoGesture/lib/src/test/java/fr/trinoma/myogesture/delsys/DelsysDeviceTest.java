package fr.trinoma.myogesture.delsys;

import org.junit.Test;
import static org.junit.Assert.*;

import fr.trinoma.myogesture.interfaces.device.Device;

public class DelsysDeviceTest {

    @Test
    public void delsysDevicesCanBeComparedToNotifyUpdates () {
        // TODO did not manage to mock this from C#
        // ComponentInfo component = mock(ComponentInfo.class);

        // at first, only the id have been discovered
        Device deviceAt1 = new DelsysDevice(
                "00000000-0000-0000-0000-d4b5fdcf6d54",
                null, null);

        // We don’t want to be notified about this state again
        assertEquals(deviceAt1, new DelsysDevice(
                "00000000-0000-0000-0000-d4b5fdcf6d54",
                null, null));

        // then name, serial and modes are discovered
        Device deviceAt2 = new DelsysDevice(
                "00000000-0000-0000-0000-d4b5fdcf6d54", "Trigno Avanti T14",
                null);

        // We want to be notified about that change
        assertNotEquals(deviceAt1, deviceAt2);
    }
}
