package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 20006030 on 12/06/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "alerts.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "alerts_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "TIME";
    public static final String COL_3 = "DURATION";
    public static final String COL_4 = "WEEKDAYS";

    public DatabaseHelper(Context context) {
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

