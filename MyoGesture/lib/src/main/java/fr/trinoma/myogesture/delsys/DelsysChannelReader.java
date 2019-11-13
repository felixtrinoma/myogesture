package fr.trinoma.myogesture.delsys;

import java.nio.ByteBuffer;

import fr.trinoma.myogesture.interfaces.signal.ChannelReader;
import fr.trinoma.myogesture.interfaces.signal.SampledSignalType;

public class DelsysChannelReader implements ChannelReader {

    private final int count;
    private final int offset;
    private final SampledSignalType signalType;

    DelsysChannelReader(int offset, int count, SampledSignalType signalType) {
        this.count = count;
        this.offset = offset;
        this.signalType = signalType;
    }

    @Override
    public int getCount(ByteBuffer buf) {
        return count;
    }

    @Override
    public float get(ByteBuffer buf, int i) {
        return (float) buf.getDouble((offset + i) * 8);
    }

    @Override
    public SampledSignalType getSignalType() {
        return signalType;
    }
}
