package unclesave.example.com.test2;

public class RequestClass {
    private String urllink;
    private String androidID;

    public String getUrllink() {
        return urllink;
    }

    public void setUrllink(String urllink) {
        this.urllink = urllink;
    }

    public String getAndroidID() {
        return androidID;
    }

    public void setAndroidID(String androidID) {
        this.androidID = androidID;
    }

    public RequestClass(String urllink, String androidID) {
        this.urllink = urllink;
        this.androidID = androidID;
    }
    public RequestClass() {
    }
}