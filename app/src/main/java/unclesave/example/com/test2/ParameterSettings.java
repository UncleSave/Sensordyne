package unclesave.example.com.test2;

public class ParameterSettings {
    private int batchSize;
    private int epoch;
    private int timeStep;
    private String settingsInput;

    public String getSettingsInput() { return settingsInput; }

    ParameterSettings(int batchSize, int epoch, int timeStep) {
        this.batchSize = batchSize;
        this.epoch = epoch;
        this.timeStep = timeStep;
        this.settingsInput = '/' + Integer.toString(batchSize) + ';' + Integer.toString(epoch) + ';' + Integer.toString(timeStep);
    }
}
