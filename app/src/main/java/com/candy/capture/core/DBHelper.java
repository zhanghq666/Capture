package com.candy.capture.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.candy.capture.model.Content;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String CONTENT_TABLE_NAME = "content";
    public static final String CITY_TABLE_NAME = "city";
    public static final String SEARCH_HISTORY_TABLE_NAME = "search_history";

    public static final String CONTENT_COLUMN_ID = "_id";
    public static final String CONTENT_COLUMN_TYPE = "type";
    public static final String CONTENT_COLUMN_DESC = "desc";
    public static final String CONTENT_COLUMN_FILE_PATH = "file_path";
    public static final String CONTENT_COLUMN_CITY_NAME = "city_name";
    public static final String CONTENT_COLUMN_MEDIA_DURATION = "media_duration";
    public static final String CONTENT_COLUMN_RELEASE_TIME = "release_time";

    public static final String CITY_COLUMN_CITY_NAME = "city_name";
    public static final String CITY_COLUMN_CONTENT_COUNT = "content_count";

    public static final String SEARCH_HISTORY_COLUMN_ID = "_id";
    public static final String SEARCH_HISTORY_COLUMN_WORD = "word";

    private static final String DB_NAME = "capture.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createContentTable(db);
        createCityTable(db);
        createSearchHistoryTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    private void createContentTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + "content");
        db.execSQL("CREATE TABLE " + CONTENT_TABLE_NAME + " (" +
                CONTENT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CONTENT_COLUMN_TYPE + " INTEGER," +
                CONTENT_COLUMN_DESC + " TEXT," +
                CONTENT_COLUMN_FILE_PATH + " TEXT," +
                CONTENT_COLUMN_CITY_NAME + " TEXT," +
                CONTENT_COLUMN_MEDIA_DURATION + " INTEGER," +
                CONTENT_COLUMN_RELEASE_TIME + " INTEGER," +
                "FOREIGN KEY(" + CONTENT_COLUMN_CITY_NAME + ") REFERENCES " + CITY_TABLE_NAME + "(" + CITY_COLUMN_CITY_NAME + ")" +
                ");");
    }

    private void createCityTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + CITY_TABLE_NAME);
        db.execSQL("CREATE TABLE " + CITY_TABLE_NAME + " (" +
                CITY_COLUMN_CITY_NAME + " TEXT PRIMARY KEY," +
                CITY_COLUMN_CONTENT_COUNT + " INTEGER" +
                ");");
    }

    private void createSearchHistoryTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SEARCH_HISTORY_TABLE_NAME);
        db.execSQL("CREATE TABLE " + SEARCH_HISTORY_TABLE_NAME + " (" +
                SEARCH_HISTORY_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SEARCH_HISTORY_COLUMN_WORD + " TEXT"
                + ");");
    }

    public boolean insertContent(Content content) {
        if (content == null) {
            return false;
        }
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            incrementCity(content.getCityName(), database);

            ContentValues contentValues = new ContentValues();
            contentValues.put(CONTENT_COLUMN_TYPE, content.getType());
            contentValues.put(CONTENT_COLUMN_DESC, content.getDesc());
            contentValues.put(CONTENT_COLUMN_CITY_NAME, content.getCityName());
            contentValues.put(CONTENT_COLUMN_FILE_PATH, content.getMediaFilePath());
            contentValues.put(CONTENT_COLUMN_MEDIA_DURATION, content.getMediaDuration());
            contentValues.put(CONTENT_COLUMN_RELEASE_TIME, content.getReleaseTime());
            database.insert(CONTENT_TABLE_NAME, null, contentValues);

            database.setTransactionSuccessful();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
    }

    public ArrayList<Content> getContentOlder(int startId, int count) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor;
        if (startId > -1) {
            cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                            " where " + CONTENT_COLUMN_ID + "<" + startId +
                            " order by " + CONTENT_COLUMN_ID + " desc" +
                            " limit " + count + ";",
                    new String[]{});
        } else {
//            cursor = database.query(CONTENT_TABLE_NAME, null, null, null, null, null, CONTENT_COLUMN_ID + " DESC", "20");
            cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", null);
        }
        ArrayList<Content> contents = new ArrayList<>();
        fitContents(cursor, contents);

        return contents;
    }

    public ArrayList<Content> getContentNewer(int startId, int count) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor;
        if (startId > -1) {
            cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                            " where " + CONTENT_COLUMN_ID + ">" + startId +
                            " order by " + CONTENT_COLUMN_ID + " desc" +
                            " limit " + count + ";",
                    new String[]{});
        } else {
//            cursor = database.query(CONTENT_TABLE_NAME, null, null, null, null, null, CONTENT_COLUMN_ID + " DESC", "20");
            cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", null);
        }
        ArrayList<Content> contents = new ArrayList<>();
        fitContents(cursor, contents);

        return contents;
    }

    private void fitContents(Cursor cursor, ArrayList<Content> contents) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_ID));
                    int type = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_TYPE));
                    String desc = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_DESC));
                    String filePath = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_FILE_PATH));
                    String cityName = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_CITY_NAME));
                    int mediaDuration = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_MEDIA_DURATION));
                    int releaseTime = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_RELEASE_TIME));
                    Content content = new Content();
                    content.setId(id);
                    content.setType(type);
                    content.setDesc(desc);
                    content.setMediaFilePath(filePath);
                    content.setCityName(cityName);
                    content.setMediaDuration(mediaDuration);
                    content.setReleaseTime(releaseTime);

                    contents.add(content);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
    }

    public boolean deleteContent(ArrayList<Content> contentList) {
        if (contentList == null)
            return false;
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (Content content : contentList) {
                database.delete(CONTENT_TABLE_NAME, CONTENT_COLUMN_ID + "=?", new String[]{String.valueOf(content.getId())});

                decrementCity(content.getCityName(), database);
            }
            database.setTransactionSuccessful();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
    }

    private void incrementCity(String cityName, SQLiteDatabase database) {
        if (TextUtils.isEmpty(cityName))
            return;
        Cursor cursor = database.query(CITY_TABLE_NAME, null, CITY_COLUMN_CITY_NAME + "=?", new String[]{cityName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndex(CITY_COLUMN_CONTENT_COUNT));
            ContentValues contentValues = new ContentValues();
            contentValues.put(CITY_COLUMN_CITY_NAME, cityName);
            contentValues.put(CITY_COLUMN_CONTENT_COUNT, ++count);
            database.update(CITY_TABLE_NAME, contentValues, CITY_COLUMN_CITY_NAME + "=?", new String[]{cityName});
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(CITY_COLUMN_CITY_NAME, cityName);
            contentValues.put(CITY_COLUMN_CONTENT_COUNT, 1);
            database.insert(CITY_TABLE_NAME, null, contentValues);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    public ArrayList<String> getAllCity() {
        ArrayList<String> cityList = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(CITY_TABLE_NAME, null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String city = cursor.getString(cursor.getColumnIndex(CITY_COLUMN_CITY_NAME));
                    cityList.add(city);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return cityList;
    }

    private void decrementCity(String cityName, SQLiteDatabase database) {
        if (TextUtils.isEmpty(cityName))
            return;
        Cursor cursor = database.query(CITY_TABLE_NAME, null, CITY_COLUMN_CITY_NAME + "=?", new String[]{cityName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndex(CITY_COLUMN_CONTENT_COUNT));
            if (--count > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(CITY_COLUMN_CITY_NAME, cityName);
                contentValues.put(CITY_COLUMN_CONTENT_COUNT, count);
                database.update(CITY_TABLE_NAME, contentValues, CITY_COLUMN_CITY_NAME + "=?", new String[]{cityName});
            } else {
                database.delete(CITY_TABLE_NAME, CITY_COLUMN_CITY_NAME + "=?", new String[]{cityName});
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }
}
