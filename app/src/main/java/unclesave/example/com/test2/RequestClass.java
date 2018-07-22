package unclesave.example.com.test2;

public class RequestClass {
    long timeStamp;
    double accelerometerVal[];
    double gyroscopeVal[];

    public long getTimeStamp() {
        return timeStamp;
    }

    public double[] getAccelerometerVal() {
        return accelerometerVal;
    }

    public void setAccelerometerVal(double[] accelerometerVal) {
        this.accelerometerVal = accelerometerVal;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double[] getGyroscopeVal() {
        return gyroscopeVal;
    }

    public void setGyroscopeVal(double[] gyroscopeVal) {
        this.gyroscopeVal = gyroscopeVal;
    }

    public RequestClass(long timeStamp, double accelerometerVal[], double gyroscopeVal[]) {
        this.timeStamp = timeStamp;
        this.accelerometerVal = accelerometerVal;
        this.gyroscopeVal = gyroscopeVal;
    }

    public RequestClass() {
    }
}