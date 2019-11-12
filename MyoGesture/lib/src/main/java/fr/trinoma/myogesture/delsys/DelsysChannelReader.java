package fr.trinoma.myogesture.delsys;

import java.nio.ByteBuffer;

import fr.trinoma.myogesture.interfaces.signal.ChannelReader;
import fr.trinoma.myogesture.interfaces.signal.SignalType;

public class DelsysChannelReader implements ChannelReader {

    private final int count;
    private final int offset;
    private final SignalType signalType;

    DelsysChannelReader(int offset, int count, SignalType signalType) {
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
    public SignalType getSignalType() {
        return signalType;
    }
}
