package com.candy.capture.model;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class Content implements Serializable {
    private int id;
    /**
     * 内容类型。分为：
     * 纯文字{@link com.candy.capture.core.ConstantValues#CONTENT_TYPE_TEXT};
     * 语音{@link com.candy.capture.core.ConstantValues#CONTENT_TYPE_AUDIO};
     * 照片{@link com.candy.capture.core.ConstantValues#CONTENT_TYPE_PHOTO};
     * 视频{@link com.candy.capture.core.ConstantValues#CONTENT_TYPE_VIDEO}
     */
    private int type;
    /**
     * 文字描述
     */
    private String desc;
    /**
     * 媒体文件路径
     */
    private String mediaFilePath;
    /**
     * 媒体文件时长
     */
    private int mediaDuration;
    /**
     * 所在城市
     */
    private String cityName;
    /**
     * 发布时间 in 秒
     */
    private int releaseTime;
    /**
     * 是否已同步到服务器（暂时无用）
     */
    private boolean isSynced;

    /**
     * 是否选中
     */
    private boolean isSelect;
    /**
     * 是否在播放音频
     */
    private boolean isPlayingAudio;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMediaFilePath() {
        return mediaFilePath;
    }

    public void setMediaFilePath(String mediaFilePath) {
        this.mediaFilePath = mediaFilePath;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public int getMediaDuration() {
        return mediaDuration;
    }

    public void setMediaDuration(int mediaDuration) {
        this.mediaDuration = mediaDuration;
    }

    public int getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(int releaseTime) {
        this.releaseTime = releaseTime;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Content content = (Content) o;

        if (id != content.id) return false;
        if (type != content.type) return false;
        if (mediaDuration != content.mediaDuration) return false;
        if (releaseTime != content.releaseTime) return false;
        if (isSynced != content.isSynced) return false;
        if (isSelect != content.isSelect) return false;
        if (desc != null ? !desc.equals(content.desc) : content.desc != null) return false;
        if (mediaFilePath != null ? !mediaFilePath.equals(content.mediaFilePath) : content.mediaFilePath != null)
            return false;
        return cityName != null ? cityName.equals(content.cityName) : content.cityName == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + type;
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + (mediaFilePath != null ? mediaFilePath.hashCode() : 0);
        result = 31 * result + mediaDuration;
        result = 31 * result + (cityName != null ? cityName.hashCode() : 0);
        result = 31 * result + releaseTime;
        result = 31 * result + (isSynced ? 1 : 0);
        result = 31 * result + (isSelect ? 1 : 0);
        return result;
    }

    public boolean isPlayingAudio() {
        return isPlayingAudio;
    }

    public void setPlayingAudio(boolean playingAudio) {
        isPlayingAudio = playingAudio;
    }
}
