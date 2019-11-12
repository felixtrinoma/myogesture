package fr.trinoma.myogesture.interfaces.device;

import java.util.List;

/**
 * A generic device that can be used by the gesture recognition algorithm.
 */
public interface Device {
    /**
     * Vendor unique identifier, like a mac address or a serial number
     * @return
     */
    String getId();
    String getVendor();
    String getModel();
    List<Mode> getSupportedModes();
}
