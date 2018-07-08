package unclesave.example.com.test2;

import android.provider.BaseColumns;

public class SensorDatabase {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SensorDatabase() {}

    /* Inner class that defines the table contents */
    public static class SensorDatabaseEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_ANDROID_ID = "android_id";
        public static final String COLUMN_NAME_ACC_X = "acc_x";
    }
}
