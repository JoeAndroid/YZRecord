package com.yanzhi.record.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.LinkedList;

/**
 *分段录制进度
 */
public class RecordProgressView extends View {

    // 最长录制时间 和 最小录制时间
    public final static float MAX_RECORD_TIME = 30000f;
    public final static float MIN_RECORD_TIME = 5000f;

    // 各种画笔 进度 光标 最小时间标线 分段分割线 回滚
    private Paint progressPaint, flashPaint, minTimePaint, breakPaint, rollbackPaint;
    // 光标宽度
    private float flashWidth = 10f;
    // 最小时间标线宽度
    private float minTimeWidth = 5f;
    // 分段分割线宽度
    private float breakWidth = 2f;

    // 背景及各种画笔颜色
    private int backgroundColor = Color.parseColor("#525252");
    private int progressColor = Color.parseColor("#86b00b");
    private int flashColor = Color.parseColor("#FFFFFF");
    private int minTimeColor = Color.parseColor("#FF0000");
    private int breakColor = Color.parseColor("#000000");
    private int rollbackColor = Color.parseColor("#e93c50");

    // 闪动光标是否可见
    private boolean isFlashVisible = true;
    // 光标绘制时间戳
    private long lastDrawFlashTime = 0;

    // 每次绘制完成后，进度条的长度
    private float countWidth = 0;
    private float perWidth;

    // 录制片段
    private LinkedList<Integer> recordPartList = new LinkedList<>();
    private volatile RecordState currentState = RecordState.PAUSE;

    public RecordProgressView(Context context) {
        super(context);
        init();
    }

    public RecordProgressView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    public RecordProgressView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init();
    }

    private void init() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        perWidth = screenWidth / MAX_RECORD_TIME;

        progressPaint = new Paint();
        flashPaint = new Paint();
        minTimePaint = new Paint();
        breakPaint = new Paint();
        rollbackPaint = new Paint();

        setBackgroundColor(backgroundColor);
        progressPaint.setColor(progressColor);
        flashPaint.setColor(flashColor);
        minTimePaint.setColor(minTimeColor);
        breakPaint.setColor(breakColor);
        rollbackPaint.setColor(rollbackColor);
    }

    /**
     * @description 更新录制状态
     */
    public void changeRecordState(RecordState state) {
        if (currentState == state) return;
        if (currentState != RecordState.START && state == RecordState.START && !recordPartList.isEmpty()) {
            recordPartList.add(recordPartList.getLast());
        }
        currentState = state;
        if (state == RecordState.DELETE) {
            if ((recordPartList != null) && (!recordPartList.isEmpty())) {
                recordPartList.removeLast();
            }
        }
    }

    public void resetProgress() {
        currentState = RecordState.PAUSE;
        recordPartList.clear();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // onDraw是此自定义View的核心了
        int progressHeight = getMeasuredHeight();
        long currentTimeMillis = System.currentTimeMillis();
        countWidth = 0;
        // 绘制录制片段和分割线
        if (!recordPartList.isEmpty()) {
            long previousTime = 0;
            long currentTime = 0;
            for (Integer aTimeList : recordPartList) {
                currentTime = aTimeList;
                float left = countWidth;
                countWidth += (currentTime - previousTime) * perWidth;
                canvas.drawRect(left, 0, countWidth - breakWidth, progressHeight, progressPaint);
                canvas.drawRect(countWidth - breakWidth, 0, countWidth, progressHeight, breakPaint);
                previousTime = currentTime;
            }
        }
        // 绘制最小时间分割线
        if (recordPartList.isEmpty()
                || (!recordPartList.isEmpty() && recordPartList.getLast() <= MIN_RECORD_TIME)) {
            float left = perWidth * MIN_RECORD_TIME;
            canvas.drawRect(left, 0, left + minTimeWidth, progressHeight, minTimePaint);
        }
        // 绘制回滚状态
        if (currentState == RecordState.ROLLBACK) {
            long lastPartStartTime =
                    recordPartList.size() > 1 ? recordPartList.get(recordPartList.size() - 2) : 0;
            long lastPartEndTime =
                    recordPartList.size() > 0 ? recordPartList.get(recordPartList.size() - 1) : 0;
            float left = countWidth - (lastPartEndTime - lastPartStartTime) * perWidth;
            float right = countWidth;
            canvas.drawRect(left, 0, right, progressHeight, rollbackPaint);
        }
        // 绘制录制进度
        if (currentState == RecordState.START) {
            canvas.drawRect(countWidth, 0, countWidth + flashWidth, getMeasuredHeight(), flashPaint);
        } else {
            // 如果当前不是录制状态的话 光标是闪动的 而闪动的时间是这里800
            if (lastDrawFlashTime == 0 || currentTimeMillis - lastDrawFlashTime >= 800) {
                isFlashVisible = !isFlashVisible;
                lastDrawFlashTime = System.currentTimeMillis();
            }
            if (isFlashVisible) {
                canvas.drawRect(countWidth, 0, countWidth + flashWidth, getMeasuredHeight(), flashPaint);
            }
        }
        // 基于 Android 16毫秒 刷新
        getHandler().postDelayed(invalidateRunnable, 16);
    }

    private Runnable invalidateRunnable = new Runnable() {
        @Override
        public void run() {
            switch (currentState) {
                case START: // 更新绘制进度
                    if (recordPartList.size() > 0)
                        recordPartList.add(recordPartList.removeLast() + 16);
                    else
                        recordPartList.add(16);
                    break;
            }
            invalidate();
        }
    };

    public RecordState getCurrentState(){
        return currentState;
    }

    public int getLastTime() {
        if ((recordPartList != null) && (!recordPartList.isEmpty())) {
            return recordPartList.getLast();
        }
        return 0;
    }

    public int getLastStartTime() {
        try {
            if ((recordPartList != null) && (!recordPartList.isEmpty())) {
                return recordPartList.get(recordPartList.size() - 2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isRecordEmpty() {
        return recordPartList.isEmpty();
    }

    /**
     * @description 视频录制的各种状态
     */
    public enum RecordState {
        START(0x1), PAUSE(0x2), ROLLBACK(0x3), DELETE(0x4);
        private int value;

        RecordState(int intValue) {
            value = intValue;
        }

        int getValue() {
            return value;
        }
    }
}
