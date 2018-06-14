package com.yanzhi.record.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


import com.yanzhi.record.R;

import java.util.List;

import static com.yanzhi.record.utils.GlobalData.VIEW_MODE_CAPTION;


/**
 * Created by zd on 2017/5/4.
 */

public class DrawRect extends View {

    private Bitmap rotationImgBtn = BitmapFactory.decodeResource(getResources(), R.mipmap.rotate);
    private OnTouchListener mListener;
    private PointF prePointF = new PointF(0, 0);
    private RectF rotationRectF = new RectF();
    private RectF alignRectF = new RectF();
    private RectF deleteRectF = new RectF();
    private List<PointF> mListPointF;
    private boolean canScalOrRotate = false;
    private boolean canAlignClick = false;
    private boolean canMove = false;
    private boolean canDel = false;
    private boolean isOutScreen = false;
    private OnAlignClickListener mClickListener;
    private int mIndex = 0;
    private int viewMode = 0;

    private Bitmap alignImgArray[] = {BitmapFactory.decodeResource(getResources(),R.mipmap.l), BitmapFactory.decodeResource(getResources(),R.mipmap.h), BitmapFactory.decodeResource(getResources(),R.mipmap.r)};
    private Bitmap deleteImgBtn = BitmapFactory.decodeResource(getResources(), R.mipmap.del);

    public DrawRect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
;
    public DrawRect(Context context) {

        super(context);
    }

    public void setalignIndex(int index){
        mIndex = index;
        invalidate();
    }


    public void SetDrawRect(List<PointF> list, int mode) {
        mListPointF = list;
        viewMode = mode;
        invalidate();
    }

    public void SetOnTouchListener(OnTouchListener listener) {
        mListener = listener;
    }

    public void SetOnAlignClickListener(OnAlignClickListener alignClickListener){mClickListener = alignClickListener;}

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint m_paint = new Paint();
        // 设置颜色
        m_paint.setColor(Color.RED);
        // 设置抗锯齿
        m_paint.setAntiAlias(true);
        // 设置线宽
        m_paint.setStrokeWidth(4);
        // 设置非填充
        m_paint.setStyle(Paint.Style.STROKE);

        if (mListPointF == null) {
            return;
        }

        // 画矩形框
        canvas.drawLine(mListPointF.get(0).x, mListPointF.get(0).y, mListPointF.get(1).x, mListPointF.get(1).y, m_paint);
        canvas.drawLine(mListPointF.get(1).x, mListPointF.get(1).y, mListPointF.get(2).x, mListPointF.get(2).y, m_paint);
        canvas.drawLine(mListPointF.get(2).x, mListPointF.get(2).y, mListPointF.get(3).x, mListPointF.get(3).y, m_paint);
        canvas.drawLine(mListPointF.get(3).x, mListPointF.get(3).y, mListPointF.get(0).x, mListPointF.get(0).y, m_paint);

        // 画旋转和放缩的按钮
        canvas.drawBitmap(rotationImgBtn, mListPointF.get(2).x - rotationImgBtn.getHeight() / 2, mListPointF.get(2).y - rotationImgBtn.getWidth() / 2, m_paint);
        rotationRectF.set(mListPointF.get(2).x - rotationImgBtn.getWidth() / 2, mListPointF.get(2).y - rotationImgBtn.getHeight() / 2, mListPointF.get(2).x+ rotationImgBtn.getWidth()/2, mListPointF.get(2).y + rotationImgBtn.getHeight()/2);
        canvas.drawBitmap(deleteImgBtn, mListPointF.get(3).x - deleteImgBtn.getWidth()/2 , mListPointF.get(3).y - deleteImgBtn.getHeight()/2, m_paint);
        deleteRectF.set(mListPointF.get(3).x - deleteImgBtn.getWidth() / 2, mListPointF.get(3).y - deleteImgBtn.getHeight() / 2, mListPointF.get(3).x + deleteImgBtn.getWidth()/2, mListPointF.get(3).y + deleteImgBtn.getHeight()/2);

        if (viewMode == VIEW_MODE_CAPTION) {
            canvas.drawBitmap(alignImgArray[mIndex],mListPointF.get(1).x - alignImgArray[mIndex].getHeight() / 2,mListPointF.get(1).y - alignImgArray[mIndex].getWidth() / 2, m_paint);
            alignRectF.set(mListPointF.get(1).x - alignImgArray[mIndex].getWidth() / 2, mListPointF.get(1).y - alignImgArray[mIndex].getHeight() / 2, mListPointF.get(1).x - alignImgArray[mIndex].getWidth() / 2 + alignImgArray[mIndex].getWidth(), mListPointF.get(1).y - alignImgArray[mIndex].getWidth() / 2 + alignImgArray[mIndex].getHeight());
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float targetX = event.getX();
        float targetY = event.getY();

        if (mListPointF != null) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN: {

                    canScalOrRotate = rotationRectF.contains(targetX, targetY);
                    canDel = deleteRectF.contains(targetX, targetY);
                    if (viewMode == VIEW_MODE_CAPTION) {
                        canAlignClick = alignRectF.contains(targetX, targetY);
                    }

                    if(mListener != null){
                        mListener.onTouchDown(new PointF(targetX, targetY));
                    }

                    // 判断手指是否在字幕框内
                    RectF r = new RectF();
                    Path path = new Path();
                    path.moveTo(mListPointF.get(0).x, mListPointF.get(0).y);
                    path.lineTo(mListPointF.get(1).x, mListPointF.get(1).y);
                    path.lineTo(mListPointF.get(2).x, mListPointF.get(2).y);
                    path.lineTo(mListPointF.get(3).x, mListPointF.get(3).y);
                    path.close();
                    path.computeBounds(r, true);
                    Region region = new Region();
                    region.setPath(path, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
                    canMove = region.contains((int)targetX, (int)targetY);

                    prePointF.x = targetX;
                    prePointF.y = targetY;

                    break;
                }

                case MotionEvent.ACTION_UP: {
                    canScalOrRotate = false;
                    canMove = false;
                    if(mListener != null && canDel){
                        mListener.onDel();
                    }
                    if (viewMode == VIEW_MODE_CAPTION && canAlignClick) {
                        mClickListener.onAlignClick();
                    }

                    canAlignClick = false;
                    canDel = false;
                    break;
                }


                case MotionEvent.ACTION_MOVE: {

                    // 防止移出屏幕
                    if(targetX <= 100 || targetX >= getWidth() - 100 || targetY >= getHeight() - 10 || targetY <= 10 ){
                        isOutScreen = true;
                        break;
                    }

                    if(isOutScreen){
                        isOutScreen = false;
                        break;
                    }

                    // 计算字幕框中心点
                    PointF centerPointF = new PointF();
                    centerPointF.x = (mListPointF.get(0).x + mListPointF.get(2).x) / 2;
                    centerPointF.y = (mListPointF.get(0).y + mListPointF.get(2).y) / 2;


                    if (canMove) {
                        mListener.onDrag(prePointF, new PointF(targetX, targetY));
                    }


                    if (canScalOrRotate) {

                        float offset = 1;
                        float angle = 0;

                        // 计算手指在屏幕上滑动的距离比例
                        double temp = Math.pow(prePointF.x - centerPointF.x, 2) + Math.pow(prePointF.y - centerPointF.y, 2);
                        double preLength = Math.sqrt(temp);
                        double temp2 = Math.pow(targetX - centerPointF.x, 2) + Math.pow(targetY - centerPointF.y, 2);
                        double length = Math.sqrt(temp2);
                        offset = (float) (length / preLength);

                        // 计算手指滑动的角度
                        float radian = (float) (Math.atan2(targetY - centerPointF.y, targetX - centerPointF.x)
                                - Math.atan2(prePointF.y - centerPointF.y, prePointF.x - centerPointF.x));
                        // 弧度转换为角度
                        angle = (float) (radian * 180 / Math.PI);

                        mListener.onScaleAndRotate(offset, new PointF(centerPointF.x, centerPointF.y), -angle);
                    }


                    prePointF.x = targetX;
                    prePointF.y = targetY;

                }
                break;
            }
        }

        return super.onTouchEvent(event);

    }

    public interface OnAlignClickListener {
        void onAlignClick();
    }



    public interface OnTouchListener {
        void onDrag(PointF prePointF, PointF nowPointF);
        void onScaleAndRotate(float scaleFactor, PointF anchor, float rotation);
        void onDel();
        void onTouchDown(PointF curPoint);
    }

}