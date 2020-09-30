package com.candy.capture.model

import java.io.Serializable

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:28
 */
data class Content(var id: Int = 0, var type: Int = 0, var desc: String? = null, var mediaFilePath: String? = null,
                   var mediaDuration: Int = 0, var cityName: String? = null, var releaseTime: Long = 0,
                   var isSynced: Boolean = false, var isSelect: Boolean = false, var isPlayingAudio: Boolean = false) : Serializable {

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
        if (isSynced != other.isSynced) return false
        if (isSelect != other.isSelect) return false
        if (isPlayingAudio != other.isPlayingAudio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + type
        result = 31 * result + (desc?.hashCode() ?: 0)
        result = 31 * result + (mediaFilePath?.hashCode() ?: 0)
        result = 31 * result + mediaDuration.hashCode()
        result = 31 * result + (cityName?.hashCode() ?: 0)
        result = 31 * result + releaseTime.hashCode()
        result = 31 * result + isSynced.hashCode()
        result = 31 * result + isSelect.hashCode()
        result = 31 * result + isPlayingAudio.hashCode()
        return result
    }

}