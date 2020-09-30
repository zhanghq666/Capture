package com.candy.capture.core

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import com.candy.capture.model.Content
import com.candy.commonlibrary.utils.LogUtil
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:19
 */
class DBHelper(context: Context?): SQLiteOpenHelper(context, "capture.db", null, 1) {
    val CONTENT_TABLE_NAME = "content"
    val CITY_TABLE_NAME = "city"
    val SEARCH_HISTORY_TABLE_NAME = "search_history"

    val CONTENT_COLUMN_ID = "_id"
    val CONTENT_COLUMN_TYPE = "type"
    val CONTENT_COLUMN_DESC = "desc"
    val CONTENT_COLUMN_FILE_PATH = "file_path"
    val CONTENT_COLUMN_CITY_NAME = "city_name"
    val CONTENT_COLUMN_MEDIA_DURATION = "media_duration"
    val CONTENT_COLUMN_RELEASE_TIME = "release_time"

    val CITY_COLUMN_CITY_NAME = "city_name"
    val CITY_COLUMN_CONTENT_COUNT = "content_count"

    val SEARCH_HISTORY_COLUMN_ID = "_id"
    val SEARCH_HISTORY_COLUMN_WORD = "word"

    override fun onCreate(db: SQLiteDatabase) {
        createContentTable(db)
        createCityTable(db)
        createSearchHistoryTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    //region 内容相关
    private fun createContentTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + "content")
        db.execSQL("CREATE TABLE " + CONTENT_TABLE_NAME + " (" +
                CONTENT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CONTENT_COLUMN_TYPE + " INTEGER," +
                CONTENT_COLUMN_DESC + " TEXT," +
                CONTENT_COLUMN_FILE_PATH + " TEXT," +
                CONTENT_COLUMN_CITY_NAME + " TEXT," +
                CONTENT_COLUMN_MEDIA_DURATION + " INTEGER," +
                CONTENT_COLUMN_RELEASE_TIME + " INTEGER," +
                "FOREIGN KEY(" + CONTENT_COLUMN_CITY_NAME + ") REFERENCES " + CITY_TABLE_NAME + "(" + CITY_COLUMN_CITY_NAME + ")" +
                ");")
    }

    fun insertContent(content: Content?): Boolean {
        if (content == null) {
            return false
        }
        val database = writableDatabase
        database.beginTransaction()
        val rowId: Long
        try {
            incrementCity(content.cityName, database)
            val contentValues = ContentValues()
            contentValues.put(CONTENT_COLUMN_TYPE, content.type)
            contentValues.put(CONTENT_COLUMN_DESC, content.desc)
            contentValues.put(CONTENT_COLUMN_CITY_NAME, content.cityName)
            contentValues.put(CONTENT_COLUMN_FILE_PATH, content.mediaFilePath)
            contentValues.put(CONTENT_COLUMN_MEDIA_DURATION, content.mediaDuration)
            contentValues.put(CONTENT_COLUMN_RELEASE_TIME, content.releaseTime)
            rowId = database.insert(CONTENT_TABLE_NAME, null, contentValues)
            database.setTransactionSuccessful()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        } finally {
            database.endTransaction()
        }
        return rowId != -1L
    }

    fun getContentOlder(startId: Int, count: Int): ArrayList<Content>? {
        val database = readableDatabase
        val cursor: Cursor
        cursor = if (startId > -1) {
            database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " where " + CONTENT_COLUMN_ID + "<" + startId +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", arrayOf())
        } else {
            //            cursor = database.query(CONTENT_TABLE_NAME, null, null, null, null, null, CONTENT_COLUMN_ID + " DESC", "20");
            database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", null)
        }
        val contents = ArrayList<Content>()
        fitContents(cursor, contents)
        return contents
    }

    fun getContentNewer(startId: Int, count: Int): ArrayList<Content>? {
        val database = readableDatabase
        val cursor: Cursor
        cursor = if (startId > -1) {
            database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " where " + CONTENT_COLUMN_ID + ">" + startId +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", arrayOf())
        } else {
            //            cursor = database.query(CONTENT_TABLE_NAME, null, null, null, null, null, CONTENT_COLUMN_ID + " DESC", "20");
            database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                    " order by " + CONTENT_COLUMN_ID + " desc" +
                    " limit " + count + ";", null)
        }
        val contents = ArrayList<Content>()
        fitContents(cursor, contents)
        return contents
    }

    private fun fitContents(cursor: Cursor?, contents: ArrayList<Content>) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_ID))
                    val type = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_TYPE))
                    val desc = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_DESC))
                    val filePath = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_FILE_PATH))
                    val cityName = cursor.getString(cursor.getColumnIndex(CONTENT_COLUMN_CITY_NAME))
                    val mediaDuration = cursor.getInt(cursor.getColumnIndex(CONTENT_COLUMN_MEDIA_DURATION))
                    val releaseTime = cursor.getLong(cursor.getColumnIndex(CONTENT_COLUMN_RELEASE_TIME))
                    val content = Content()
                    content.id = id
                    content.type = type
                    content.desc = desc
                    content.mediaFilePath = filePath
                    content.cityName = cityName
                    content.mediaDuration = mediaDuration
                    content.releaseTime = releaseTime
                    contents.add(content)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    fun deleteContent(contentList: ArrayList<Content>?): Boolean {
        if (contentList == null) return false
        val database = writableDatabase
        database.beginTransaction()
        try {
            for (content in contentList) {
                database.delete(CONTENT_TABLE_NAME, "$CONTENT_COLUMN_ID=?", arrayOf(content.id.toString()))
                decrementCity(content.cityName, database)
            }
            database.setTransactionSuccessful()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        } finally {
            database.endTransaction()
        }
        return true
    }

    fun searchByCity(city: String, count: Int): ArrayList<Content>? {
        val database = readableDatabase
        val cursor = database.query(CONTENT_TABLE_NAME, null,
                "$CONTENT_COLUMN_CITY_NAME=?", arrayOf(city),
                null, null, "$CONTENT_COLUMN_ID DESC", count.toString())
        //        Cursor cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
//                        " where " + CONTENT_COLUMN_CITY_NAME + "=" + city +
//                        " order by " + CONTENT_COLUMN_ID + " desc" +
//                        " limit " + count + ";",
//                new String[]{});
        val contents = ArrayList<Content>()
        fitContents(cursor, contents)
        return contents
    }

    fun searchByType(type: Int, count: Int): ArrayList<Content>? {
        val database = readableDatabase
        val cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                " where " + CONTENT_COLUMN_TYPE + "=" + type +
                " order by " + CONTENT_COLUMN_ID + " desc" +
                " limit " + count + ";", arrayOf())
        val contents = ArrayList<Content>()
        fitContents(cursor, contents)
        return contents
    }

    fun searchByDesc(keyword: String, count: Int): ArrayList<Content>? {
        if (TextUtils.isEmpty(keyword) || TextUtils.isEmpty(keyword.trim { it <= ' ' })) return null
        //        String key = keyword.trim().replace("[", "[[]").replace("_", "[_]").replace("%", "[%]");//.replaceAll("\\s+", " ");
        val keys = keyword.trim { it <= ' ' }.replace("[", "[[]").replace("_", "[_]").replace("%", "[%]").split("\\s+".toRegex()).toTypedArray()
        var whereCase = ""
        for (i in keys.indices) {
            val key = keys[i]
            if (i > 0) {
                whereCase += " and "
            }
            whereCase += "$CONTENT_COLUMN_DESC like '%$key%'"
        }
        LogUtil.d("searchByDesc", "whereCase = $whereCase")
        val database = readableDatabase
        val cursor = database.rawQuery("select * from " + CONTENT_TABLE_NAME +
                " where " + whereCase +
                " order by " + CONTENT_COLUMN_ID + " desc" +
                " limit " + count + ";", arrayOf())
        val contents = ArrayList<Content>()
        fitContents(cursor, contents)
        return contents
    }
    //endregion

    //endregion
    //region 城市相关
    private fun createCityTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $CITY_TABLE_NAME")
        db.execSQL("CREATE TABLE " + CITY_TABLE_NAME + " (" +
                CITY_COLUMN_CITY_NAME + " TEXT PRIMARY KEY," +
                CITY_COLUMN_CONTENT_COUNT + " INTEGER" +
                ");")
    }

    private fun incrementCity(cityName: String?, database: SQLiteDatabase) {
        if (TextUtils.isEmpty(cityName)) return
        val cursor = database.query(CITY_TABLE_NAME, null, "$CITY_COLUMN_CITY_NAME=?", arrayOf(cityName), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            var count = cursor.getInt(cursor.getColumnIndex(CITY_COLUMN_CONTENT_COUNT))
            val contentValues = ContentValues()
            contentValues.put(CITY_COLUMN_CITY_NAME, cityName)
            contentValues.put(CITY_COLUMN_CONTENT_COUNT, ++count)
            database.update(CITY_TABLE_NAME, contentValues, "$CITY_COLUMN_CITY_NAME=?", arrayOf(cityName))
        } else {
            val contentValues = ContentValues()
            contentValues.put(CITY_COLUMN_CITY_NAME, cityName)
            contentValues.put(CITY_COLUMN_CONTENT_COUNT, 1)
            database.insert(CITY_TABLE_NAME, null, contentValues)
        }
        cursor?.close()
    }

    fun getAllCity(): ArrayList<String>? {
        val cityList = ArrayList<String>()
        val database = readableDatabase
        val cursor = database.query(CITY_TABLE_NAME, null, null, null, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val city = cursor.getString(cursor.getColumnIndex(CITY_COLUMN_CITY_NAME))
                    cityList.add(city)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return cityList
    }

    private fun decrementCity(cityName: String?, database: SQLiteDatabase) {
        if (TextUtils.isEmpty(cityName)) return
        val cursor = database.query(CITY_TABLE_NAME, null, "$CITY_COLUMN_CITY_NAME=?", arrayOf(cityName), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            var count = cursor.getInt(cursor.getColumnIndex(CITY_COLUMN_CONTENT_COUNT))
            if (--count > 0) {
                val contentValues = ContentValues()
                contentValues.put(CITY_COLUMN_CITY_NAME, cityName)
                contentValues.put(CITY_COLUMN_CONTENT_COUNT, count)
                database.update(CITY_TABLE_NAME, contentValues, "$CITY_COLUMN_CITY_NAME=?", arrayOf(cityName))
            } else {
                database.delete(CITY_TABLE_NAME, "$CITY_COLUMN_CITY_NAME=?", arrayOf(cityName))
            }
        }
        cursor?.close()
    }
    //endregion

    //endregion
    //region 搜索历史
    private fun createSearchHistoryTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $SEARCH_HISTORY_TABLE_NAME")
        db.execSQL("CREATE TABLE " + SEARCH_HISTORY_TABLE_NAME + " (" +
                SEARCH_HISTORY_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SEARCH_HISTORY_COLUMN_WORD + " TEXT"
                + ");")
    }

    fun insertSearchHistory(keyword: String): Boolean {
        if (TextUtils.isEmpty(keyword) || TextUtils.isEmpty(keyword.trim { it <= ' ' })) {
            return false
        }
        val database = writableDatabase
        database.delete(SEARCH_HISTORY_TABLE_NAME, "$SEARCH_HISTORY_COLUMN_WORD=?", arrayOf(keyword))
        val contentValues = ContentValues()
        contentValues.put(SEARCH_HISTORY_COLUMN_WORD, keyword)
        val rowId = database.insert(SEARCH_HISTORY_TABLE_NAME, null, contentValues)
        return rowId != -1L
    }

    fun getSearchHistory(): ArrayList<String>? {
        val keywordList = ArrayList<String>()
        val database = writableDatabase
        val cursor = database.query(SEARCH_HISTORY_TABLE_NAME, null, null, null, null, null, "$SEARCH_HISTORY_COLUMN_ID desc", "4")
        var id = -1
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val keyword = cursor.getString(cursor.getColumnIndex(SEARCH_HISTORY_COLUMN_WORD))
                    id = cursor.getInt(cursor.getColumnIndex(SEARCH_HISTORY_COLUMN_ID))
                    keywordList.add(keyword)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        if (keywordList.size >= 4) {
            database.delete(SEARCH_HISTORY_TABLE_NAME, "$SEARCH_HISTORY_COLUMN_ID<?", arrayOf(id.toString()))
        }
        return keywordList
    }
    //endregion
}