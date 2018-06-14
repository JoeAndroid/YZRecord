package com.yanzhi.record.edit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.meicam.sdk.NvsAudioResolution;
import com.meicam.sdk.NvsLiveWindow;
import com.meicam.sdk.NvsRational;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoResolution;
import com.meicam.sdk.NvsVideoTrack;
import com.yanzhi.record.R;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class TrimActivity extends AppCompatActivity implements NvsStreamingContext.PlaybackCallback2,
        NvsStreamingContext.PlaybackCallback, NvsStreamingContext.CompileCallback {
    private static final String TAG = "Edit";
    private static final int SINGLE_CLIP_EDIT = 101;
    private static final int UPDATE_PLAY_SEEKBAR = 102;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 103;
    private String m_compilePath;

    private ImageView m_backBtn;
    private TextView m_compileBtn;
    private NvsLiveWindow m_liveWindow;
    private ImageView m_playButton;
    private TextView m_curPlayTime;
    private SeekBar m_playSeekBar;
    private TextView m_totalDuaration;
    private SeekBar m_changeSpeed;
    private RecyclerView m_recycleViewVideoList;

    private LinearLayout m_compilePage;
    private CompileVideo m_compileLinearLayout;

    private ArrayList<String> arrayFilePath;
    private List<VideoInfoDescription> m_videoInfoDescList;

    private RecyclerViewAdapter m_videoListRecycleAdapter;
    private ItemTouchHelper m_itemTouchHelper;

    private int m_currentPosition = 0;
    //
    private NvsStreamingContext m_streamingContext;
    private NvsTimeline m_timeline = null;
    private NvsVideoTrack m_videoTrack;
    private CountDownTimer m_countDownTimer;

    NvsVideoResolution m_videoEditRes = new NvsVideoResolution();
    private int m_ratio = 0;
    private int m_QR = 0;
    private boolean m_fromCap = true;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PLAY_SEEKBAR:
                    resetUIState();
                    break;
            }
        }
    };

    public static class VideoInfoDescription {
        public String videoFilePath;
        public String mVideoIndex;

        public VideoInfoDescription() {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //流媒体初始化
        m_streamingContext = NvsStreamingContext.getInstance();
        setContentView(R.layout.activity_trim);
        getDataFromIntent();
        initView();
        initTimeline();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) m_liveWindow.getLayoutParams();
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;
        if (m_fromCap) {
            params.width = width;
            params.height = width;
        } else {
            switch (m_ratio) {
                case 0:
                    params.width = width;
                    params.height = width * 9 / 16;
                    break;
                case 1:
                    params.width = width;
                    params.height = width * 3 / 4;
                    break;
                case 2:
                    params.width = width;
                    params.height = width;
                    break;
            }
        }
        m_liveWindow.setLayoutParams(params);
        m_liveWindow.setFillMode(NvsLiveWindow.FILLMODE_PRESERVEASPECTFIT);

        initRecycleViewAdaper(arrayFilePath);
        operationListener();
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        arrayFilePath = getIntent().getStringArrayListExtra("videoFilePathArray");
        m_ratio = intent.getIntExtra("ratio", 0);
        m_QR = intent.getIntExtra("QR", 0);
        m_fromCap = intent.getBooleanExtra("fromCap", true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SINGLE_CLIP_EDIT:
                if (resultCode == RESULT_OK) {
                    long trim_in = data.getLongExtra("TRIM_IN", 0);
                    long trim_out = data.getLongExtra("TRIM_OUT", 1000000);
                    NvsVideoClip clip = m_videoTrack.getClipByIndex(m_currentPosition);
                    /*
                        裁剪！改变片段的入出点，裁剪片段。第二个参数请设置为true。
                    */
                    clip.changeTrimInPoint(trim_in, true);
                    clip.changeTrimOutPoint(trim_out, true);
                    resetUIState();
                }
                break;
        }
    }

    private void initRecycleViewAdaper(ArrayList<String> arrayPaths) {
        for (int index = 0; index < arrayPaths.size(); ++index) {
            initRecycleViewData(arrayPaths.get(index));
        }
        resetUIState();

        LinearLayoutManager layoutManager = new LinearLayoutManager(TrimActivity.this);
        m_recycleViewVideoList.setLayoutManager(layoutManager);
        m_recycleViewVideoList.addItemDecoration(new SpaceItemDecoration(0, 4));
        m_videoListRecycleAdapter = new RecyclerViewAdapter(TrimActivity.this, m_videoInfoDescList);
        m_recycleViewVideoList.setAdapter(m_videoListRecycleAdapter);

        m_itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {

            /**
             * 是否处理滑动事件 以及拖拽和滑动的方向 如果是列表类型的RecyclerView的只存在UP和DOWN，如果是网格类RecyclerView则还应该多有LEFT和RIGHT
             * @param recyclerView
             * @param viewHolder
             * @return
             */
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                    final int swipeFlags = 0;
                    return makeMovementFlags(dragFlags, swipeFlags);
                } else {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    final int swipeFlags = 0;
//                    final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, swipeFlags);
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //得到当拖拽的viewHolder的Position
                int fromPosition = viewHolder.getAdapterPosition();
                //拿到当前拖拽到的item的viewHolder
                int toPosition = target.getAdapterPosition();
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(m_videoInfoDescList, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(m_videoInfoDescList, i, i - 1);
                    }
                }

                m_videoListRecycleAdapter.notifyItemMoved(fromPosition, toPosition);
                moveClip(fromPosition, toPosition);
                resetUIState();
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

            /**
             * 重写拖拽可用
             * @return
             */
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            /**
             * 长按选中Item的时候开始调用
             *
             * @param viewHolder
             * @param actionState
             */
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    RelativeLayout mSelItemTopShadow = (RelativeLayout) viewHolder.itemView.findViewById(R.id.itembg_topShadow);
                    mSelItemTopShadow.setBackgroundResource(R.mipmap.itembgselected);
                    RelativeLayout mSelItemBottomShadow = (RelativeLayout) viewHolder.itemView.findViewById(R.id.itembg_bottomShadow);
                    mSelItemBottomShadow.setBackgroundResource(R.mipmap.itembgselected);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            /**
             * 手指松开的时候还原
             * @param recyclerView
             * @param viewHolder
             */
            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                RelativeLayout mSelItemTopShadow = (RelativeLayout) viewHolder.itemView.findViewById(R.id.itembg_topShadow);
                mSelItemTopShadow.setBackgroundResource(R.mipmap.itembg);
                RelativeLayout mSelItemBottomShadow = (RelativeLayout) viewHolder.itemView.findViewById(R.id.itembg_bottomShadow);
                mSelItemBottomShadow.setBackgroundResource(R.mipmap.itembg);

                m_videoListRecycleAdapter.notifyDataSetChanged();
            }
        });
        m_itemTouchHelper.attachToRecyclerView(m_recycleViewVideoList);

        m_videoListRecycleAdapter.setOnItemLongPressListener(new RecyclerViewAdapter.OnItemLongPressListener() {
            @Override
            public void onItemLongPress(RecyclerViewAdapter.ViewHolder holder, int pos) {
                //停止引擎
                if (getCurrentEngineState() == m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    stopEngine();
                }
                m_itemTouchHelper.startDrag(holder);
                //获取系统震动服务
                Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);//震动70毫秒
                vib.vibrate(70);
            }
        });

        m_videoListRecycleAdapter.setOnEditClickListener(new RecyclerViewAdapter.OnEditClickListener() {
            @Override
            public void onEditClick(View view, int pos) {
                //停止引擎
                if (getCurrentEngineState() == m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    stopEngine();
                }

                Intent intent = new Intent(TrimActivity.this, SingleClipEditActivity.class);
                intent.putExtra("current_path", m_videoInfoDescList.get(pos).videoFilePath);

                m_currentPosition = pos;
                //获取index值所对应的视频片段
                NvsVideoClip clip = m_videoTrack.getClipByIndex(pos);
                if (clip != null) {
                    //获取裁剪入点值
                    long trimIn = clip.getTrimIn();
                    //获取裁剪出点值
                    long trimOut = clip.getTrimOut();
                    intent.putExtra("trim_in", trimIn);
                    intent.putExtra("trim_out", trimOut);
                }
                startActivityForResult(intent, SINGLE_CLIP_EDIT);
            }
        });

        m_videoListRecycleAdapter.setOnSortClickListener(new RecyclerViewAdapter.OnSortClickListener() {
            @Override
            public void onSortClick(View view, int pos) {

            }
        });

        m_videoListRecycleAdapter.setOnDeleteClickListener(new RecyclerViewAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(View view, int pos) {
                //停止引擎
                if (getCurrentEngineState() == m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    stopEngine();
                }
                m_videoInfoDescList.remove(pos);
                updateClipIndex();
                //移除视频片段
                if (!m_videoTrack.removeClip(pos, false))
                    Log.e(TAG, "removeClip failed");

                m_videoListRecycleAdapter.notifyDataSetChanged();
                resetUIState();
                if (m_videoListRecycleAdapter.getItemCount() == 0) {
                    m_playButton.setEnabled(false);
                    m_changeSpeed.setEnabled(false);
                    m_compileBtn.setEnabled(false);
                    //清空视频帧
                    m_liveWindow.clearVideoFrame();
                }
            }
        });
    }

    private void operationListener() {
        m_backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroy();
                finish();
            }
        });

        m_compileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compileVideo();
            }
        });

        m_playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断当前引擎状态是否是播放状态
                if (getCurrentEngineState() != m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    long startTime = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                    // 播放视频
                    m_streamingContext.playbackTimeline(m_timeline, startTime, m_timeline.getDuration(), NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
                    m_playButton.setBackgroundResource(R.mipmap.pause);
                } else {
                    stopEngine();
                }
            }
        });

        m_playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    seekTimeline(i, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
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


        m_changeSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 3) {
                    for (int i = 0; i < m_videoTrack.getClipCount(); i++) {
                        //索引遍历视频片段
                        NvsVideoClip clip = m_videoTrack.getClipByIndex(i);
                        // 改变片段的播放速度
                        clip.changeSpeed(Math.pow(2, progress - 3));
                    }
                } else {
                    for (int i = 0; i < m_videoTrack.getClipCount(); i++) {
                        //索引遍历视频片段
                        NvsVideoClip clip = m_videoTrack.getClipByIndex(i);
                        // 改变片段的播放速度
                        clip.changeSpeed(Math.pow(0.5, 3 - progress));
                    }
                }

                resetUIState();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        m_compileLinearLayout.setCompileStopListener(new CompileVideo.OnCompileStopListener() {
            @Override
            public void onCompileStop(View v) {
                if (getCurrentEngineState() == NvsStreamingContext.STREAMING_ENGINE_STATE_COMPILE) {
                    m_streamingContext.stop();//引擎停止
                    m_compilePage.setVisibility(View.GONE);
                    m_compileLinearLayout.compileVideoInitState();
                    m_countDownTimer.cancel();
                }
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

    @Override
    public void onPlaybackTimelinePosition(NvsTimeline timeline, long position) {
        m_playSeekBar.setProgress((int) position);
        updateCurPlayTime(position);
    }

    @Override
    public void onPlaybackEOF(NvsTimeline var1) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = UPDATE_PLAY_SEEKBAR;

                        handler.sendMessage(message);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onCompileProgress(NvsTimeline timeline, int progress) {
        m_compileLinearLayout.updateCompileProgress(progress);
    }

    @Override
    public void onCompileFinished(NvsTimeline timeline) {
        m_compileLinearLayout.compileVideoFinishState();
        m_countDownTimer.start();
        EditActivity.actionStart(this, new ArrayList<String>(Arrays.asList(m_compilePath)), m_ratio, m_QR, m_fromCap);
//        EditActivity.actionStart(this, new ArrayList<String>(Arrays.asList(Environment.getExternalStorageDirectory() + "/NvStreamingSdk" + File.separator + "Compile/video2.mp4")));
//        Toast.makeText(this, "生成文件：NvStreamingSdk/Compile/video.mp4", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCompileFailed(NvsTimeline timeline) {
        m_compilePage.setVisibility(View.GONE);
        m_compileLinearLayout.compileVideoInitState();
    }

    @Override
    public void onResume() {
        m_streamingContext.setCompileCallback(this);//设置生成回调接口
        m_streamingContext.setPlaybackCallback(this);//设置播放回调接口
        m_streamingContext.setPlaybackCallback2(this);

        super.onResume();
    }

    @Override
    public void onBackPressed() {
        destroy();
        super.onBackPressed();
    }

    private void compileVideo() {
        m_compilePage.setVisibility(View.VISIBLE);
        int state = getCurrentEngineState();
        //停止引擎
        if (state == m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
            stopEngine();
        }

        /*
            compileTimeline用于将时间线上的片段生成输出一个素材。
            参数1，时间线。
            参数2，开始时间，从时间线上哪个时间开始生成。一般从0开始生成。
            参数3，结束时间，时间线上哪个时间作为结束时间，一般是整个时间线的时长。
            参数4，生成素材的路径。
            参数5，生成素材的分辨率的高。可以是360，480，720，1080。和Timeline的分辨率共同决定了生成素材的分辨率。假如此处高是480，低于Timeline的高，
            那么生成的素材的分辨率的高就是480，宽是480乘以Timeline的分辨率的横纵比。如果此处的高大于timeline的分辨率的高，则生成视频的分辨率就是Timeline
            的分辨率。
            参数6，码率级别，高，中，低。决定了生成素材的清晰度，级别越高，码率越大，越清晰，视频文件也越大。
            所以一般是生成到本地的素材，采用高或中。准备发布到服务器上的素材，生成为中或低，节省CDN费用。
            生成素材的分辨率也会影响码率，分辨率越大，码率越大，视频文件也越大，请根据使用场景自行决定。
            参数7，flag。一般设置为0。除非特例非常介意文件的大小，又完全不介意生成文件所花的时间，可设置STREAMING_ENGINE_COMPILE_FLAG_DISABLE_HARDWARE_ENCODER。
            将禁止硬件编码器而采用软件编码器，文件大小降低一半，但生成时间是原来的2至3倍。请根据使用场景自行设定。
         */
        m_streamingContext.compileTimeline(m_timeline, 0, m_timeline.getDuration(), m_compilePath, NvsStreamingContext.COMPILE_VIDEO_RESOLUTION_GRADE_1080, NvsStreamingContext.COMPILE_BITRATE_GRADE_HIGH, 0);
    }

    private void moveClip(int startPos, int endPos) {
        //移动片段
        m_videoTrack.moveClip(startPos, endPos);
        //定位预览视频图像
        seekTimeline(0, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    private void initView() {
        m_backBtn = findViewById(R.id.back);
        m_compileBtn = findViewById(R.id.compilevideo);
        m_playButton = findViewById(R.id.play_button);
        m_liveWindow = findViewById(R.id.live_window);
        m_curPlayTime = findViewById(R.id.curPlayTime);
        m_playSeekBar = findViewById(R.id.playSeekBar);
        m_totalDuaration = findViewById(R.id.totalDuaration);
        m_changeSpeed = findViewById(R.id.changeSpeed_seekBar);
        m_changeSpeed.setSelected(true);
        m_recycleViewVideoList = findViewById(R.id.video_recycleview);

        m_videoInfoDescList = new ArrayList<>();
        m_compilePage = (LinearLayout) findViewById(R.id.compilePage);
        m_compileLinearLayout = (CompileVideo) findViewById(R.id.compileLinearLayout);
        m_countDownTimer = new CountDownTimer(500, 500) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                m_compilePage.setVisibility(View.GONE);
                m_compileLinearLayout.compileVideoInitState();
            }
        };

        m_compileLinearLayout.updateCompileProgress(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                getStorageFilePath();
            } else {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
            }
        } else {
            getStorageFilePath();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return;

        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE:
                getStorageFilePath();
                break;
        }
    }

    public static void actionStart(Context context, ArrayList<String> clipPath, int ratio, int QR, boolean fromCap) {
        Intent intent = new Intent(context, TrimActivity.class);
        intent.putStringArrayListExtra("videoFilePathArray", clipPath);
        intent.putExtra("ratio", ratio);
        intent.putExtra("QR", QR);
        intent.putExtra("fromCap", fromCap);
        context.startActivity(intent);
    }

    //初始化时间线
    private void initTimeline() {
        /*
        注意：请在使用LiveWindow预览的时候，将NvsLiveWindow的横纵比与此处保持一致，如果此处设置为16比9,LiveWindow的横纵比也需要16比9
        如果此处是1比1的方形视频，请在使用NvsLiveWindow的时候宽/高也是1比1
        */
        setRatio();
        NvsRational videoFps = new NvsRational(25, 1); /*帧速率，代表1秒播出多少帧画面，一般设25帧，也可设为30 */

        NvsAudioResolution audioEditRes = new NvsAudioResolution();
        audioEditRes.sampleRate = 44100; /*音频采样率，可以是44100，或者48000*/
        audioEditRes.channelCount = 2; /*音频通道数,一般是2*/

        /*创建时间线*/
        m_timeline = m_streamingContext.createTimeline(m_videoEditRes, videoFps, audioEditRes);
        //将timeline连接到LiveWindow控件
        m_streamingContext.connectTimelineWithLiveWindow(m_timeline, m_liveWindow);
        /*添加视频轨道，如果不做画中画，添加一条视频轨道即可*/
        m_videoTrack = m_timeline.appendVideoTrack();
    }

    private void setRatio() {
        switch (m_ratio) {
            case 0: {
                switch (m_QR) {
                    case 0:
                        m_videoEditRes.imageWidth = m_fromCap ? 1080 : 1920;
                        m_videoEditRes.imageHeight = m_fromCap ? 1920 : 1080;
                        break;
                    case 1:
                        m_videoEditRes.imageWidth = m_fromCap ? 720 : 1280;
                        m_videoEditRes.imageHeight = m_fromCap ? 1280 : 720;
                        break;
                    case 2:
                        m_videoEditRes.imageWidth = m_fromCap ? 540 : 960;
                        m_videoEditRes.imageHeight = m_fromCap ? 960 : 540;
                        break;
                }
                break;
            }
            case 1: {
                switch (m_QR) {
                    case 0:
                        m_videoEditRes.imageWidth = m_fromCap ? 1080 : 1440;
                        m_videoEditRes.imageHeight = m_fromCap ? 1440 : 1080;
                        break;
                    case 1:
                        m_videoEditRes.imageWidth = m_fromCap ? 720 : 960;
                        m_videoEditRes.imageHeight = m_fromCap ? 960 : 720;
                        break;
                    case 2:
                        m_videoEditRes.imageWidth = m_fromCap ? 480 : 640;
                        m_videoEditRes.imageHeight = m_fromCap ? 640 : 480;
                        break;
                }
                break;
            }
            case 2: {
                switch (m_QR) {
                    case 0:
                        m_videoEditRes.imageWidth = 1080;
                        m_videoEditRes.imageHeight = 1080;
                        break;
                    case 1:
                        m_videoEditRes.imageWidth = 720;
                        m_videoEditRes.imageHeight = 720;
                        break;
                    case 2:
                        m_videoEditRes.imageWidth = 480;
                        m_videoEditRes.imageHeight = 480;
                        break;
                }
                break;
            }
        }
        m_videoEditRes.imagePAR = new NvsRational(1, 1);
    }

    private void initRecycleViewData(String mediaPath) {
        /*
            通过素材的路径将素材添加到轨道上，添加到轨道上的每个素材我们称其为片段。如果要将多个素材合并生成
            一个素材，就需要将素材逐个添加到轨道上。然后利用compileTimeline方法生成。
        */
        NvsVideoClip clip = m_videoTrack.appendClip(mediaPath);
        if (clip == null)
            return;

        if (m_videoTrack.getClipCount() > 0
                && m_videoListRecycleAdapter != null
                && m_videoListRecycleAdapter.getItemCount() == 0) {
            m_compileBtn.setEnabled(true);
            m_playButton.setEnabled(true);
            m_changeSpeed.setEnabled(true);
        }
        VideoInfoDescription videoInfoDesc = new VideoInfoDescription();
        videoInfoDesc.videoFilePath = mediaPath;
        videoInfoDesc.mVideoIndex = String.valueOf(m_videoTrack.getClipCount());
        m_videoInfoDescList.add(videoInfoDesc);
    }

    public void getStorageFilePath() {
        //要生成视频的路径
        File compileDir = new File(Environment.getExternalStorageDirectory(), "NvStreamingSdk" + File.separator + "Compile");
        if (!compileDir.exists() && !compileDir.mkdirs()) {
            Log.d(TAG, "Failed to make Compile directory");
            return;
        }

        File file = new File(compileDir, "video.mp4");
        if (file.exists())
            file.delete();
        m_compilePath = file.getAbsolutePath();
    }

    private void updateClipIndex() {
        for (int index = 0; index < m_videoInfoDescList.size(); ++index) {
            m_videoInfoDescList.get(index).mVideoIndex = String.valueOf(index + 1);
        }
    }

    private void resetUIState() {
        m_playSeekBar.setProgress(0);
        m_playSeekBar.setMax((int) m_timeline.getDuration());
        updateCurPlayTime(0);
        updateTotalDuration(m_timeline.getDuration());
        //定位预览视频图像
        seekTimeline(0, NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return m_streamingContext.getStreamingEngineState();
    }

    void destroy() {
        m_streamingContext.removeTimeline(m_timeline);
        m_streamingContext = null;
    }

    private void stopEngine() {
        m_playButton.setBackgroundResource(R.mipmap.play);
        if (m_streamingContext != null) {
            m_streamingContext.stop();//停止播放
        }
    }

    private void seekTimeline(long timestamp, int seekShowMode) {
        /* seekTimeline
         * param1: 当前时间线
         * param2: 时间戳 取值范围为  [0, timeLine.getDuration()) (左闭右开区间)
         * param3: 图像预览模式
         * param4: 引擎定位的特殊标志
         * */
        m_streamingContext.seekTimeline(m_timeline, timestamp, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, seekShowMode);
    }

    private void updateTotalDuration(long totalDuration) {
        m_totalDuaration.setText(formatTimeStrWithUs(totalDuration));
    }

    private void updateCurPlayTime(long time) {
        m_curPlayTime.setText(formatTimeStrWithUs(time));
    }

    /*格式化时间(us)*/
    private String formatTimeStrWithUs(long us) {
        int second = (int) (us / 1000000.0 + 0.5);
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


    @Override
    protected void onDestroy() {
        m_streamingContext = null;
//        NvsStreamingContext.close();
        super.onDestroy();
    }
}