package fr.trinoma.myogesture;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.System.nanoTime;

public class RawDataLogger {

    public final static String TAG = "RawDataLogger";

    private final FileChannel fileChanel;

    private RawDataLogger(FileChannel fileChannel) {
        fileChanel = fileChannel;
    }

    public static RawDataLogger make(File file) throws IOException {
        return new RawDataLogger(new FileOutputStream(file).getChannel());
    }

    public void logRawData(ByteBuffer buffer) {
        try {
            String header = String.format(Locale.US, "\n%d/%d\n", nanoTime(), buffer.remaining());
            fileChanel.write(ByteBuffer.wrap(header.getBytes()));
            fileChanel.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Unable to log raw data", e);
        }
    }

    public void logMeta(String meta) {
        if (meta == null) {
            meta = "null";
        }
        logRawData(ByteBuffer.wrap(meta.getBytes()));
    }

    public void close() throws IOException {
        fileChanel.close();
    }
}
