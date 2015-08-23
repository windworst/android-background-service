package com.android.system.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LocationDb extends SQLiteOpenHelper {
    final private static String DB_NAME = "location";
    final private static String TABLE_NAME = "location";
    private static SQLiteDatabase sDatabase = null;
    public static void init(Context context) {
        sDatabase = new LocationDb(context, DB_NAME, null, 1).getWritableDatabase();
    }
    public LocationDb(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(longitude double, latitude double, time datetime)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS " + DB_NAME);
            onCreate(db);
        }
    }

    public static void save(double longitude, double latitude) {
        if(sDatabase==null) return;
        ContentValues values = new ContentValues();
        values.put("longitude", longitude);
        values.put("latitude", latitude);
        values.put("time", System.currentTimeMillis());
        sDatabase.insert(TABLE_NAME,null, values);
    }

    public static class LocationInfo {
        public double longitude,latitude;
        public String time;
    }
    public static List<LocationInfo> list() {
        List<LocationInfo> list = new ArrayList<>();
        try {
            Cursor cursor = sDatabase.query(TABLE_NAME, null, null, null, null, null, null, null);
            if(cursor == null || cursor.getCount() == 0 ) return list;
            while(cursor.moveToNext()) {
                LocationInfo info = new LocationInfo();
                info.longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                info.latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                info.time = cursor.getString(cursor.getColumnIndex("time"));
                list.add(info);
            }
        } catch (Exception e) {
        }
        return list;
    }
}
