package com.candy.capture.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/20 17:30
 */
@Entity(tableName = "city", indices = [Index(name = "index_city_city_name", value = ["city_name"], unique = true)])
data class City(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int = 0,
                @ColumnInfo(name = "city_name") var cityName: String,
                @ColumnInfo(name = "content_count") var count: Int) {
}