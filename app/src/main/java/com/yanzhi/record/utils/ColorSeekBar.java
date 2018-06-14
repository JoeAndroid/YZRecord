package com.yanzhi.record.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yanzhi.record.R;

public class ColorSeekBar extends View {

    private final String TAG = "ColorSeekBar";
    private final Paint mPaint = new Paint();
    private float mLeft, mTop, mRight, mBottom;
    private float mWidth, mHeight;
    private float mProgress = 100f;
    private LinearGradient mlinearGradient;
    private float mIndicatorX;
    private float mPreIndicatorX;
    private OnStateChangeListener mStateChangeListener;
    private Bitmap mIndicatorBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.edit_color);
    private int mColorArray[] = {0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
            0xFF00FFFF, 0xFF00FF00,0xFFFFFF00, 0xFFFF0000};


    public ColorSeekBar(Context context) {
        this(context, null);
    }


    public ColorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec * 2);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLeft = 20;                  // 背景左
        mTop = h * 0.25f;           // 背景上
        mRight = w - 20;            // 背景右
        mBottom = h * 0.75f;        // 背景底
        mWidth = mRight - mLeft;    // 背景的宽度
        mHeight = mBottom - mTop;   // 背景的高度

        mIndicatorX =  mProgress/100f * mWidth;
        int curColor = getSeekBarColor(mColorArray, mIndicatorX/mWidth);
        if (mStateChangeListener != null) {
            mStateChangeListener.onColorChanged(curColor);
            if(mProgress < 0 ){
                mProgress = 0;
            }

            if(mProgress > 100){
                mProgress = 100;
            }
            mStateChangeListener.onProgressChanged(mProgress);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawIndicator(canvas);
        mPaint.reset();
    }

    public boolean onTouchEvent(MotionEvent event) {
        mIndicatorX = event.getX();
        mProgress = 100 - (mIndicatorX/mWidth * 100);
        int curColor = getSeekBarColor(mColorArray, mIndicatorX/mWidth);
        if (mStateChangeListener != null) {
            mStateChangeListener.onColorChanged(curColor);

            if(mProgress < 0){
                mProgress = 0;
            }

            if(mProgress > 100) {
                mProgress = 100;
            }
            mStateChangeListener.onProgressChanged(mProgress);
        }
        invalidate();
        return true;
    }

    private void drawIndicator(Canvas canvas) {
        Paint paint = new Paint();
        if( (5 <= mIndicatorX) && (mIndicatorX <= mWidth - 5) ){
            canvas.drawBitmap(mIndicatorBitmap, mIndicatorX, 0, paint);
            mPreIndicatorX = mIndicatorX;
        }else{
            canvas.drawBitmap(mIndicatorBitmap, mPreIndicatorX, 0, paint);
        }


    }

    private void drawBackground(Canvas canvas) {
        mlinearGradient = new LinearGradient(mLeft, mTop, getMeasuredWidth(), 0, mColorArray, null, LinearGradient.TileMode.REPEAT);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        //设置渲染器
        mPaint.setShader(mlinearGradient);
        RectF rectF = new RectF(mLeft, mTop, mRight, mBottom);
        canvas.drawRect(rectF, mPaint);
    }

    public interface OnStateChangeListener {
        void onColorChanged(int curColor);
        void onProgressChanged(float progress);
    }

    public void setOnStateChangeListener(OnStateChangeListener listener){
        mStateChangeListener = listener;
    }

    public void setColorArray(int[] colorArray){
        mColorArray = colorArray;
        mIndicatorX = 5;
        if (mStateChangeListener != null) {
            mStateChangeListener.onProgressChanged(100);
        }
        invalidate();
    }

    public void setProgress(int progress){
        mProgress = 100 - progress;
        invalidate();
    }

    // 获取SeekBar上的颜色
    private int getSeekBarColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int)p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i+1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

}
