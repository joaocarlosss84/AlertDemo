package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper mInstance = null;

    private static final String DATABASE_NAME = "alerts.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "alerts_table";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "TIME";
    private static final String COL_3 = "DURATION";
    private static final String COL_4 = "WEEKDAYS";



    public DatabaseHelper(Context context) throws SQLException {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("Create table "+TABLE_NAME+" (ID INTERGER PRIMARY KEY AUTOINCREMENT," +
                "TIME TEXT, DURATION TEXT, WEEKDAYS TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }
}

