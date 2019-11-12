package fr.trinoma.myogesture.ui.devices;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;

public class DevicesViewModel extends ViewModel implements DeviceListener {

    private final static String EMPTY_PLACEHOLDER = "Looking for devices…";

    private MutableLiveData<String> connectedDevicesAsText;

    public MutableLiveData<String> getConnectedDevicesAsText() {
        if (connectedDevicesAsText == null) {
            connectedDevicesAsText = new MutableLiveData<>();
            connectedDevicesAsText.setValue(EMPTY_PLACEHOLDER);
        }
        return connectedDevicesAsText;
    }

    private static class DeviceInfo {
        final String description;
        final boolean isConnected;
        DeviceInfo(Device device, boolean isConnected) {
            this.isConnected = isConnected;
            StringBuilder builder = new StringBuilder();
            if (!isConnected) {
                builder.append("[Disconnected] ");
            }
            builder.append(device.getId());
            builder.append('\n');
            if (device.getModel() != null) {
                builder.append(device.getModel());
            } else {
                builder.append("Unknown model");
            }
            description = builder.toString();
        }
    }

    private final HashMap<String, DeviceInfo> devicesInfo = new HashMap<>();

    @Override
    public synchronized void onUpdate(Device device, boolean isConnected) {
        String uuid = device.getVendor() + device.getId();
        devicesInfo.put(uuid, new DeviceInfo(device, isConnected));
        StringBuilder builder = new StringBuilder();
        for (DeviceInfo info : devicesInfo.values()) {
            builder.append(info.description);
            builder.append("\n\n");
        }
        String description = builder.toString();
        if (description.isEmpty()) {
            getConnectedDevicesAsText().postValue(EMPTY_PLACEHOLDER);
        } else {
            getConnectedDevicesAsText().postValue(builder.toString());
        }
    }
}