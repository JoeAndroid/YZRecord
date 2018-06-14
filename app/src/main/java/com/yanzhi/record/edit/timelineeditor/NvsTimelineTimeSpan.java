package com.yanzhi.record.edit.timelineeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.yanzhi.record.R;


/**
 * Created by admin on 2017/9/1.
 */

public class NvsTimelineTimeSpan extends RelativeLayout {
    private String TAG = "TimeSpan";
    private int prevRawX = 0;
    private boolean mCanMoveHandle = false;
    private OnTrimInChangeListener mOnTrimInChangeListener;
    private OnTrimOutChangeListener mOnTrimOutChangeListener;

    //
    private int mHandleWidth = 0;
    private long mInPoint = 0;
    private long mOutPoint = 0;
    private double mPixelPerMicrosecond = 0D;
    private boolean hasSelected = true;
    private int mTotalWidth = 0;
    //
    private long minDraggedTimeSpanDuration = 1000000;
    private int minDraggedTimeSpanPixel = 0;
    private int originLeft = 0;
    private int originRight = 0;
    private int dragDirection = 0;
    private static final int LEFT = 0x16;
    private static final int CENTER = 0x17;
    private static final int RIGHT = 0x18;
    private View timeSpanshadowView;
    public NvsTimelineTimeSpan(Context context){
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.timespan, this);
        RelativeLayout leftHandleView = (RelativeLayout) view.findViewById(R.id.leftHandle);
        mHandleWidth = leftHandleView.getLayoutParams().width;
        timeSpanshadowView = view.findViewById(R.id.timeSpanShadow);
    }
    public View getTimeSpanshadowView(){return timeSpanshadowView;}
    public long getInPoint() {
        return mInPoint;
    }

    public void setInPoint(long mInPoint) {
        this.mInPoint = mInPoint;
    }


    public long getOutPoint() {
        return mOutPoint;
    }

    public void setOutPoint(long mOutPoint) {
        this.mOutPoint = mOutPoint;
    }

    public void setPixelPerMicrosecond(double mPixelPerMicrosecond) {
        this.mPixelPerMicrosecond = mPixelPerMicrosecond;
    }

    public int getHandleWidth() {
        return mHandleWidth;
    }

    public boolean isHasSelected() {
        return hasSelected;
    }

    public void setHasSelected(boolean hasSelected) {
        this.hasSelected = hasSelected;
    }

    public void setTotalWidth(int mTotalWidth) {
        this.mTotalWidth = mTotalWidth;
    }
    private void updateTimeSpan(){
        RelativeLayout mLeftView = (RelativeLayout) findViewById(R.id.leftHandle);
        RelativeLayout mRightView = (RelativeLayout) findViewById(R.id.rightHandle);
        if(mLeftView == null || mRightView == null){
            return;
        }

        if(hasSelected){
            mLeftView.setVisibility(View.VISIBLE);
            mRightView.setVisibility(View.VISIBLE);
        }else {
            mLeftView.setVisibility(View.INVISIBLE);
            mRightView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!hasSelected){
            if((dragDirection == LEFT) && mOnTrimInChangeListener != null){
                mInPoint = (long) Math.floor(originLeft / mPixelPerMicrosecond + 0.5D);
                mOnTrimInChangeListener.onChange(mInPoint,true);
            }
            if((dragDirection == RIGHT) && mOnTrimOutChangeListener != null){
                mOutPoint = (long) Math.floor((originRight - 2 * mHandleWidth) / mPixelPerMicrosecond + 0.5D);
                mOnTrimOutChangeListener.onChange(mOutPoint,true);
            }
            return false;//未被选中，不作编辑操作
        }

        minDraggedTimeSpanPixel = (int) Math.floor(minDraggedTimeSpanDuration * mPixelPerMicrosecond + 0.5D);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCanMoveHandle = !(mHandleWidth < ev.getX() && ev.getX() < getWidth() - mHandleWidth);
                getParent().requestDisallowInterceptTouchEvent(true);
                originLeft = getLeft();
                originRight = getRight();

                prevRawX = (int) ev.getRawX();
                dragDirection = getDirection((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                int tempRawX = (int)ev.getRawX();

                int dx = tempRawX - prevRawX;
                prevRawX = tempRawX;
                if(dragDirection == LEFT){
                    left(dx);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
                    lp.width = originRight - originLeft;
                    lp.setMargins(originLeft, RelativeLayout.LayoutParams.MATCH_PARENT, mTotalWidth - originRight , 0);
                    setLayoutParams(lp);

                    mInPoint = (long) Math.floor(originLeft / mPixelPerMicrosecond + 0.5D);
                    if(mOnTrimInChangeListener != null) {
                        mOnTrimInChangeListener.onChange(mInPoint, false);
                    }
                }
                if(dragDirection == RIGHT){
                    right(dx);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
                    lp.width = originRight - originLeft;
                    lp.setMargins(originLeft, RelativeLayout.LayoutParams.MATCH_PARENT, mTotalWidth - originRight , 0);
                    setLayoutParams(lp);
                    mOutPoint = (long) Math.floor((originRight - 2 * mHandleWidth) / mPixelPerMicrosecond + 0.5D);
                    if(mOnTrimOutChangeListener != null) {
                        mOnTrimOutChangeListener.onChange(mOutPoint, false);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if((dragDirection == LEFT) && mOnTrimInChangeListener != null){
                    mInPoint = (long) Math.floor(originLeft / mPixelPerMicrosecond + 0.5D);
                    mOnTrimInChangeListener.onChange(mInPoint,true);
                }
                if((dragDirection == RIGHT) && mOnTrimOutChangeListener != null){
                    mOutPoint = (long) Math.floor((originRight - 2 * mHandleWidth) / mPixelPerMicrosecond + 0.5D);
                    mOnTrimOutChangeListener.onChange(mOutPoint,true);
                }
                break;
        }
        return mCanMoveHandle;
    }

    public void setOnChangeListener(OnTrimInChangeListener onChangeListener) {
        this.mOnTrimInChangeListener = onChangeListener;
    }

    public void setOnChangeListener(OnTrimOutChangeListener onChangeListener) {
        this.mOnTrimOutChangeListener = onChangeListener;
    }


    public interface OnTrimInChangeListener {
         void onChange(long timeStamp, boolean isDragEnd);
    }

    public interface OnTrimOutChangeListener {
         void onChange(long timeStamp, boolean isDragEnd);
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        updateTimeSpan();
    }
    private int getDirection( int x, int y) {
        int left = getLeft();
        int right = getRight();

        if (x < mHandleWidth) {
            return LEFT;
        }
        if (right - left - x < mHandleWidth) {
            return RIGHT;
        }
        return CENTER;
    }
    /**
     * 触摸点为右边缘
     */
    private void right(int dx) {
        originRight += dx;
        if (originRight > mTotalWidth ) {
            originRight = mTotalWidth;
        }
        if (originRight - originLeft - 2 * mHandleWidth  < minDraggedTimeSpanPixel) {
            originRight = originLeft  + minDraggedTimeSpanPixel + 2 * mHandleWidth;
        }
    }

    /**
     * 触摸点为左边缘
     */
    private void left(int dx) {
        originLeft += dx;
        if (originLeft < 0) {
            originLeft = 0;
        }
        if (originRight - originLeft - 2 * mHandleWidth  < minDraggedTimeSpanPixel) {
            originLeft = originRight - 2 * mHandleWidth - minDraggedTimeSpanPixel;
        }
    }
}
