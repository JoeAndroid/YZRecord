package com.yanzhi.record.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.meicam.sdk.NvsLiveWindow;

/**
 * Created by zj on 2017/3/3.
 */

public class LiveWindow extends NvsLiveWindow {

    public LiveWindow(Context context) {
        super(context);
    }

    public LiveWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        setMeasuredDimension(metrics.widthPixels, metrics.widthPixels*9/16);
    }
}
