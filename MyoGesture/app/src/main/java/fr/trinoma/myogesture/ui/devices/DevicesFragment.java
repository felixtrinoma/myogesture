package fr.trinoma.myogesture.ui.devices;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.List;

import fr.trinoma.myogesture.MainActivity;
import fr.trinoma.myogesture.R;
import fr.trinoma.myogesture.delsys.DelsysMode;
import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.DeviceListener;
import fr.trinoma.myogesture.interfaces.device.Mode;

public class DevicesFragment extends Fragment {

    private static final String TAG = "ScanActivity";

    private DevicesViewModel devicesViewModel;
    private TextView devicesTextView;
    private ProgressBar mScanProgressBar;
    private DataAcquisitionApi dataAcquisition;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_devices, container, false);

        devicesTextView = root.findViewById(R.id.devicesTextView);
        mScanProgressBar = root.findViewById(R.id.scanProgressBar);

        devicesViewModel = ViewModelProviders.of(this).get(DevicesViewModel.class);

        final Observer<String> descriptionObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String newName) {
                // Update the UI, in this case, a TextView.
                devicesTextView.setText(newName);
            }
        };
        devicesViewModel.getConnectedDevicesAsText().observe(this, descriptionObserver);

        dataAcquisition = MainActivity.gestureRecognition;
        dataAcquisition.registerDeviceListener(devicesViewModel);

        // For now let say every scanned devices will be used for gesture recognition
        dataAcquisition.registerDeviceListener(new DeviceListener() {
            @Override
            public void onUpdate(Device device, boolean isConnected) {
                Log.i(TAG, "Device updated: " + device.getId());
                if (isConnected) {
                    Log.i(TAG, "Connected");
                } else {
                    Log.i(TAG, "Not Connected");
                }
                List<Mode> modes = device.getSupportedModes();
                for (Mode mode : modes) {
                    Log.i(TAG, mode.getDescription());
                }
                if (!isConnected) {
                    dataAcquisition.unselectDevice(device);
                } else if (device.getModel() != null) {
                    if (device.getModel().contains("Avanti T14")) {
                        dataAcquisition.selectDevice(device, DelsysMode.get("EMG RMS"), null);
//                    dataAcquisition.selectDevice(device, DelsysMode.get("EMG RMS+ACC,ACC:+/-4g"), null);
                    } else {
                        dataAcquisition.selectDevice(device, DelsysMode.get("EMG RMS x4 plus IMU,ACC:+/-16g,GYRO:+/-2000dps"), null);
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        enableScan(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        enableScan(true);
    }

    void enableScan(boolean enable) {
        if (enable) {
            mScanProgressBar.setVisibility(View.VISIBLE);
        } else {
            mScanProgressBar.setVisibility(View.GONE);
        }
        dataAcquisition.enableScan(enable);
    }
}
