package fr.trinoma.myogesture;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.DataAcquisitionInfo;
import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;
import fr.trinoma.myogesture.interfaces.GestureRecognition;
import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.GestureListener;
import fr.trinoma.myogesture.interfaces.signal.ChannelReader;
import fr.trinoma.myogesture.interfaces.signal.SampledSignalType;
import fr.trinoma.myogesture.processing.RawDataTensorflowModel;

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
    public DataAcquisitionInfo start() {
        dataAcquisitionInfo = new DataAcquisitionInfo();
        int bufferSize = 0;
        for (DataAcquisitionApi dataAcquisitionApi : dataAcquisitionApis) {
            DataAcquisitionInfo info = dataAcquisitionApi.start();
            bufferSize += info.getMinimumBufferSize();
            for (DataAcquisitionInfo.Signal signal : info.getSignals()) {
                dataAcquisitionInfo.addSignal(signal);
            }
        }
        dataAcquisitionInfo.setMinimumBufferSize(bufferSize);
        // For now, start gesture recognition when we have subscribers
        // FIXME Raw data reading from the outside will not work while detecting
        if (gestureListeners.size() > 0) {
            new RecordingThread().start();
            new DetectionThread().start();
        }
        return dataAcquisitionInfo;
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

    final HashSet<GestureListener> gestureListeners = new HashSet<>();

    @Override
    public void registerGestureListener(GestureListener listener) {
        gestureListeners.add(listener);
    }

    @Override
    public void removeGestureListener(GestureListener listener) {
        gestureListeners.remove(listener);
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

    DataAcquisitionInfo dataAcquisitionInfo = null;
    RawDataTensorflowModel detectionModel = null;

    public void setDetectionModel(RawDataTensorflowModel model) {
        detectionModel = model;
    }

    private class RecordingThread extends Thread {
        @Override
        public void run() {
            ByteBuffer buf = ByteBuffer.allocate(dataAcquisitionInfo.getMinimumBufferSize());
            while (!isInterrupted()) {
                    buf.clear();
                    int ret = read(buf);
                    buf.flip();
                    if (ret < 0) {
                        Log.i(TAG, "read() returned: " + ret);
                        return;
                    }
                    if (detectionModel != null) {
                        detectionModel.feed(buf);
                    }
            }
        }
    }

    private class DetectionThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted() && MyoGestureRecognition.this.getState() == DataAcquisitionApi.State.STARTED) {
                if (detectionModel == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                    continue;
                }
                long stamp = System.nanoTime();
                float[] scores = detectionModel.run();
                int maxScoreId = 0;
                float maxScore = scores[0];
                for (int i = 0; i < scores.length; i++) {
                    if (scores[i] > maxScore) {
                        maxScore = scores[i];
                        maxScoreId = i;
                    }
                }
                for (GestureListener listener : gestureListeners) {
                    listener.onDetection(stamp, maxScoreId, scores);
                }
            }
        }
    }
}
