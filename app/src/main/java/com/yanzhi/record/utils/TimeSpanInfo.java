package com.yanzhi.record.utils;

import com.meicam.sdk.NvsTimelineAnimatedSticker;
import com.meicam.sdk.NvsTimelineCaption;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineTimeSpan;

/**
 * Created by Administrator on 2017/9/26.
 */

public class TimeSpanInfo {
    public NvsTimelineCaption caption;
    public NvsTimelineAnimatedSticker sticker;
    public NvsTimelineTimeSpan timeSpan;


    public TimeSpanInfo(NvsTimelineCaption caption, NvsTimelineAnimatedSticker sticker, NvsTimelineTimeSpan timeSpan){
        this.caption = caption;
        this.sticker = sticker;
        this.timeSpan = timeSpan;
    }

}
