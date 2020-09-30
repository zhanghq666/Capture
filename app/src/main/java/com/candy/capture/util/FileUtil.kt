package com.candy.capture.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:44
 */
object FileUtil {
    private const val APP_ROOT_DIR = "Capture"
    private const val AUDIO_DIR = "audio"
    private const val PHOTO_DIR = "photo"
    private const val VIDEO_DIR = "video"

    const val MEDIA_TYPE_AUDIO = 1
    const val MEDIA_TYPE_PHOTO = 2
    const val MEDIA_TYPE_VIDEO = 3

    fun initRootFolder(context: Context) {
        val rootDir  = if (Environment.isExternalStorageEmulated() && !Environment.isExternalStorageRemovable()) {
            File(Environment.getExternalStorageDirectory().toString() + File.separator + APP_ROOT_DIR)
        } else {
            File(context.filesDir.toString() + File.separator + APP_ROOT_DIR)
        }
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }
    }

    fun deleteFile(path: String?) {
        val file = File(path)
        if (file.exists()) {
            if (file.isDirectory) {
                for (f in file.listFiles()) {
                    deleteFile(f)
                }
            }
            file.delete()
        }
    }

    fun deleteFile(file: File) {
        if (file.exists()) {
            if (file.isDirectory) {
                for (f in file.listFiles()) {
                    deleteFile(f)
                }
            } else {
                file.delete()
            }
        }
    }

    fun getMediaFilePath(context: Context, mediaType: Int): String? {
        val rootDir = if (Environment.isExternalStorageEmulated() && !Environment.isExternalStorageRemovable()) {
            File(Environment.getExternalStorageDirectory().toString() + File.separator + APP_ROOT_DIR)
        } else {
            File(context.filesDir.toString() + File.separator + APP_ROOT_DIR)
        }
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }
        var filePath = ""
        var mediaDir: File? = null
        when (mediaType) {
            MEDIA_TYPE_AUDIO -> mediaDir = File(rootDir.absolutePath + File.separator + AUDIO_DIR)
            MEDIA_TYPE_PHOTO -> mediaDir = File(rootDir.absolutePath + File.separator + PHOTO_DIR)
            MEDIA_TYPE_VIDEO -> mediaDir = File(rootDir.absolutePath + File.separator + VIDEO_DIR)
        }
        if (mediaDir != null) {
            if (!mediaDir.exists()) {
                mediaDir.mkdir()
            }
            filePath = mediaDir.absolutePath + File.separator + Date().time
        }
        return filePath
    }
}