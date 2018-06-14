package com.yanzhi.record.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yanzhi.record.R;

import pl.droidsonroids.gif.GifImageView;

/**
 * Created by admin on 2017/11/28.
 */

public class CompileVideo extends LinearLayout {
    private GifImageView m_compileAnimatGif;
    private TextView m_compileProgress;
    private LinearLayout m_compileFinish;
    private LinearLayout m_compileIn;
    private ImageView m_cancelCompile;

    OnCompileStopListener m_onCompileStopListener = null;

    public CompileVideo(Context context, AttributeSet attrs){
        super(context,attrs);
        View compileView = LayoutInflater.from(context).inflate(R.layout.compile_video, this);
        m_compileAnimatGif = (GifImageView) compileView.findViewById(R.id.compileAnimateGif);
        m_compileProgress = (TextView) compileView.findViewById(R.id.compileProgress);
        m_compileFinish = (LinearLayout) compileView.findViewById(R.id.compileFinish);
        m_compileIn = (LinearLayout) compileView.findViewById(R.id.compileIn);
        m_cancelCompile = (ImageView) compileView.findViewById(R.id.cancelCompile);
        m_cancelCompile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(m_onCompileStopListener != null){
                    m_onCompileStopListener.onCompileStop(v);
                }
            }
        });
    }

    public void setCompileStopListener(OnCompileStopListener compileStopListener){
        m_onCompileStopListener = compileStopListener;
    }

    public void compileVideoInitState(){
        updateCompileProgress(0);
        m_compileIn.setVisibility(View.VISIBLE);
        m_cancelCompile.setVisibility(View.VISIBLE);
        m_compileFinish.setVisibility(View.INVISIBLE);
    }

    public void compileVideoFinishState(){
        m_compileIn.setVisibility(View.INVISIBLE);
        m_cancelCompile.setVisibility(View.INVISIBLE);
        m_compileFinish.setVisibility(View.VISIBLE);
    }
    public void updateCompileProgress(int progress){
        m_compileProgress.setText("已生成" + String.valueOf(progress) + "%");
    }

    public interface OnCompileStopListener {
        public void onCompileStop(View v);
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }
}
