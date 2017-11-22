package net.unitecgroup.www.unitecrfid;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import static android.content.ContentValues.TAG;

/**
 * Created by 20006030 on 12/06/2017.
 * To avoid multiple instances of the database, the helper must be static
 * https://stackoverflow.com/questions/18147354/sqlite-connection-leaked-although-everything-closed/18148718#18148718
 */

public class DatabaseTable extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "alerts.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + AlertEntry.TABLE_NAME + " (" +
                    AlertEntry._ID + " INTEGER PRIMARY KEY," +
                    AlertEntry.COL_TIME + TEXT_TYPE + COMMA_SEP +
                    AlertEntry.COL_DURATION + TEXT_TYPE + COMMA_SEP +
                    AlertEntry.COL_WEEKDAYS + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AlertEntry.TABLE_NAME;

    private final Context mHelperContext;

    public DatabaseTable(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mHelperContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE_ENTRIES);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseTable", "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");

        try {
            db.execSQL(SQL_DELETE_ENTRIES);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public void deleteAll() {
        SQLiteDatabase db = getReadableDatabase();
        db.delete(AlertEntry.TABLE_NAME, null, null);
        db.close();
    }

    public static class AlertEntry implements BaseColumns {
        public static final String TABLE_NAME = "ALERTS";
        public static final String COL_TIME = "TIME";
        public static final String COL_DURATION = "DURATION";
        public static final String COL_WEEKDAYS = "WEEKDAYS";
    }

    public Cursor getWordMatches(String query, String[] columns) {
        String selection = AlertEntry.COL_TIME + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(AlertEntry.TABLE_NAME);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = builder.query(db,
                columns, selection, selectionArgs, null, null, null);


        if (!cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return null;
        }
        db.close();
        return cursor;
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
    //String time, String duration, String weekdays
    public long addAlert(Alert alert) {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        // Insert the new row, returning the primary key value of the new row
        long id = -1;

        // Create a new map of values, where column names are the keys
        ContentValues initialValues = new ContentValues();
        initialValues.put(AlertEntry.COL_TIME, alert.get_time());
        initialValues.put(AlertEntry.COL_DURATION, alert.get_duration());
        initialValues.put(AlertEntry.COL_WEEKDAYS, alert.get_weekdays().toString());
        if (alert.get_id() > 0) {
            id = alert.get_id();
            initialValues.put(AlertEntry._ID, alert.get_id());
        }

        try {
            id = db.insert(AlertEntry.TABLE_NAME, null, initialValues);
            alert.set_id((int) id);
        } finally {
            //db.close();
        }


        return id;
    }

    // Getting single alert
    Alert getAlert(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                AlertEntry._ID,
                AlertEntry.COL_TIME,
                AlertEntry.COL_DURATION,
                AlertEntry.COL_WEEKDAYS
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = AlertEntry._ID + " = ?";
        String[] selectionArgs = { Integer.toString(id) };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                AlertEntry.COL_TIME + " DESC";

        Cursor c = db.query(
                AlertEntry.TABLE_NAME,                    // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        Alert alert = null;

        try {
            if (c != null) {
                c.moveToFirst();

                int itemId = c.getInt(
                        c.getColumnIndexOrThrow(AlertEntry._ID)
                );

                ArrayList<Integer> weekdays = arrayStringToIntegerArrayList(c.getString(3));

                alert = new Alert(itemId,
                        c.getString(1), c.getString(2), weekdays);
            }
        } finally {
            c.close();
            db.close();
        }
        return alert;
    }

    // Updating single alert
    public boolean updateAlert(Alert alert) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(AlertEntry.COL_TIME, alert.get_time());
        values.put(AlertEntry.COL_DURATION, alert.get_duration());
        values.put(AlertEntry.COL_WEEKDAYS, alert.get_weekdays().toString());

        // Which row to update, based on the time
        //String selection = AlertEntry.COL_TIME + " LIKE ?";
        //String[] selectionArgs = { alert.get_time() };

        // Which row to update, based on the id
        String selection = AlertEntry._ID + " = ?";
        String[] selectionArgs = { Integer.toString(alert.get_id()) };

        int count = -1;

        try {
            count = db.update(
                    AlertEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);
        } finally {
            db.close();
        }

        // updating row
        return (count == 1);
    }

    // Deleting single alert
    public boolean deleteAlert(Alert alert) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Define 'where' part of query.
        //String selection = AlertEntry.COLUMN_NAME_TITLE + " LIKE ?";
        String selection = AlertEntry._ID + " = ?";

        // Specify arguments in placeholder order.
        //String[] selectionArgs = { "MyTitle" };
        String[] selectionArgs = { Integer.toString(alert.get_id()) };
        // Issue SQL statement.
        int rows = 0;

        try {
            rows = db.delete(AlertEntry.TABLE_NAME, selection, selectionArgs);
        } finally {
            db.close();
        }

        return (rows == 1);
    }

    // Getting All alert
    public ArrayList<Alert> getAllAlerts() {
        ArrayList<Alert> alertList = new ArrayList<Alert>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + AlertEntry.TABLE_NAME;


        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Cursor c = db.rawQuery(selectQuery, null);

        try {
            // looping through all rows and adding to list
            if (c.moveToFirst()) {
                do {
                    Alert alert = new Alert();
                    alert.set_id(Integer.parseInt(c.getString(0)));
                    alert.set_time(c.getString(1));
                    alert.set_duration(c.getString(2));
                    //alert.set_weekdays(c.getString(3));
                    ArrayList<Integer> weekdays = arrayStringToIntegerArrayList(c.getString(3));
                    alert.set_weekdays(weekdays);
                    // Adding contact to list
                    alertList.add(alert);
                } while (c.moveToNext());
            }
        } finally {
            c.close();
            db.close();
        }

        // return contact list
        return alertList;
    }

    // Getting contacts Count
    public int getAlertsCount() {
        String countQuery = "SELECT  * FROM " + AlertEntry.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int iCount = -1;

        try {
            iCount = cursor.getCount();
            cursor.close();
            db.close();
        } finally {
            cursor.close();
            db.close();
        }

        return iCount;
    }

    //Populate Database from file
    public void loadDictionary() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    loadAlerts();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void loadAlerts() throws IOException {
        final Resources resources = mHelperContext.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.alerts);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] strings = TextUtils.split(line, "-");
                if (strings.length < 3) continue;
                ArrayList<Integer> weekdays = arrayStringToIntegerArrayList(strings[2].trim());
                long id = addAlert(new Alert(0, strings[0].trim(), strings[1].trim(), weekdays));
                if (id < 0) {
                    Log.e(TAG, "unable to add word: " + strings[0].trim());
                }
            }
        } finally {
            reader.close();
        }
    }

    //Creates a ArrayList<Integer> from String "[1,2,3]"
    public static ArrayList<Integer> arrayStringToIntegerArrayList(String arrayString){
        String removedBrackets = arrayString.substring(1, arrayString.length() - 1);
        String[] individualNumbers = removedBrackets.split(",");
        ArrayList<Integer> integerArrayList = new ArrayList<>();
        for(String numberString : individualNumbers){
            integerArrayList.add(Integer.parseInt(numberString.trim()));
        }
        Collections.sort(integerArrayList);
        return integerArrayList;
    }
}
