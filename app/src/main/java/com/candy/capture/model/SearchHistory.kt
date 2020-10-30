package com.candy.capture.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/20 17:30
 */
@Entity(tableName = "search_history")
data class SearchHistory(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int = 0,
                         @ColumnInfo(name = "word") var keyword: String) {
}