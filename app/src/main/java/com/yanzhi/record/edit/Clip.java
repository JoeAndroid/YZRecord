package com.yanzhi.record.edit;

import java.io.Serializable;


/**
 * 拍摄模块
 */
public class Clip implements Serializable {
    private String path;//视频地址
    private long duration;
    private long startTime;
    private long endTime;
    private int recordRotation = 0;//拍摄方向
    private int camareId;//摄像头

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return getEndTime() - getStartTime();
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getRecordRotation() {
        return recordRotation;
    }

    public void setRecordRotation(int recordRotation) {
        this.recordRotation = recordRotation;
    }

    public int getCamareId() {
        return camareId;
    }

    public void setCamareId(int camareId) {
        this.camareId = camareId;
    }
}
