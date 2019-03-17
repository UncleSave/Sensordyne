package unclesave.example.com.test2;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class TensorFlowClassifier {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/model.pb";
    private static final String INPUT_NODE = "lstm_1_input";
    private static final String[] OUTPUT_NODES = {"dense_2/Softmax"};
    private static final String OUTPUT_NODE = "dense_2/Softmax";
    private static final long[] INPUT_SIZE = {1, 15, 15};
    private static final int OUTPUT_SIZE = 3;

    public TensorFlowClassifier(final Context context) {
        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public float[] predictProbabilities(float[] data) {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, data, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES, false);
        inferenceInterface.fetch(OUTPUT_NODE, result);
        return result;
    }
}
