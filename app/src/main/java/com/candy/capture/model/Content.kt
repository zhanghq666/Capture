package com.candy.capture.model

import androidx.room.*
import java.io.Serializable

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:28
 */
@Entity(tableName = "content", foreignKeys = [ForeignKey(entity = City::class, childColumns = ["city_name"], parentColumns = ["city_name"])])
data class Content(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int = 0, @ColumnInfo(name = "type") var type: Int = 0,
                   @ColumnInfo(name = "description") var desc: String = "", @ColumnInfo(name = "file_path") var mediaFilePath: String = "",
                   @ColumnInfo(name = "media_duration") var mediaDuration: Int = 0, @ColumnInfo(name = "city_name", index = true) var cityName: String = "",
                   @ColumnInfo(name = "release_time") var releaseTime: Long = 0, @Ignore var synced: Boolean = false,
                   @Ignore var select: Boolean = false, @Ignore var playingAudio: Boolean = false) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Content

        if (id != other.id) return false
        if (type != other.type) return false
        if (desc != other.desc) return false
        if (mediaFilePath != other.mediaFilePath) return false
        if (mediaDuration != other.mediaDuration) return false
        if (cityName != other.cityName) return false
        if (releaseTime != other.releaseTime) return false
        if (synced != other.synced) return false
        if (select != other.select) return false
        if (playingAudio != other.playingAudio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + type
        result = 31 * result + desc.hashCode()
        result = 31 * result + mediaFilePath.hashCode()
        result = 31 * result + mediaDuration
        result = 31 * result + cityName.hashCode()
        result = 31 * result + releaseTime.hashCode()
        result = 31 * result + synced.hashCode()
        result = 31 * result + select.hashCode()
        result = 31 * result + playingAudio.hashCode()
        return result
    }
}