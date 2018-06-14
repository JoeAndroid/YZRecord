package com.yanzhi.record.edit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.meicam.sdk.NvsAudioResolution;
import com.meicam.sdk.NvsClip;
import com.meicam.sdk.NvsMultiThumbnailSequenceView;
import com.meicam.sdk.NvsRational;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsVideoResolution;
import com.meicam.sdk.NvsVideoTrack;
import com.yanzhi.record.R;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineEditor;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineTimeSpan;

import java.util.ArrayList;

public class SingleClipEditActivity extends AppCompatActivity implements NvsStreamingContext.PlaybackCallback,NvsStreamingContext.PlaybackCallback2
{
    private static final int UPDATE_PLAYSTATE = 100;

    private ImageView m_backBtn;
    private TextView m_trimFinishBtn;
    private LiveWindow m_liveWindow;
    private ImageView m_playButton;
    private TextView m_curPlayTime;
    private SeekBar m_playSeekBar;
    private TextView m_totalDuaration;
    private NvsTimelineEditor m_timelineEditor;
    private TextView m_durationLeftSlider;
    private TextView m_durationDifference;
    private TextView m_durationRightSlider;

    private NvsStreamingContext m_streamingContext;
    private NvsTimeline m_timeline;
    private NvsVideoTrack m_videoTrack;

    private NvsMultiThumbnailSequenceView m_multiThumbnailSequenceView = null;
    private long m_trimIn;
    private long m_trimOut;
    private boolean isPlayOrSeekBarState = false;
    NvsTimelineTimeSpan m_timelineTimeSpan;

    private Handler m_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_PLAYSTATE:
                {
                    m_multiThumbnailSequenceView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                    seekTimeline(0, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    m_playButton.setBackgroundResource(R.mipmap.play);
                    updateSeekBar(0);
                    updateCurPlayTime(0);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取streamingContext实例对象
        m_streamingContext = NvsStreamingContext.getInstance();

        String path = getIntent().getStringExtra("current_path");
        m_trimIn = getIntent().getLongExtra("trim_in",0);
        m_trimOut = getIntent().getLongExtra("trim_out",1000000);
        setContentView(R.layout.activity_singleclip_edit);
        initUI();
        createTimeline(path);
        initSequenceView(path);
        addTimeSpan();
        updateTrimDuration(m_trimIn,m_trimOut);
        operationListener();
    }

    @Override
    public void onBackPressed() {
        destroy();
        super.onBackPressed();
    }

    private void destroy(){
        m_streamingContext.removeTimeline(m_timeline);
        m_streamingContext = null;
    }

    private void initUI(){
        m_backBtn = (ImageView) findViewById(R.id.clipEdit_back);
        m_trimFinishBtn = (TextView) findViewById(R.id.trimFinish);
        m_liveWindow = (LiveWindow) findViewById(R.id.clipEdit_liveWindow);
        m_playButton = (ImageView) findViewById(R.id.clipEdit_playbutton);
        m_curPlayTime = (TextView) findViewById(R.id.clipEdit_curPlayTime);
        m_playSeekBar = (SeekBar)findViewById(R.id.clipEdit_SeekBar);
        m_totalDuaration = (TextView) findViewById(R.id.clipEdit_totalDuaration);
        m_timelineEditor = (NvsTimelineEditor) findViewById(R.id.timelineEditor);
        m_durationLeftSlider = (TextView) findViewById(R.id.durationLeftSlider);
        m_durationDifference = (TextView) findViewById(R.id.durationDifference);
        m_durationRightSlider = (TextView) findViewById(R.id.durationRightSlider);

        m_multiThumbnailSequenceView = m_timelineEditor.getMultiThumbnailSequenceView();
    }

    private void createTimeline(String path){
        NvsVideoResolution videoEditRes = new NvsVideoResolution();
        videoEditRes.imageWidth = 1280;
        videoEditRes.imageHeight = 720;
        videoEditRes.imagePAR = new NvsRational(1, 1);

        NvsRational videoFps = new NvsRational(25, 1);
        NvsAudioResolution audioEditRes = new NvsAudioResolution();
        audioEditRes.sampleRate = 48000;
        audioEditRes.channelCount = 2;
        // 创建时间线
        m_timeline = m_streamingContext.createTimeline(videoEditRes, videoFps, audioEditRes);
        //将时间线连接到LiveWindow控件
        m_streamingContext.connectTimelineWithLiveWindow(m_timeline, m_liveWindow);
        m_streamingContext.setPlaybackCallback(this);//设置播放回调接口
        m_streamingContext.setPlaybackCallback2(this);
        //添加视频轨道
        m_videoTrack = m_timeline.appendVideoTrack();
        //获取视频路径，添加视频片段
        m_videoTrack.appendClip(path);
        m_totalDuaration.setText(formatTimeStrWithUs(m_timeline.getDuration()));
        m_playSeekBar.setMax((int)m_timeline.getDuration());
    }

    private void operationListener(){
        m_backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroy();
                finish();
            }
        });

        m_trimFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //引擎停止
                m_streamingContext.stop();
                Intent intent = new Intent();
                intent.putExtra("TRIM_IN", m_trimIn);
                intent.putExtra("TRIM_OUT", m_trimOut);
                setResult(RESULT_OK, intent);
                destroy();
                finish();
            }
        });
        m_playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断当前引擎状态是否是播放状态
                if(getCurrentEngineState() != m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    long startTime = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                    // 播放视频
                    m_streamingContext.playbackTimeline(m_timeline, startTime, m_timeline.getDuration(), NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
                    m_playButton.setBackgroundResource(R.mipmap.pause);
                } else {
                    stopEngine();
                }
            }
        });
         /* 对ThumbnailSequenceView的滑动监听 */
        m_timelineEditor.setOnScrollListener(new NvsTimelineEditor.OnScrollChangeListener(){
            @Override
            public void onScrollX(long timeStamp){
                if(isPlayOrSeekBarState)
                    return;

                seekTimeline(timeStamp, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                updateSeekBar(timeStamp);
                updateCurPlayTime(timeStamp);
            }
        });

        m_playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    isPlayOrSeekBarState = true;
                    seekTimeline(i, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    if(m_multiThumbnailSequenceView != null){
                        m_multiThumbnailSequenceView.smoothScrollTo(Math.round((float)(i / (float) m_timeline.getDuration() * m_timelineEditor.getSequenceWidth())),0);
                    }
                    updateCurPlayTime(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        m_multiThumbnailSequenceView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                isPlayOrSeekBarState = false;
                return false;
            }
        });
    }
    private void initSequenceView(String mediaPath) {
        ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> infoDescArray = new ArrayList<>();
        NvsClip curClip = m_videoTrack.getClipByIndex(0);
        NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc infoDesc = new NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc();
        infoDesc.mediaFilePath = mediaPath;
        infoDesc.trimIn = curClip.getTrimIn();
        infoDesc.trimOut = curClip.getTrimOut();
        infoDesc.inPoint = curClip.getInPoint();
        infoDesc.outPoint = curClip.getOutPoint();
        infoDesc.stillImageHint = false;
        infoDescArray.add(infoDesc);

        m_timelineEditor.initTimelineEditor(infoDescArray,m_timeline.getDuration());
    }
    private void addTimeSpan(){
        m_timelineTimeSpan = m_timelineEditor.addTimeSpan(m_trimIn,m_trimOut);
        m_timelineTimeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimInChangeListener() {
            @Override
            public void onChange(long timeStamp, boolean isDragEnd) {
                updateTrimDuration(timeStamp,m_trimOut);
                m_trimIn = timeStamp;
                seekTimeline(timeStamp, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });

        m_timelineTimeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimOutChangeListener() {
            @Override
            public void onChange(long timeStamp, boolean isDragEnd) {
                updateTrimDuration(m_trimIn,timeStamp);
                m_trimOut = timeStamp;
                seekTimeline(timeStamp-1, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });
    }

    @Override
    public void onPlaybackPreloadingCompletion(NvsTimeline var1) {

    }

    @Override
    public void onPlaybackStopped(NvsTimeline var1) {
        m_playButton.setBackgroundResource(R.mipmap.play);
    }
    public void onPlaybackEOF(NvsTimeline var1) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                m_handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = UPDATE_PLAYSTATE;
                        m_handler.sendMessage(message);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onPlaybackTimelinePosition(NvsTimeline timeline, long position){
        if(m_multiThumbnailSequenceView != null){
            isPlayOrSeekBarState = true;
            m_multiThumbnailSequenceView.smoothScrollTo(Math.round((float)(position / (float) m_timeline.getDuration() * m_timelineEditor.getSequenceWidth())),0);
        }
        updateSeekBar((int)position);
        updateCurPlayTime(position);
    }

    private void stopEngine() {
        m_playButton.setBackgroundResource(R.mipmap.play);
        if(m_streamingContext != null) {
            m_streamingContext.stop();//停止播放
        }
    }

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return m_streamingContext.getStreamingEngineState();
    }
    private void seekTimeline(long timestamp, int seekShowMode) {
        /* seekTimeline
        * param1: 当前时间线
        * param2: 时间戳 取值范围为  [0, timeLine.getDuration()) (左闭右开区间)
        * param3: 图像预览模式
        * param4: 引擎定位的特殊标志
        * */
        m_streamingContext.seekTimeline(m_timeline,timestamp, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, seekShowMode);
    }
    private void updateCurPlayTime(long time) {
        m_curPlayTime.setText(formatTimeStrWithUs(time));
    }

    private void updateSeekBar(long timeStamp){
        m_playSeekBar.setProgress((int)timeStamp);
    }
    private void updateTrimDuration(long trimInDuration,long trimOutDuration){
        m_durationLeftSlider.setText(formatTimeStrWithUs(trimInDuration));
        long durationDifference = trimOutDuration - trimInDuration;
        m_durationDifference.setText(formatTimeStrWithUs(durationDifference));
        m_durationRightSlider.setText(formatTimeStrWithUs(trimOutDuration));
    }

    /*格式化时间(us)*/
    private String formatTimeStrWithUs(long us) {
        int second = (int)(us / 1000000.0 + 0.5);
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String timeStr;
        if (us == 0) {
            timeStr = "00:00";
        }
        if (hh > 0) {
            timeStr = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            timeStr = String.format("%02d:%02d", mm, ss);
        }
        return timeStr;
    }
}
