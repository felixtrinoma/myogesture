package fr.trinoma.myogesture.delsys;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import fr.trinoma.daq.delsys.androidwrapper.ChannelInfo;
import fr.trinoma.daq.delsys.androidwrapper.ComponentConfig;
import fr.trinoma.daq.delsys.androidwrapper.ComponentInfo;
import fr.trinoma.daq.delsys.androidwrapper.DelsysApiWrapper;
import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.DataAcquisitionInfo;
import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;
import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.signal.ChannelReader;
import fr.trinoma.myogesture.interfaces.signal.SampledSignalType;

public class DelsysApi implements DataAcquisitionApi {

    private static final String TAG = "DelsysApi";
    private static final DelsysApi INSTANCE = new DelsysApi();
    private static final DelsysApiWrapper DELSYS_API = new DelsysApiWrapper();

    private boolean isInitialized = false;

    // Expected to be read/written to by locking DELSYS_API
    //private boolean isStarted = false;

    private double scanFinishedAt = 0;

    private DelsysApi() { }

    public static DelsysApi getInstance() { return INSTANCE; }

    public void initialize(String publicKey, String license) {
        synchronized (DELSYS_API) {
            DELSYS_API.initialize(publicKey, license);
            isInitialized = true;
        }
    }

    private final AtomicReference<ScanningThread> scanningThread = new AtomicReference<>();
    private final HashMap<String, DelsysDevice> connectedDevices = new HashMap<>();
    private final HashSet<DeviceListener> deviceListeners = new HashSet<>();

    @Override
    public void enableScan(boolean enable) {
        synchronized (scanningThread) {
            ScanningThread thread = scanningThread.get();
            if (thread != null && !enable) {
                thread.interrupt();
                scanningThread.set(null);
            } else if (thread == null && enable) {
                thread = new ScanningThread();
                thread.start();
                scanningThread.set(thread);
            }
        }
    }

    private class ScanningThread extends Thread {
        @Override
        public void run() {
            while (!hasBeenInterrupted) {
                ComponentInfo[] components = null;
                synchronized (DELSYS_API) {
                    // FIXME check when we can scan exactly
                    if (isInitialized && DELSYS_API.getState() <= PIPELINE_Armed) {
                        if (DELSYS_API.getState() > PIPELINE_Connected) {
                            DELSYS_API.disarm();
                        }
                        if (DELSYS_API.scan()) {
                            components = DELSYS_API.listDevices();
                            scanFinishedAt = System.nanoTime() / 1e9;
                        }
                    }
                }
                // Scan was not possible, wait after releasing DELSYS_API
                if (components == null) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        return;
                    }
                    continue;
                } else {
                    Log.i(TAG, "Scanned, " + components.length + " devices connected");
                }

                HashMap<String, DelsysDevice> freshDevices = new HashMap<>();
                for (ComponentInfo component : components) {
                    DelsysDevice device = new DelsysDevice(component);
                    freshDevices.put(device.getId(), device);
                }
                // Notifies about disconnected devices
                Iterator<HashMap.Entry<String, DelsysDevice> >
                        iterator = connectedDevices.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry<String, DelsysDevice> entry = iterator.next();
                    if (!freshDevices.containsKey(entry.getKey())) {
                        synchronized (deviceListeners) {
                            for (DeviceListener listener : deviceListeners) {
                                listener.onUpdate(entry.getValue(), false);
                            }
                        }
                        iterator.remove();
                    }
                }

                // Notifies about changes and new devices
                for (HashMap.Entry<String, DelsysDevice> entry : freshDevices.entrySet()) {
                    DelsysDevice knownDevice =  connectedDevices.get(entry.getKey());
                    if (knownDevice == null || knownDevice != entry.getValue()) {
                        synchronized (deviceListeners) {
                            connectedDevices.put(entry.getKey(), entry.getValue());
                            for (DeviceListener listener : deviceListeners) {
                                listener.onUpdate(entry.getValue(), true);
                            }
                        }
                    }
                }
            }
        }

        boolean hasBeenInterrupted = false;

        // When interrupting while scanning, it looks like the C# part of the
        // API swallows the InterruptException. In this case the interrupt flag is cleared so
        // we miss the interrupt. This other flags allows to get notified anyway.
        @Override
        public void interrupt() {
            hasBeenInterrupted = true;
            super.interrupt();
        }
    }

    @Override
    public List<Device> getConnectedDevices() {
        return new ArrayList<Device>(connectedDevices.values());
    }

    @Override
    public void registerDeviceListener(DeviceListener deviceListener) {
        synchronized (deviceListeners) {
            deviceListeners.add(deviceListener);
        }
    }

    @Override
    public void removeDeviceListener(DeviceListener deviceListener) {
        synchronized (deviceListeners) {
            deviceListeners.remove(deviceListener);
        }
    }

    private final HashMap<String, DelsysMode> selectedDevices = new HashMap<>();

    @Override
    public void selectDevice(Device device, Mode mode, Object settings) {
        if (device instanceof DelsysDevice && mode instanceof DelsysMode) {
            Log.i(TAG, "Selecting device " + device.getId());
            selectedDevices.put(device.getId(), (DelsysMode) mode);
        } else {
            throw new IllegalArgumentException("Unable to select a non Delsys device");
        }
    }

    @Override
    public void unselectDevice(Device device) {
        if (device instanceof DelsysDevice) {
            selectedDevices.remove(device.getId());
        }
    }

    @Override
    public void reset() {
        stop();
        selectedDevices.clear();
    }


    @Override
    public DataAcquisitionInfo start() {
        ensureInitialized();
        DataAcquisitionInfo aquisitionInfo = new DataAcquisitionInfo();
        synchronized (DELSYS_API) {
            if (selectedDevices.isEmpty()) {
                throw new UnsupportedOperationException(
                        "Unable to start streaming when no device have been selected");
            }
            if (DELSYS_API.getState() == PIPELINE_Armed) {
                if (!DELSYS_API.disarm()) {
                    Log.e(TAG, "Unable to disarm pipleline");
                }
            }
            // Wait a bit after scan is finished before starting
            // FIXME check why do we need to wait before starting
            while (System.nanoTime() / 1e9 < scanFinishedAt + 10.0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return aquisitionInfo;
                }
            }
            final ArrayList<ComponentConfig> devicesConfig = new ArrayList<>();
            for (HashMap.Entry<String, DelsysMode> entry : selectedDevices.entrySet()) {
                devicesConfig.add(ComponentConfig.create(
                        entry.getKey(), entry.getValue().getDescription()));
            }
            Log.i(TAG, "Arming " + devicesConfig.size() + " devices");
            ChannelInfo[] infos = DELSYS_API.arm(devicesConfig);
            if (infos == null) {
                Log.e(TAG, "Unable to arm pipleline");
                return aquisitionInfo;
            }
            Log.i(TAG, "Armed");

            int offset = 0;
            for (final ChannelInfo info : infos) {
                Log.i(TAG, String.format(Locale.US, "Channel for %s: %s, %d, %f",
                        info.getComponentId(), info.getUnit(), info.getSamplesPerFrame(), info.getFrameInterval()));

                // TODO I get twice the advertized amount per frame, don’t know why
                final int actualSamplesPerFrame = info.getSamplesPerFrame() * 2;
                Device device = connectedDevices.get(info.getComponentId());
                aquisitionInfo.addSignal(device, new DelsysChannelReader(offset, actualSamplesPerFrame, new SampledSignalType() {
                    @Override
                    public int getSizeOfSample() {
                        return 8;
                    }

                    @Override
                    public float getSampleInterval() {
                        return (float) info.getFrameInterval() / info.getSamplesPerFrame();
                    }
                }), "");
                offset += actualSamplesPerFrame;
            }
            aquisitionInfo.setMinimumBufferSize(offset * 8);

            Set<String> presentComponents = new HashSet<>();
            for (int i = 0; i < infos.length; ++i) {
                presentComponents.add(infos[i].getComponentId());
            }
            if (presentComponents.size() != selectedDevices.size()) {
                // This happened when not waiting after scan…
                Log.e(TAG, "Some selected components missing after configuration");
                // TODO return false?
                return aquisitionInfo;
            }
            Log.i(TAG, "Srarting");
            DELSYS_API.start();
            Log.i(TAG, "Started");
        }
        return aquisitionInfo;
    }

    @Override
    public void stop() {
        ensureInitialized();
        synchronized (DELSYS_API) {
            if (isStarted()) {
                if (!DELSYS_API.stop()) {
                    Log.e(TAG, "Failed to stop");
                }
                // TODO cannot disarm here, but works a bit later
//                if (!DELSYS_API.disarm()) {
//                    Log.e(TAG, "Failed to disarm");
//                }
            }
        }
    }

    @Override
    public int read(ByteBuffer dst) {
        if (DELSYS_API.read(dst)) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public State getState() {
        if (isStarted()) {
            return State.STARTED;
        } else if (isInitialized && !selectedDevices.isEmpty()) {
            return State.READY;
        } else {
            return State.NOT_CONFIGURED;
        }
    }

    private void ensureInitialized() {
        if (!isInitialized) {
            throw new UnsupportedOperationException(
                    "Delsys API have not been initialized");
        }
    }

    private static int PIPELINE_Off = 0;
    private static int PIPELINE_Connected = 1;
    private static int PIPELINE_InputsConfigured = 2;
    private static int PIPELINE_OutputsConfigured = 3;
    private static int PIPELINE_Armed = 4;
    private static int PIPELINE_Running = 5;
    private static int PIPELINE_Finished = 6;

    private boolean isStarted() {
        return DELSYS_API.getState() == PIPELINE_Running;
    }
}
