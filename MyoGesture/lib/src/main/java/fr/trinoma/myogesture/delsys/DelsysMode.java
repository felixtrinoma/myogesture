package fr.trinoma.myogesture.delsys;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fr.trinoma.myogesture.interfaces.device.Mode;
import fr.trinoma.myogesture.interfaces.signal.BufferedSignalType;
import fr.trinoma.myogesture.interfaces.signal.EmgSignalType;
import fr.trinoma.myogesture.interfaces.signal.RmsEmgSignalType;
import fr.trinoma.myogesture.interfaces.signal.SampledSignalType;
import fr.trinoma.myogesture.interfaces.signal.SignalType;

public class DelsysMode implements Mode {

    private static HashMap<String, Mode> sModes;

    static class DelsysSignalType implements SampledSignalType, BufferedSignalType {
        private final int mSamplesPerFrame;
        private final float mSampleInterval;

        public DelsysSignalType(float sampleInterval, int samplesPerFrame) {
            mSampleInterval = sampleInterval;
            mSamplesPerFrame = samplesPerFrame;
        }

        @Override
        final public int getSamplesPerFrame() {
            return mSamplesPerFrame;
        }

        @Override
        final public int getSizeOfSample() {
            return 8; // Delsys API returns 8 bytes doubles
        }

        @Override
        final public float getSampleInterval() {
            return mSampleInterval;
        }
    }

    static class DelsysRmsEmgSignalType extends DelsysSignalType implements RmsEmgSignalType {
        public DelsysRmsEmgSignalType(float sampleInterval, int samplesPerFrame) {
            super(sampleInterval, samplesPerFrame);
        }
    }

    static class DelsysEmgSignalType extends DelsysSignalType implements EmgSignalType {
        public DelsysEmgSignalType(float sampleInterval, int samplesPerFrame) {
            super(sampleInterval, samplesPerFrame);
        }
    }

    public final static RmsEmgSignalType RMS_EMG_SIGNAL_TYPE =
            new DelsysRmsEmgSignalType(0.003f, 5);

    public final static EmgSignalType EMG_1000HZ_SIGNAL_TYPE =
            new DelsysEmgSignalType(0.001f, 15);

    public final static EmgSignalType EMG_2000HZ_SIGNAL_TYPE =
            new DelsysEmgSignalType(0.0005f, 30);

    public static Mode get(String description) {
        if (sModes == null) {
            sModes = new HashMap<>();
        }
        if (!sModes.containsKey(description)) {
            sModes.put(description, makeFromDescription(description));
        }
        return sModes.get(description);
    }

    private static Mode makeFromDescription(final String description) {
        final List<SignalType> signalTypes = new LinkedList<>();
        if (description.contains("RMS")) {
            signalTypes.add(RMS_EMG_SIGNAL_TYPE);
        } else if (description.contains("EMG")) {
            if (description.contains("1000")) {
                signalTypes.add(EMG_1000HZ_SIGNAL_TYPE);
            } else if (description.contains("2000")) {
                signalTypes.add(EMG_2000HZ_SIGNAL_TYPE);
            } else {
                // TODO check default EMG signal type
                signalTypes.add(EMG_1000HZ_SIGNAL_TYPE);
            }
        }
        return new DelsysMode(signalTypes, description);
    }

    private final List<SignalType> mSignalTypes;
    private final String mDescription;

    private DelsysMode(List<SignalType> signalTypes, String description) {
        mDescription = description;
        mSignalTypes = signalTypes;
    }

    @Override
    public List<SignalType> getSignalTypes() {
        return mSignalTypes;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }
}
