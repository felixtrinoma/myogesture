package fr.trinoma.myogesture.ui.train;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.trinoma.myogesture.Config;
import fr.trinoma.myogesture.FileProvider;
import fr.trinoma.myogesture.MainActivity;
import fr.trinoma.myogesture.R;
import fr.trinoma.myogesture.RawDataLogger;
import fr.trinoma.myogesture.delsys.DelsysApi;
import fr.trinoma.myogesture.interfaces.DataAcquisitionApi;
import fr.trinoma.myogesture.interfaces.device.Device;

public class TrainFragment extends Fragment {

    private TrainViewModel trainViewModel;

    private static final String TAG = "TrainFragment";

    private LineChart chart;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private DataAcquisitionApi dataAcquisition;
    private FileProvider fileProvider;

    // Get a handler that can be used to post to the main thread
    Handler mainHandler = new Handler(Looper.getMainLooper());

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        trainViewModel =
                ViewModelProviders.of(this).get(TrainViewModel.class);
        View root = inflater.inflate(R.layout.fragment_train, container, false);

        chart = root.findViewById(R.id.record_chart);
        progressBar = root.findViewById(R.id.record_progressbar);
        statusTextView = root.findViewById(R.id.record_status);
        setupChart();

        setHasOptionsMenu(true);

        trainViewModel.getState().observe(this, uiStateUpdater);
        trainViewModel.postState(RecordingState.STARTING);

        dataAcquisition = MainActivity.gestureRecognition;
        fileProvider = MainActivity.fileProvider;

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
        super.onCreateOptionsMenu(menu,inflater);
    }

    private File labelsFile = null;

    private class RecordingThread extends Thread {

        long sampleId = 0;

        private void record() throws IOException {
            chart.getLineData().clearValues();
            final List<Device> devices = dataAcquisition.getConnectedDevices();
            for (int i = 0; i < devices.size(); ++i) {
                // TODO set device nickname / label / serial as description
                chart.getLineData().addDataSet(createLineDataSet(Integer.toString(i), LINE_COLORS[i]));
            }

            // Get a handler that can be used to post to the main thread
            Handler mainHandler = new Handler(getContext().getMainLooper());

            Runnable invalidateChart = new Runnable() {
                @Override
                public void run() {
                    chart.notifyDataSetChanged();
                    chart.invalidate();
                }
            };

            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            String date = formatter.format(new Date(System.currentTimeMillis()));
            File rawDataFile = fileProvider.get(date + ".dat");
            labelsFile = fileProvider.get(date + ".labels.txt");

            RawDataLogger dataLogger = RawDataLogger.make(rawDataFile);

            dataAcquisition.start();

            ByteBuffer buf = ByteBuffer.allocate(1024);
            while (!isInterrupted()) {
                buf.clear();
                int ret = dataAcquisition.read(buf);
                buf.flip();
                if (ret < 0 || buf.remaining() < devices.size() * 10 * 8) {
                    Log.i(TAG, "Not enough bytes to read: " + buf.remaining());
                    Log.i(TAG, "read() returned: " + ret);
                    dataLogger.close();
                    return;
                }

                dataLogger.logRawData(buf);

                buf.rewind();

                LineData data = chart.getLineData();
                for (int i = 0; i < 10; ++i) {
                    for (int device = 0; device < devices.size(); ++device) {
                        float value = (float) buf.getDouble((device * 10 + i) * 8);
                        if (Float.isNaN(value)) {
                            value = 0.0f;
                        }
                        data.addEntry(new Entry((sampleId + i) * 0.003f, value), device);
                    }
                }
                sampleId += 10;
                mainHandler.post(invalidateChart);
                trainViewModel.postState(RecordingState.LOGGING);
            }
        }

        @Override
        public void run() {
            trainViewModel.postState(RecordingState.STARTING);
            try {
                record();
            } catch (IOException e) {
                Log.e(TAG, "Unable to record data", e);
                e.printStackTrace();
            } finally {
                trainViewModel.postState(RecordingState.LABELING);
            }
        }
    }

    void startRecording() {
        Log.i(TAG, "Starting record");
        String error = null;
        if (dataAcquisition.getState() == DataAcquisitionApi.State.STARTED) {
            error = "Unable to record: Previous capture not finished.";
        }
        else if (dataAcquisition.getState() != DataAcquisitionApi.State.READY) {
            error = "Unable to record: Not configured.";
        }
        if (error == null) {
            new RecordingThread().start();
        } else {
            Log.e(TAG, "Error");
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            // TODO go back to devices
            NavDirections action = TrainFragmentDirections.actionNavigationRecordToNavigationDevices();
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    void stopRecording() {
        trainViewModel.postState(RecordingState.STOPPING);
        new Thread() {
            @Override
            public void run() {
                dataAcquisition.stop();
            }
        }.start();
    }

    private void setupChart() {
        chart.setData(new LineData());

        chart.setTouchEnabled(true);
        chart.setOnChartValueSelectedListener(chartValueSelectedListener);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.format(Locale.US, "%.3f", value);
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(true);
    }

    private ActionMode labelizeActionMode;
    private int selectedEntryIndex;
    private int selectedGestureIndex;

    private final OnChartValueSelectedListener chartValueSelectedListener = new OnChartValueSelectedListener() {

        @Override
        public void onValueSelected(Entry e, Highlight h) {

            if (trainViewModel.getState().getValue() != RecordingState.LABELING) {
                return;
            }

            ILineDataSet set = chart.getLineData().getDataSetByIndex(h.getDataSetIndex());
            selectedEntryIndex = set.getEntryIndex(e);

            float stopAt = set.getEntryForIndex(selectedEntryIndex).getX() + Config.GESTURE_DURATIION;
            Entry endEntry = null;
            for (int i = selectedEntryIndex; i < set.getEntryCount(); i++) {
                endEntry = set.getEntryForIndex(i);
                if (endEntry.getX() > stopAt) {
                    break;
                }
            }

            Highlight endHightlight = new Highlight(endEntry.getX(), endEntry.getY(), h.getDataSetIndex());
            chart.highlightValues(new Highlight[]{h, endHightlight});

            if (labelizeActionMode != null) {
                return;
            }

            labelizeActionMode = ((MainActivity)getActivity()).startSupportActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.labelize_menu, menu);
                    mode.setTitle("Labelize");
                    MenuItem item = menu.findItem(R.id.labels_spinner);
                    Spinner spinner = (Spinner) item.getActionView();
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, Config.GESTURES);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(arrayAdapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // TODO style properly, label was black
                            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                            selectedGestureIndex = position;
                        }
                        @Override
                        public void onNothingSelected(AdapterView <?> parent) {
                        }
                    });
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.labelize_save_btn:
                            try (BufferedWriter out = new BufferedWriter(new FileWriter(labelsFile, true))) {
                                out.write(String.format(Locale.US, "%d, %d\n", selectedEntryIndex, selectedGestureIndex));
                                Toast.makeText(getContext(), "Label saved", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to log label", e);
                            }
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    labelizeActionMode = null;
                }
            });
        }

        @Override
        public void onNothingSelected() {
            selectedEntryIndex = -1;
        }
    };

    // region chart helper methods
    private static final int[] LINE_COLORS = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF000000};

    private static LineDataSet createLineDataSet(String description, int color) {
        LineDataSet set = new LineDataSet(null, description);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setDrawCircleHole(false);
        set.setLineWidth(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.BLACK);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawHighlightIndicators(true);
        set.setDrawIcons(false);
        set.setDrawHorizontalHighlightIndicator(false);
        set.setDrawFilled(false);
        return set;
    }

    final Observer<RecordingState> uiStateUpdater = new Observer<RecordingState>() {
        @Override
        public void onChanged(@Nullable final RecordingState newState) {
            switch (newState) {
                case STARTING:
                    chart.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    statusTextView.setVisibility(View.VISIBLE);
                    break;
                case LOGGING:
                    chart.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setVisibility(View.GONE);
                    break;
                case STOPPING:
                    chart.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    statusTextView.setVisibility(View.GONE);
                    break;
                case LABELING:
                    chart.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setVisibility(View.GONE);
                    break;
            }
            if (captureStopButton != null && captureRestartButton != null) {
                switch (newState) {
                    case STARTING:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(false);
                        break;
                    case LOGGING:
                        captureStopButton.setVisible(true);
                        captureRestartButton.setVisible(false);
                        break;
                    case STOPPING:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(false);
                        break;
                    case LABELING:
                        captureStopButton.setVisible(false);
                        captureRestartButton.setVisible(true);
                        break;
                }
            }
        }
    };
}