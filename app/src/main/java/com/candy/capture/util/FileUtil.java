package com.candy.capture.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Date;

/**
 * Created by zhanghq on 2016/11/13.
 */

public class FileUtil {
    private static final String APP_ROOT_DIR = "Capture";
    private static final String AUDIO_DIR = "audio";
    private static final String PHOTO_DIR = "photo";
    private static final String VIDEO_DIR = "video";

    public static final int MEDIA_TYPE_AUDIO = 1;
    public static final int MEDIA_TYPE_PHOTO = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;

    public static void initRootFolder(Context context) {
        File rootDir;
        if (Environment.isExternalStorageEmulated() && !Environment.isExternalStorageRemovable()) {
            rootDir = new File(Environment.getExternalStorageDirectory() + File.separator + APP_ROOT_DIR);
        } else {
            rootDir = new File(context.getFilesDir() + File.separator + APP_ROOT_DIR);
        }
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteFile(f);
                }
            }
            file.delete();
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteFile(f);
                }
            } else {
                file.delete();
            }
        }
    }

    public static String getMediaFilePath(Context context, int mediaType) {
        File rootDir;
        if (Environment.isExternalStorageEmulated() && !Environment.isExternalStorageRemovable()) {
            rootDir = new File(Environment.getExternalStorageDirectory() + File.separator + APP_ROOT_DIR);
        } else {
            rootDir = new File(context.getFilesDir() + File.separator + APP_ROOT_DIR);
        }
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }

        String filePath = "";
        File mediaDir = null;
        switch (mediaType) {
            case MEDIA_TYPE_AUDIO:
                mediaDir = new File(rootDir.getAbsolutePath() + File.separator + AUDIO_DIR);
                break;
            case MEDIA_TYPE_PHOTO:
                mediaDir = new File(rootDir.getAbsolutePath() + File.separator + PHOTO_DIR);
                break;
            case MEDIA_TYPE_VIDEO:
                mediaDir = new File(rootDir.getAbsolutePath() + File.separator + VIDEO_DIR);
                break;
        }
        if (mediaDir != null) {
            if (!mediaDir.exists()) {
                mediaDir.mkdir();
            }

            filePath = mediaDir.getAbsolutePath() + File.separator + new Date().getTime();
        }

        return filePath;
    }
}
