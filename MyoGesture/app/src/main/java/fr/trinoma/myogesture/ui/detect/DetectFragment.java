package fr.trinoma.myogesture.ui.detect;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import fr.trinoma.myogesture.Config;
import fr.trinoma.myogesture.MainActivity;
import fr.trinoma.myogesture.MyoGestureRecognition;
import fr.trinoma.myogesture.R;
import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.DataAcquisitionInfo;
import fr.trinoma.myogesture.interfaces.GestureListener;
import fr.trinoma.myogesture.interfaces.signal.ChannelReader;
import fr.trinoma.myogesture.interfaces.signal.SampledSignalType;
import fr.trinoma.myogesture.processing.RawDataTensorflowModel;
import fr.trinoma.myogesture.ui.train.TrainFragmentDirections;

import static fr.trinoma.myogesture.ui.detect.DetectionState.STARTING;
import static fr.trinoma.myogesture.ui.detect.DetectionState.STOPPED;
import static fr.trinoma.myogesture.ui.detect.DetectionState.STOPPING;

public class DetectFragment extends Fragment {

    private static final String TAG = "DetectFragment";

    private MyoGestureRecognition gestureRecognition;

    private DetectViewModel detectViewModel;
    private ProgressBar progressBar;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        detectViewModel =
                ViewModelProviders.of(this).get(DetectViewModel.class);
        View root = inflater.inflate(R.layout.fragment_detect, container, false);
        final TextView textView = root.findViewById(R.id.detected_gesture);
        detectViewModel.getDetectedGesture().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        progressBar = root.findViewById(R.id.detect_progressbar);

        setHasOptionsMenu(true);

        detectViewModel.getState().observe(this, uiStateUpdater);
        detectViewModel.postState(STARTING);

        gestureRecognition = MainActivity.gestureRecognition;

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        startRecording();
    }

    @Override
    public void onPause() {
        stopRecording();
        super.onPause();
    }

    private MenuItem captureRestartButton;
    private MenuItem captureStopButton;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.train_menu, menu);
        captureRestartButton = menu.findItem(R.id.capture_restart_btn);
        captureStopButton = menu.findItem(R.id.capture_stop_btn);
        captureRestartButton.setVisible(false);
        captureStopButton.setVisible(true);
        captureStopButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                stopRecording();
                return true;
            }
        });
        captureRestartButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                startRecording();
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    GestureListener gestureListener = new GestureListener() {
        @Override
        public void onDetection(long timestamp, int gestureId, float[] scores) {
            String gesture;
            if (gestureId < Config.GESTURES.length) {
                gesture = Config.GESTURES[gestureId];
            } else {
                gesture = Integer.toString(gestureId);
            }
            detectViewModel.postDetectedGesture("Detected " + gesture);
        }
    };

    void startRecording() {
        detectViewModel.postState(STARTING);
        Log.i(TAG, "Starting record");
        String error = null;
        Interpreter interpreter = null;
        if (gestureRecognition.getState() == DataAcquisitionApi.State.STARTED) {
            error = "Unable to record: Previous capture not finished.";
        } else if (gestureRecognition.getState() != DataAcquisitionApi.State.READY) {
            error = "Unable to record: Not configured.";
        } else {
            try {
                interpreter = RawDataTensorflowModel.makeInterpreter(getContext(), Config.MODEL_ASSET);
            } catch (IOException e) {
                error = "Unable to load " + Config.MODEL_ASSET + " from assets";
                Log.e(TAG, "Unable to load model", e);
            }
        }
        if (error == null) {
            final Interpreter interpreterFinal = interpreter;
            new Thread() {
                @Override
                public void run() {
                    gestureRecognition.registerGestureListener(gestureListener);
                    DataAcquisitionInfo info = gestureRecognition.start();
                    if (info.getSignals().size() < 2) {
                        Log.e(TAG, "Model was intended for 2 signals, stopping");
                        stopRecording();
                    }
                    DataAcquisitionInfo.Signal emg1 = info.getSignals().get(0);
                    DataAcquisitionInfo.Signal emg2 = info.getSignals().get(1);
                    float sampleInterval = ((SampledSignalType) emg1.getReader().getSignalType()).getSampleInterval();
                    int frameSize = Math.round(Config.GESTURE_DURATIION / sampleInterval);
                    LinkedList<Pair<ChannelReader, Integer>> channels = new LinkedList<>();
                    channels.add(new Pair<>(emg1.getReader(), frameSize));
                    channels.add(new Pair<>(emg2.getReader(), frameSize));

                    RawDataTensorflowModel model = new RawDataTensorflowModel(interpreterFinal, channels, info.getMinimumBufferSize());
                    gestureRecognition.setDetectionModel(model);
                    detectViewModel.postState(DetectionState.STARTED);
                }
            }.start();
        } else {
            Log.e(TAG, error);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            // TODO go back to devices
            NavDirections action = DetectFragmentDirections.actionNavigationDetectToNavigationDevices();
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    void stopRecording() {
        detectViewModel.postState(STOPPING);
        new Thread() {
            @Override
            public void run() {
                gestureRecognition.stop();
                gestureRecognition.removeGestureListener(gestureListener);
                detectViewModel.postState(STOPPED);
            }
        }.start();
    }

    final Observer<DetectionState> uiStateUpdater = new Observer<DetectionState>() {
        @Override
        public void onChanged(@Nullable final DetectionState newState) {
            switch (newState) {
                case STARTING:
                    progressBar.setVisibility(View.VISIBLE);
                    detectViewModel.postDetectedGesture("Configuring devices…");
                    break;
                case STARTED:
                    progressBar.setVisibility(View.GONE);
                    detectViewModel.postDetectedGesture("Detecting…");
                    break;
                case STOPPING:
                    progressBar.setVisibility(View.VISIBLE);
                    detectViewModel.postDetectedGesture("Stopping…");
                    break;
                case STOPPED:
                    progressBar.setVisibility(View.VISIBLE);
                    detectViewModel.postDetectedGesture("");
                    break;
            }
            if (captureStopButton != null && captureRestartButton != null) {
                switch (newState) {
                    case STARTING:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(false);
                        break;
                    case STARTED:
                        captureStopButton.setVisible(true);
                        captureRestartButton.setVisible(false);
                        break;
                    case STOPPING:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(false);
                        break;
                    case STOPPED:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(true);
                        break;
                }
            }
        }
    };
}