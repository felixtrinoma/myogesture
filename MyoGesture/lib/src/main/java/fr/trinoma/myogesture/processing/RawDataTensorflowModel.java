package fr.trinoma.myogesture.processing;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import fr.trinoma.myogesture.interfaces.signal.ChannelReader;

public class RawDataTensorflowModel {

    private final static String TAG = "RawEmgTensorflowModel";

    private final float[] input;
    private final float[][] output;
    private final Interpreter model;

    public RawDataTensorflowModel(Interpreter model, List<Pair<ChannelReader, Integer>> channels, int outputSize) {
        int inputSize = 0;
        for (Pair<ChannelReader, Integer> channel : channels) {
            int size = channel.second;
            ChannelReader reader = channel.first;
            inputSize += size;
            bufferedChannels.add(new BufferedChannel(reader, size));
        }
        input = new float[inputSize];
        output = new float[1][outputSize];
        this.model = model;
    }

    public static Interpreter makeInterpreter(Context context, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        return new Interpreter(model, new Interpreter.Options());
    }

    private static class BufferedChannel {
        public final ChannelReader channelReader;
        public final RingBuffer buffer;
        BufferedChannel(ChannelReader channelReader, int size) {
            this.channelReader = channelReader;
            buffer = new RingBuffer(size);
        }
        void addDataFrom(ByteBuffer dataFrame) {
            for (int i = 0; i < channelReader.getCount(dataFrame); i++) {

                float value = channelReader.get(dataFrame, i);
                if (Float.isNaN(value)) {
                    value = 0.0f;
                } else {
                    value = value / 0.0002f; // FIXME extract normalization
                }
                buffer.add(value);
            }
        }
    }

    private final LinkedList<BufferedChannel> bufferedChannels = new LinkedList<>();

    public void feed(ByteBuffer dataFrame) {
        synchronized (bufferedChannels) {
            for (BufferedChannel bufferedChannel : bufferedChannels) {
                bufferedChannel.addDataFrom(dataFrame);
            }
        }
    }

    public float[] run() {
        synchronized (bufferedChannels) {
            int pos = 0;
            for (BufferedChannel bufferedChannel : bufferedChannels) {
                bufferedChannel.buffer.flattenTo(input, pos);
                pos += bufferedChannel.buffer.size();
            }
        }
        model.run(input, output);
        return output[0]; // TODO return a copy
    }

}
