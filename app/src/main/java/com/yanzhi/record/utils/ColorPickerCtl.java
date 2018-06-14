package com.yanzhi.record.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.yanzhi.record.R;


public class ColorPickerCtl extends LinearLayout {

    private ColorSeekBar mSelectColorSeekBar;
    private ColorSeekBar mSelectAlphaSeekBar;
    private View mSelectedColorView;
    private Button mConfirmBtn;
    private OnColorStateListener mListener;
    private int mCurColor;

    public ColorPickerCtl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.color_picker, this);
        InitUIView();
    }


    private void InitUIView(){
        mSelectColorSeekBar = (ColorSeekBar) findViewById(R.id.select_color_seek_bar);
        mSelectAlphaSeekBar = (ColorSeekBar) findViewById(R.id.select_alpha_seek_bar);
        mSelectedColorView = findViewById(R.id.selected_color_view);
        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
        mSelectColorSeekBar.setOnStateChangeListener(new ColorSeekBar.OnStateChangeListener() {
            @Override
            public void onColorChanged(int curColor) {
                mCurColor = curColor;
                int colorArray[] = {curColor, getColorWithAlpha(0, curColor)};
                mSelectAlphaSeekBar.setColorArray(colorArray);
                mSelectedColorView.setBackgroundColor(curColor);
            }

            @Override
            public void onProgressChanged(float progress) {

            }
        });
        mSelectAlphaSeekBar.setOnStateChangeListener(new ColorSeekBar.OnStateChangeListener() {
            @Override
            public void onColorChanged(int curColor) {
                mCurColor = curColor;

                mSelectedColorView.setBackgroundColor(curColor);
            }

            @Override
            public void onProgressChanged(float progress) {
                if(mListener != null){
                    mListener.selectedAlpha(progress/100);
                }
            }
        });

        mConfirmBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.selectedColor(mCurColor);
            }
        });
        mSelectColorSeekBar.setProgress(50);
    }

    public void SetSelectedColorChangedListener(OnColorStateListener listener){
        mListener = listener;
    }

    public interface OnColorStateListener{
        void selectedColor(int color);
        void selectedAlpha(float alpha);
    }

    /**
     * 对rgb色彩加入透明度
     * @param alpha     透明度，取值范围 0.0f -- 1.0f.
     * @param baseColor
     * @return a color with alpha made from base color
     */
    private  int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }
}
