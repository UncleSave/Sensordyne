package unclesave.example.com.sensordyne;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/* A helper class which predicts the outcome of a series of records that sensor collected
The class is used within PredictGestureActivity and PredictMorseCodeActivity */
public class TensorFlowClassifier {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;
    private String modelFile;
    private String inputNode;
    private String[] outputNodes;
    private String outputNode;
    private long[] inputSize;
    private int outputSize;

    public TensorFlowClassifier(final Context context, String modelFile,
                                String inputNode, String[] outputNodes,
                                String outputNode, long[] inputSize,
                                int outputSize) {
        this.modelFile = modelFile;
        this.inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), this.modelFile);
        this.inputNode = inputNode;
        this.outputNodes = outputNodes;
        this.outputNode = outputNode;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public float[] predictProbabilities(float[] data) {
        float[] result = new float[this.outputSize];
        this.inferenceInterface.feed(this.inputNode, data, this.inputSize);
        this.inferenceInterface.run(this.outputNodes, false);
        this.inferenceInterface.fetch(this.outputNode, result);
        return result;
    }
}
