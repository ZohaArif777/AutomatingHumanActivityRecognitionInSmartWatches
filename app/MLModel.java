import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MLModel {
        private Interpreter interpreter;

        public MLModel(Context context) throws IOException {
            interpreter = new Interpreter(loadModelFile(context));
        }

        private MappedByteBuffer loadModelFile(Context context) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd("model.tflite").getFileDescriptor());
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = context.getAssets().openFd("model.tflite").getStartOffset();
            long declaredLength = context.getAssets().openFd("model.tflite").getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }

        public float[] predict(float[] input) {
            float[][] output = new float[1][24]; // Assuming output shape is [1, 24]
            interpreter.run(input, output);
            return output[0];
        }

        public void close() {
            interpreter.close();
        }
    }

