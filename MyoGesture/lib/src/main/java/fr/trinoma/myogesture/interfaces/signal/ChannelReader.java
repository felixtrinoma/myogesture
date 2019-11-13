package fr.trinoma.myogesture.interfaces.signal;

import java.nio.ByteBuffer;

public interface ChannelReader {

    int getCount(ByteBuffer buf);

    float get(ByteBuffer buf, int i);

    SampledSignalType getSignalType();
}
