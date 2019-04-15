package unclesave.example.com.sensordyne;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

// A helper class that checks for S3 cloud storage access
public class S3Handler {
    private Context context;
    private Activity activity;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private AmazonS3Client s3client;
    private String bucketName;
    private String androidID;


    public S3Handler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.credentialsProvider = new CognitoCachingCredentialsProvider(
                this.activity,
                "ap-southeast-1:3a7f3577-7580-4ee2-8a26-1e0f50d1019b", // Identity pool ID
                Regions.AP_SOUTHEAST_1 // Region
        );
        this.s3client = new AmazonS3Client(credentialsProvider);
        this.bucketName = "test-userfiles-mobilehub-1913606923";
        this.androidID = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public AmazonS3Client getS3client() { return s3client; }

    public String getBucketName() { return bucketName; }

    public String getAndroidID() { return androidID; }
}
