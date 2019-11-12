package fr.trinoma.myogesture;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;
import fr.trinoma.myogesture.interfaces.GestureRecognition;
import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.GestureListener;

public class MyoGestureRecognition implements GestureRecognition, DataAcquisitionApi {

    private static final String TAG = "MyoGestureRecognition";

    private final HashSet<DataAcquisitionApi> dataAcquisitionApis = new HashSet<>();

    public void useDataAcquisitionApi(DataAcquisitionApi api) {
        dataAcquisitionApis.add(api);
    }

    @Override
    public void enableScan(boolean enable) {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.enableScan(enable);
        }
    }

    @Override
    public List<Device> getConnectedDevices() {
        List<Device> devices = new LinkedList<>();
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            devices.addAll(dataAcquisitionApi.getConnectedDevices());
        }
        return devices;
    }

    @Override
    public void registerDeviceListener(DeviceListener deviceListener) {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.registerDeviceListener(deviceListener);
        }
    }

    @Override
    public void removeDeviceListener(DeviceListener deviceListener) {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.registerDeviceListener(deviceListener);
        }
    }

    @Override
    public void selectDevice(Device device, Mode mode, Object settings) {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            if (dataAcquisitionApi.getConnectedDevices().contains(device)) {
                dataAcquisitionApi.selectDevice(device, mode, settings);
                return;
            }
        }
        Log.e(TAG, "Unable to find API to select device " + device);
    }

    @Override
    public void unselectDevice(Device device) {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            // TODO unselect where selected only
            dataAcquisitionApi.unselectDevice(device);
        }
    }

    @Override
    public void reset() {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.reset();
        }
    }

    @Override
    public void start() throws IOException {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.start();
        }
    }

    @Override
    public void stop() {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            dataAcquisitionApi.stop();
        }
    }

    @Override
    public void calibrate() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void registerGestureListener(GestureListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeGestureListener(GestureListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int read(ByteBuffer dst) {
        // FIXME() handle multiple APIs properly
        int ret;
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            ret = dataAcquisitionApi.read(dst);
            if (ret < 0) {
                return ret;
            }
        }
        return 0;
    }

    @Override
    public State getState() {
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            if (dataAcquisitionApi.getState() == State.NOT_CONFIGURED) {
                return State.NOT_CONFIGURED;
            }
        }
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            if (dataAcquisitionApi.getState() == State.STARTED) {
                return State.STARTED;
            }
        }
        return State.READY;
    }
}
