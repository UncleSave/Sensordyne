package unclesave.example.com.test2;

public class RequestClass {
    private long timeStamp;
    private double gyroscopeVal[];

    public long getTimeStamp() {
        return timeStamp;
    }

    public double[] getGyroscopeVal() {
        return gyroscopeVal;
    }

    public void setGyroscopeVal(double[] gyroscopeVal) {
        this.gyroscopeVal = gyroscopeVal;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public RequestClass(long timeStamp, double accelerometerVal[]) {
        this.timeStamp = timeStamp;
        this.gyroscopeVal = accelerometerVal;
    }

    public RequestClass() {
    }
}