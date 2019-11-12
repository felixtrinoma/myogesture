package fr.trinoma.myogesture;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void byteArrayToFloatArray() {
        System.out.println("Starting some test");
        byte[] bytes = new byte[4*3];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int i = 0; i < 3; ++i) {
            buf.putFloat(i);
        }
        float[] floats = buf.asFloatBuffer().array();
        for (float f : floats) {
            System.out.println(f);
        }
//        FloatBuffer.allocate(10);
    }
}