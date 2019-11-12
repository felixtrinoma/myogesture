package fr.trinoma.myogesture.interfaces;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;
import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.signal.ChannelReader;

public interface DataAcquisitionApi {

    enum State {
        NOT_CONFIGURED, READY, STARTED
    }

    /**
     * Continuously look for new compatible devices when enabled.
     *
     * The call is non blocking, scanning happens in background.
     * Discovered devices can be polled with {@link #getConnectedDevices()} and listened too with
     * {@link #registerDeviceListener(DeviceListener)}.
     *
     * @param enable Enables/disables scanning
     */
    void enableScan(boolean enable);

    /**
     * Lists devices that have been discovered.
     *
     * @see #enableScan(boolean)
     *
     * @return A list of supported devices.
     */
    List<Device> getConnectedDevices();

    void registerDeviceListener(DeviceListener deviceListener);

    void removeDeviceListener(DeviceListener deviceListener);

    /**
     * Selects a device to be used by the gesture recognition algorithm.
     *
     * @param device a compatible device from {@link #getConnectedDevices()}
     * @param mode a compatible mode from {@link Device#getSupportedModes()}
     * @param settings TODO to be defined, tells how the sensor are positioned for example
     */
    void selectDevice(Device device, Mode mode, Object settings);

    void unselectDevice(Device device);

    /**
     * Clears selected devices and there configuration.
     * Stops the current capture if started.
     */
    void reset();

    /**
     * Starts capturing data from the selected devices.
     * Starts the detection algorithms depending on registered listeners.
     */
    DataAcquisitionInfo start();

    /**
     * Stops capturing data from the selected devices, keeps the current configuration.
     * @see #reset()
     */
    void stop();


    /**
     * Provides a direct access to the sensors data.
     *
     * This method is synchronous, it requires the user to provide its own buffers.
     * This gives flexibility to manage the incoming data as needed, like reusing the same buffer,
     * pooling them or using direct ByteBuffer to be used in native code.
     *
     * TODO An interface describing its content will be provided, it is not fixed yet.
     * TODO An interface helping to allocate the buffers will be provided, it is not fixed yet.
     *
     * @param dst a ByteBuffer in writing mode, incoming data will be written to it.
     * @return TODO an error code to be defined
     */
    int read(ByteBuffer dst);

    State getState();
}
