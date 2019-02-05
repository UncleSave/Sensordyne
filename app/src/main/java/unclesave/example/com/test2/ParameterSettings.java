package unclesave.example.com.test2;

public class ParameterSettings {
    private String urllink;
    private String androidID;
    private String optimizer;
    private String loss;
    private String activation;
    private float dropout;
    private int batchSize;
    private int epoch;
    private String settingsInput;

    public String getUrllink() {
        return urllink;
    }

    public void setUrllink(String urllink) {
        this.urllink = urllink;
    }

    public String getAndroidID() { return androidID; }

    public void setAndroidID(String androidID) {
        this.androidID = androidID;
    }

    public String getOptimizer() { return optimizer; }

    public void setOptimizer(String optimizer) { this.optimizer = optimizer; }

    public String getLoss() { return loss; }

    public void setLoss(String loss) { this.loss = loss; }

    public String getActivation() { return activation; }

    public void setActivation(String activation) { this.activation = activation; }

    public float getDropout() { return dropout; }

    public void setDropout(float dropout) { this.dropout = dropout; }

    public int getBatchSize() { return batchSize; }

    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getEpoch() { return epoch; }

    public void setEpoch(int epoch) { this.epoch = epoch; }

    public String getSettingsInput() { return settingsInput; }

    public void setSettingsInput(String settingsInput) { this.settingsInput = settingsInput; }

    ParameterSettings(String urllink, String androidID, String optimizer, String loss,
                          String activation, float dropout, int batchSize, int epoch) {
        this.urllink = urllink;
        this.androidID = androidID;
        this.optimizer = optimizer;
        this.loss = loss;
        this.activation = activation;
        this.dropout = dropout;
        this.batchSize = batchSize;
        this.epoch = epoch;
        this.settingsInput = '/' + optimizer + ';' + loss + ';' + activation + ';' + dropout + ';' +
                batchSize + ';' + epoch;
    }

    public ParameterSettings() {
    }
}
