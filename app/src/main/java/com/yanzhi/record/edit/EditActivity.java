package com.yanzhi.record.edit;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.meicam.sdk.NvsAssetPackageManager;
import com.meicam.sdk.NvsAudioResolution;
import com.meicam.sdk.NvsClip;
import com.meicam.sdk.NvsColor;
import com.meicam.sdk.NvsLiveWindow;
import com.meicam.sdk.NvsMultiThumbnailSequenceView;
import com.meicam.sdk.NvsRational;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsTimelineAnimatedSticker;
import com.meicam.sdk.NvsTimelineCaption;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoResolution;
import com.meicam.sdk.NvsVideoTrack;
import com.yanzhi.record.R;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineEditor;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineTimeSpan;
import com.yanzhi.record.network.NetWorkConfig;
import com.yanzhi.record.network.OkHttpUtils;
import com.yanzhi.record.network.bean.SubtitleBean;
import com.yanzhi.record.network.callback.StringCallback;
import com.yanzhi.record.network.utils.GsonUtil;
import com.yanzhi.record.utils.CommonDialog;
import com.yanzhi.record.utils.DrawRect;
import com.yanzhi.record.utils.EditCaptionTab;
import com.yanzhi.record.utils.EditStickerCtl;
import com.yanzhi.record.utils.TimeSpanInfo;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;

import static com.yanzhi.record.utils.GlobalData.VIEW_MODE_CAPTION;
import static com.yanzhi.record.utils.GlobalData.VIEW_MODE_STICEER;

public class EditActivity extends Activity implements NvsStreamingContext.StreamingEngineCallback,
        NvsStreamingContext.PlaybackCallback, NvsStreamingContext.PlaybackCallback2, NvsStreamingContext.CompileCallback, EditCaptionTab.OnEditCaptionListener, EditStickerCtl.OnEditStickerListener, DrawRect.OnTouchListener, DrawRect.OnAlignClickListener {

    private String TAG = "EditActivity";
    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final int UPDATE_PLAY_SEEKBAR = 2;
    private static final int UPLOAD_AUDIO = 3;
    private static final int UPDATE_SUBTITLE = 4;

    private LiveWindow mLiveWindow;
    //    private Button mAddVideo;
    private ImageButton mPlayBtn;
    private RelativeLayout mLiveWindowBg;
    private SeekBar mPlayBtnSeekBar;
    private TextView mCurPlayTime;
    private TextView mAllPlayTime;
    private RelativeLayout mAddAssetLayout;
    private LinearLayout mEditLayout;

    private TextView tvCompileVideo;

    private Button mAddCaptionBtn;
    private Button mAddStickerBtn;
    private Button mEditStickerBtn;
    private Button mEditCaptionBtn;
    private ImageView mEditCaptionBtnBtnImg;
    private ImageView mEditStickerBtnBtnImg;
    private ImageButton mEditCompleteBtn;
    private EditCaptionTab mEditCaptionTab;
    private EditStickerCtl mEditStickerCtl;
    private TextView mEditTitle;
    private TextView mOperatorTip;

    private DrawRect mDrawRect;
    private boolean mHasAddVideo = false;

    private LinearLayout mSequenceView;

    private int mMinFont = 18;
    private float mEspinon = 0.000001f;
    private float mMinScale = 0.20f;
    private static long NS_TIME_BASE = 1000000;

    private LinearLayout mEditCaptionBtnTab;
    private LinearLayout mEditStickerBtnCtl;

    private NvsStreamingContext mStreamingContext;
    private NvsTimeline mTimeline;
    private NvsVideoTrack mVideoTrack;

    private List<NvsVideoClip> mClipList = new ArrayList<>();
    private boolean mHasAddedCaption = false;
    private boolean mEditCaptionBtnMode = false;
    private boolean mHasAddedSticker = false;
    private boolean mEditStickerBtnMode = false;

    private int mViewMode = VIEW_MODE_CAPTION;

    private NvsTimelineEditor mTimelineEditor;
    private NvsMultiThumbnailSequenceView multiThumbnailSequenceView = null;
    private boolean isPlayOrSeekBar = false;
    private NvsTimelineCaption mCurCaption = null;
    private NvsTimelineAnimatedSticker mCurSticker = null;
    private List<TimeSpanInfo> mTimeSpanInfoList = new ArrayList<TimeSpanInfo>();

    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;

    private List<SubtitleBean> subtitleList;
    private String m_compilePath;

    NvsVideoResolution m_videoEditRes = new NvsVideoResolution();
    private int m_ratio = 0;
    private int m_QR = 0;
    private boolean m_fromCap = true;

    private float dist;

    private String themePackagePath;
    private String fxPackagePath;

    private ProgressDialog progressDialog;

    private boolean isAddCaptionSuccess = false;

    StringBuilder mCaptionStyles[] = {
            new StringBuilder(),
            new StringBuilder(),
            new StringBuilder(),
            new StringBuilder(),
    };

    StringBuilder mStickerStyles[] = {
            new StringBuilder(),
            new StringBuilder(),
            new StringBuilder(),
    };

    private Handler m_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case UPDATE_PLAY_SEEKBAR: {
                    multiThumbnailSequenceView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                    seekTimeline(0, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    mPlayBtn.setBackgroundResource(R.mipmap.play);
                    updateCurPlayTime(0);
                }
                break;
                case UPLOAD_AUDIO:
                    getSubtitleByServer(new File(SDCARD_PATH + "/output_audio.m4a"));
                    break;
                case UPDATE_SUBTITLE:
                    for (int i = 0; i < subtitleList.size() - 1; i++) {
                        addCaption(subtitleList.get(i));
                        if (i == 0) {
                            NvsTimelineCaption captionNow = mTimeline.getFirstCaption();
                            List<PointF> list = captionNow.getBoundingRectangleVertices();    // 获取字幕的原始包围矩形框变换后的顶点位置
                            PointF point1 = list.get(0);
                            PointF point2 = list.get(1);
                            PointF f1 = mLiveWindow.mapCanonicalToView(point1);
                            PointF f2 = mLiveWindow.mapCanonicalToView(point2);
                            if (m_fromCap) {
                                dist = f1.y * 2 /*- (f2.y - f1.y)*/;
                            } else {
                                dist = f1.y /*- (f2.y - f1.y)*/;
                            }
                        }
                        // 移动字幕
                        mCurCaption.translateCaption(new PointF(0, -dist));
                    }
                   /*

                    addCaption("我是一条字幕");
                    NvsTimelineCaption captionNow = mTimeline.getFirstCaption();
                    List<PointF> list = captionNow.getBoundingRectangleVertices();    // 获取字幕的原始包围矩形框变换后的顶点位置
                    PointF point1 = list.get(0);
                    PointF point2 = list.get(1);
                    PointF f1 = mLiveWindow.mapCanonicalToView(point1);
                    PointF f2 = mLiveWindow.mapCanonicalToView(point2);
                    if (m_fromCap) {
                        dist = f1.y * 2 *//*- (f2.y - f1.y)*//*;
                    } else {
                        dist = f1.y *//*- (f2.y - f1.y)*//*;
                    }
                    // 移动字幕
                    mCurCaption.translateCaption(new PointF(0, -dist));*/
                    compileVideo();
                    break;
            }
        }
    };

    private ArrayList<String> arrayFilePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mStreamingContext = NvsStreamingContext.init(this, null);
        setContentView(R.layout.activity_edit);
        getDataFromIntent();
        muxerAudio();
        initUIView();
        initTimeline();

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLiveWindow.getLayoutParams();
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
        mLiveWindow.setLayoutParams(params);
        mLiveWindow.setFillMode(NvsLiveWindow.FILLMODE_PRESERVEASPECTFIT);

        multiThumbnailSequenceView = mTimelineEditor.getMultiThumbnailSequenceView();
        installAssetPackage();
        buttonListener();
        scrollViewListener();
        playSeekBarListener();
        //
        getStorageFilePath();
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        arrayFilePath = getIntent().getStringArrayListExtra("videoFilePathArray");
        m_ratio = intent.getIntExtra("ratio", 0);
        m_QR = intent.getIntExtra("QR", 0);
        m_fromCap = intent.getBooleanExtra("fromCap", true);
    }

    private void initUIView() {
        mLiveWindowBg = (RelativeLayout) findViewById(R.id.live_window_bg_layout);
        mLiveWindow = (LiveWindow) findViewById(R.id.live_window);
//        mAddVideo = (Button) findViewById(R.id.add_video_btn);
        mPlayBtn = (ImageButton) findViewById(R.id.play_video);
        mPlayBtnSeekBar = (SeekBar) findViewById(R.id.play_seekBar);
        mCurPlayTime = (TextView) findViewById(R.id.cur_play_time);
        mAllPlayTime = (TextView) findViewById(R.id.all_play_time);

        tvCompileVideo = (TextView) findViewById(R.id.tvCompileVideo);
        tvCompileVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                getSubtitleByServer(new File(SDCARD_PATH + "/output_audio.m4a"));

                if (isAddCaptionSuccess) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    arrayList.add(m_compilePath);
                    FxManageActivity.actionStart(EditActivity.this, themePackagePath, fxPackagePath, arrayList, m_ratio, m_QR, m_fromCap);
                } else {
//                    intent.putStringArrayListExtra("videoFilePathArray", arrayFilePath);
                    Toast.makeText(EditActivity.this, "字幕正在加载中", Toast.LENGTH_LONG).show();
                }
            }
        });

       /* findViewById(R.id.tvMoveSub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              *//*  NvsTimelineCaption captionNow = mTimeline.getFirstCaption();
                List<PointF> list = captionNow.getBoundingRectangleVertices();    // 获取字幕的原始包围矩形框变换后的顶点位置
                PointF point1 = list.get(0);
                PointF point2 = list.get(1);
                PointF f1 = mLiveWindow.mapCanonicalToView(point1);
                PointF f2 = mLiveWindow.mapCanonicalToView(point2);
                float dist = f2.y * 2 - (f2.y - f1.y) / 2;*//*
                // 移动字幕
                mCurCaption.translateCaption(new PointF(0, -500));
                updateCaptionCoordinate(mCurCaption);
            }
        });*/

        mAddAssetLayout = (RelativeLayout) findViewById(R.id.add_asset_layout);
        mEditLayout = (LinearLayout) findViewById(R.id.edit_caption_or_sticker_layout);
        mAddStickerBtn = (Button) findViewById(R.id.add_sticker_btn);
        mAddCaptionBtn = (Button) findViewById(R.id.add_caption_btn);
        mEditCaptionBtn = (Button) findViewById(R.id.edit_caption_btn);
        mEditStickerBtn = (Button) findViewById(R.id.edit_sticker_btn);
        mEditCaptionBtnBtnImg = (ImageView) findViewById(R.id.edit_caption_btn_bgImg);
        mEditStickerBtnBtnImg = (ImageView) findViewById(R.id.edit_sticker_btn_bgImg);
        mEditCaptionBtnTab = (LinearLayout) findViewById(R.id.edit_caption_tab);
        mEditStickerBtnCtl = (LinearLayout) findViewById(R.id.edit_sticker_ctl);
        mEditTitle = (TextView) findViewById(R.id.edit_title);
        mOperatorTip = (TextView) findViewById(R.id.operation_tip);
        mEditCaptionBtn.setEnabled(false);
        mEditStickerBtn.setEnabled(false);
        mPlayBtn.setEnabled(false);
        mPlayBtnSeekBar.setEnabled(false);
        mEditCompleteBtn = (ImageButton) findViewById(R.id.edit_complete);
        mEditCaptionTab = (EditCaptionTab) findViewById(R.id.edit_caption_tab);
        mEditCaptionTab.setEditCaptionListener(this);
        mEditStickerCtl = (EditStickerCtl) findViewById(R.id.edit_sticker_ctl);
        mEditStickerCtl.setEditStickerListener(this);

        mSequenceView = (LinearLayout) findViewById(R.id.sequence_view);
        mTimelineEditor = (NvsTimelineEditor) findViewById(R.id.timelineEditor);

        mDrawRect = (DrawRect) findViewById(R.id.draw_rect_view);
        mDrawRect.SetOnTouchListener(this);
        mDrawRect.SetOnAlignClickListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("正在获取字幕，请稍等...");
        progressDialog.setCancelable(true);//设置进度条是否可以按退回键取消 ;

        themePackagePath = "assets:/ED51E571-2650-4754-A45D-AA1CFEA14A81.theme";
        fxPackagePath = "assets:/7FFCF99A-5336-4464-BACD-9D32D5D2DC5E.videofx";
    }


    private void showEditCaptionView() {
        mEditTitle.setText("编辑字幕");
        mOperatorTip.setText("拖动两侧调整字幕的位置");
        mAddAssetLayout.setVisibility(View.GONE);
        mEditLayout.setVisibility(View.VISIBLE);
        mEditCaptionBtnTab.setVisibility(View.VISIBLE);
        mEditCaptionBtnMode = true;
    }

    private void hideEditCaptionView() {
        mPlayBtn.setEnabled(true);
        mEditCaptionBtnTab.setVisibility(View.GONE);
        mEditLayout.setVisibility(View.INVISIBLE);
        mAddAssetLayout.setVisibility(View.VISIBLE);
    }

    private void showEditStickerView() {
        mEditTitle.setText("编辑贴纸");
        mOperatorTip.setText("拖动两侧调整贴纸的位置");
        mAddAssetLayout.setVisibility(View.GONE);
        mEditLayout.setVisibility(View.VISIBLE);
        mEditStickerBtnCtl.setVisibility(View.VISIBLE);
        mEditStickerBtnMode = true;
    }

    private void hideEditStickerView() {
        mPlayBtn.setEnabled(true);
        mEditStickerBtnCtl.setVisibility(View.GONE);
        mEditLayout.setVisibility(View.INVISIBLE);
        mAddAssetLayout.setVisibility(View.VISIBLE);
    }


    private void initTimeline() {
        if (null == mStreamingContext) {
            Log.e(TAG, "mStreamingContext is null!");
            return;
        }
        setRatio();
        NvsRational videoFps = new NvsRational(25, 1);

        NvsAudioResolution audioEditRes = new NvsAudioResolution();
        audioEditRes.sampleRate = 44100;
        audioEditRes.channelCount = 2;

        mTimeline = mStreamingContext.createTimeline(m_videoEditRes, videoFps, audioEditRes);
        if (null == mTimeline) {
            Log.e(TAG, "mTimeline is null!");
            return;
        }

        mStreamingContext.connectTimelineWithLiveWindow(mTimeline, mLiveWindow);
        mStreamingContext.setStreamingEngineCallback(this);
        mStreamingContext.setCompileCallback(this);//设置生成回调接口
        mStreamingContext.setPlaybackCallback(this);
        mStreamingContext.setPlaybackCallback2(this);

        mVideoTrack = mTimeline.appendVideoTrack();
        if (null == mVideoTrack) {
            Log.e(TAG, "mVideoTrack is null!");
            return;
        }
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

    private int installAssetPackage() {

        String captionStylePackagePaths[] = {
                "assets:/BFAE0272-D4AB-403E-AAD4-14CFF4BCAD9C.captionstyle", // 闪耀透白
                "assets:/809E8BB4-8E29-4E84-B1F0-FEEA23564B4A.captionstyle", // 行摄之旅
                "assets:/DF34A143-A1AF-475B-A59D-3350B2E406BC.captionstyle", // 火焰喷射
                "assets:/7F3FB6A6-BF56-4A67-ACED-CB502F4F7DBF.captionstyle"  // 逐字掉落
        };

        int error = -1;
        for (int i = 0; i < captionStylePackagePaths.length; i++) {
            error = mStreamingContext.getAssetPackageManager().installAssetPackage(captionStylePackagePaths[i], null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_CAPTIONSTYLE, true, mCaptionStyles[i]);
            if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                    && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
                Log.e(TAG, "Failed to install captionStyle package!");
            }
        }

        String stickerStylePackagePaths[] = {
                "assets:/81BC283A-786B-4C98-AB96-DAD11DEEEA66.animatedsticker",
                "assets:/35A189ED-4337-4DCC-85BD-2D9809419085.animatedsticker",
                "assets:/07C07364-B563-43C5-ABCE-5E643060C804.animatedsticker",
        };

        for (int i = 0; i < stickerStylePackagePaths.length; i++) {
            error = mStreamingContext.getAssetPackageManager().installAssetPackage(stickerStylePackagePaths[i], null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_ANIMATEDSTICKER, true, mStickerStyles[i]);
            if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                    && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
                Log.e(TAG, "Failed to install sticker package!");
            }
        }

        return error;
    }

    /*贴纸文本框*/
    private void addStickerView() {
        mViewMode = 2; //字幕

        NvsTimelineAnimatedSticker stickerNow = mTimeline.getFirstAnimatedSticker();

        updateStickerOriCoordinate(stickerNow);
    }

    private void updateCurPlayTime(int time) {
        mCurPlayTime.setText(formatTimeStrWithUs((int) time));
        mPlayBtnSeekBar.setProgress((int) time);
    }


    private void updateCaptionView() {
        updateCaptionCoordinate(mCurCaption);  //更新字幕的坐标
    }

    /*字幕文本框*/
    private void addCaptionView() {
        mViewMode = 1; //字幕

        NvsTimelineCaption captionNow = mTimeline.getFirstCaption();

        updateCaptionCoordinate(captionNow);

    }


    /*将int颜色值转换为ARGB*/
    private NvsColor convertHexToRGB(int hexColocr) {
        NvsColor color = new NvsColor(0, 0, 0, 0);
        color.a = (float) ((hexColocr & 0xff000000) >>> 24) / 0xFF;
        color.r = (float) ((hexColocr & 0x00ff0000) >> 16) / 0xFF;
        color.g = (float) ((hexColocr & 0x0000ff00) >> 8) / 0xFF;
        color.b = (float) ((hexColocr) & 0x000000ff) / 0xFF;
        return color;
    }

    /* 将NvsColor的ARGB颜色值转换为整数*/
    private int convertRGBToHex(NvsColor convertColor) {
        return Color.argb((int) (convertColor.a * 0xff), (int) (convertColor.r * 0xff), (int) (convertColor.g * 0xff), (int) (convertColor.b * 0xff));
    }

    /*播放进度条监听*/
    private void playSeekBarListener() {
        mPlayBtnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    isPlayOrSeekBar = true;
                    seekTimeline(i, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    if (multiThumbnailSequenceView != null) {
                        multiThumbnailSequenceView.smoothScrollTo(Math.round((float) (i / (float) mTimeline.getDuration() * mTimelineEditor.getSequenceWidth())), 0);
                    }
                    updateCurPlayTime(i);

                    changeCaptionViewVisible();
                    changeStickerViewVisible();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private void selectCaptionTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).caption == mCurCaption) {
                mTimelineEditor.selectTimeSpan(mTimeSpanInfoList.get(i).timeSpan);
                break;
            }
        }
    }

    private void selectStickerTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).sticker == mCurSticker) {
                mTimelineEditor.selectTimeSpan(mTimeSpanInfoList.get(i).timeSpan);
                break;
            }
        }
    }


    private void scrollViewListener() {
        /* 对字幕的ThumbnailSequenceView的滑动监听 */
        mTimelineEditor.setOnScrollListener(new NvsTimelineEditor.OnScrollChangeListener() {
            @Override
            public void onScrollX(long timeStamp) {
                if (isPlayOrSeekBar)
                    return;


                if (mViewMode == VIEW_MODE_CAPTION) {
                    NvsTimelineCaption caption = null;

                    boolean needSelected = true;
                    List<NvsTimelineCaption> captionList = mTimeline.getCaptionsByTimelinePosition(timeStamp);
                    for (int i = 0; i < captionList.size(); i++) {
                        if (mCurCaption == captionList.get(i)) {
                            needSelected = false;
                            break;
                        }
                    }

                    if ((mCurCaption.getInPoint() <= timeStamp && mCurCaption.getOutPoint() >= timeStamp)) {
                        needSelected = false;
                    }

                    if (captionList.size() != 0) {
                        if (needSelected) {
                            mCurCaption = captionList.get(0);
                        }
                        updateCaptionCoordinate(mCurCaption);
                        selectCaptionTimeSpan();

                    }

                } else if (mViewMode == VIEW_MODE_STICEER) {

                    NvsTimelineAnimatedSticker sticker = null;

                    List<NvsTimelineAnimatedSticker> stickerList = mTimeline.getAnimatedStickersByTimelinePosition(timeStamp);

                    boolean needSelected = true;
                    for (int i = 0; i < stickerList.size(); i++) {
                        if (mCurSticker == stickerList.get(i)) {
                            needSelected = false;
                            break;
                        }
                    }

                    if ((mCurSticker.getInPoint() <= timeStamp && mCurSticker.getOutPoint() >= timeStamp)) {
                        needSelected = false;
                    }

                    if (stickerList.size() != 0) {
                        if (needSelected) {
                            mCurSticker = stickerList.get(0);
                        }
                        updateStickerOriCoordinate(mCurSticker);
                        selectStickerTimeSpan();
                    }

                }

                seekTimeline(timeStamp, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);

                if (mHasAddedCaption) {
                    mEditCaptionBtn.setEnabled(true);
                }
                if (mHasAddedSticker) {
                    mEditStickerBtn.setEnabled(true);
                }

                mPlayBtn.setBackgroundResource(R.mipmap.play);
                if (mEditCaptionBtnMode) {
                    changeCaptionViewVisible();
                }
                if (mEditStickerBtnMode) {
                    changeStickerViewVisible();
                }
            }
        });
        multiThumbnailSequenceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isPlayOrSeekBar = false;
                return false;
            }
        });
    }

    private void changeCaptionViewVisible() {
        if (mEditCaptionBtnMode) {
            if (!isCaptionInandOut()) {
                mDrawRect.setVisibility(View.INVISIBLE);
            } else {
                mDrawRect.setVisibility(View.VISIBLE);
            }
        }
    }

    private void changeStickerViewVisible() {
        if (mEditStickerBtnMode) {
            if (!isStickerInandOut()) {
                mDrawRect.setVisibility(View.INVISIBLE);
            } else {
                mDrawRect.setVisibility(View.VISIBLE);
            }
        }
    }

    private void buttonListener() {
        /* 添加视频 */
      /*  mAddVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStreamingContext != null) {
                    mStreamingContext.stop();
                }

                MediaOptions.Builder builder = new MediaOptions.Builder();
                MediaOptions options = builder.selectVideo().canSelectMultiVideo(true).build();
                MediaPickerActivity.open(MainActivity.this, REQUEST_MEDIA, options);

            }
        });*/

        /* 播放 */
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = mStreamingContext.getStreamingEngineState();
                if (NvsStreamingContext.STREAMING_ENGINE_STATE_SEEKING == state || NvsStreamingContext.STREAMING_ENGINE_STATE_STOPPED == state) {
                    mPlayBtn.setBackgroundResource(R.mipmap.pause);
                    mEditCaptionBtn.setEnabled(false);
                    mEditStickerBtn.setEnabled(false);
                    mStreamingContext.playbackTimeline(mTimeline, mStreamingContext.getTimelineCurrentPosition(mTimeline), -1, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
                } else if (NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK == state) {
                    mPlayBtn.setBackgroundResource(R.mipmap.play);
                    if (mHasAddedCaption) {
                        mEditCaptionBtn.setEnabled(true);
                    }
                    if (mHasAddedSticker) {
                        mEditStickerBtn.setEnabled(true);
                    }

                    mStreamingContext.stop();
                }
            }
        });
        /* 添加字幕 */

        mAddCaptionBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mEditStickerBtnMode) {
                    quitEditStickerMode();
                }

                inputCaption();
            }
        });

        /* 编辑字幕 */
        mEditCaptionBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mEditCaptionBtnMode) {

                    if (mEditStickerBtnMode) {
                        quitEditStickerMode();
                    }

                    enterEditCaptionMode();
                } else {
                    quitEditCaptionMode();
                }
            }
        });

        /* 添加贴纸 */
        mAddStickerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mEditCaptionBtnMode) {
                    quitEditCaptionMode();
                }

                addSticker();

            }
        });

        /* 编辑贴纸 */
        mEditStickerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mEditStickerBtnMode) {
                    if (mEditCaptionBtnMode) {
                        quitEditCaptionMode();
                    }
                    enterEditStickerMode();
                } else {
                    quitEditStickerMode();
                }
            }
        });

        /*完成编辑*/
        mEditCompleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEditCaptionBtnMode) {
                    quitEditCaptionMode();
                }

                if (mEditStickerBtnMode) {
                    quitEditStickerMode();
                }
            }
        });
    }


    private boolean isCaptionInandOut() {
        if (mCurCaption == null) {
            return false;
        }

        if (mStreamingContext.getTimelineCurrentPosition(mTimeline) >= mCurCaption.getInPoint()
                && mStreamingContext.getTimelineCurrentPosition(mTimeline) <= mCurCaption.getOutPoint()) {
            return true;
        }
        return false;
    }

    private boolean isStickerInandOut() {
        if (mCurSticker == null) {
            return false;
        }
        if (mStreamingContext.getTimelineCurrentPosition(mTimeline) >= mCurSticker.getInPoint()
                && mStreamingContext.getTimelineCurrentPosition(mTimeline) <= mCurSticker.getOutPoint()) {
            return true;
        }
        return false;
    }


    private void deleteCurStickerTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).sticker == mCurSticker) {
                mTimelineEditor.deleteSelectedTimeSpan(mTimeSpanInfoList.get(i).timeSpan);
                mTimeSpanInfoList.remove(i);
                break;
            }
        }
    }

    private void deleteCurCaptionTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).caption == mCurCaption) {
                mTimelineEditor.deleteSelectedTimeSpan(mTimeSpanInfoList.get(i).timeSpan);
                mTimeSpanInfoList.remove(i);
                break;
            }
        }
    }

    private void deleteSticker() {

        deleteCurStickerTimeSpan();
        mCurSticker = mTimeline.removeAnimatedSticker(mCurSticker);
        if (mCurSticker == null) {
            if (mTimeSpanInfoList.size() > 0) {
                mCurSticker = mTimeSpanInfoList.get(0).sticker;
            }
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateStickerOriCoordinate(mCurSticker);
        changeStickerViewVisible();

        if (mTimeSpanInfoList.size() == 0) {
            quitEditStickerMode();
        }
    }

    private void deleteCaption() {

        deleteCurCaptionTimeSpan();
        mCurCaption = mTimeline.removeCaption(mCurCaption);
        if (mCurCaption == null) {
            if (mTimeSpanInfoList.size() > 0) {
                mCurCaption = mTimeSpanInfoList.get(0).caption;
            }
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionCoordinate(mCurCaption);
        changeCaptionViewVisible();


        if (mTimeSpanInfoList.size() == 0) {
            quitEditCaptionMode();
        }
    }

    private void seekMultiThumbnailSequenceView() {
        if (multiThumbnailSequenceView != null) {
            long curPos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
            long duration = mTimeline.getDuration();
            multiThumbnailSequenceView.scrollTo(Math.round(((float) curPos) / (float) duration * mTimelineEditor.getSequenceWidth()), 0);
        }
    }


    /*进入贴纸编辑模式*/
    private void enterEditStickerMode() {
//        mAddVideo.setEnabled(false);
        mEditStickerBtnMode = true;
        addStickerView();
        changeStickerViewVisible();
        showEditStickerView();
        readdStickerTimeSpan();
    }

    /*退出贴纸编辑模式*/
    private void quitEditStickerMode() {
//        mAddVideo.setEnabled(true);
        hideEditStickerView();
        mPlayBtn.setEnabled(true);
        mEditStickerBtnMode = false;

        if (mTimeSpanInfoList.size() == 0) {
            mEditStickerBtn.setEnabled(false);
            mEditStickerBtnBtnImg.setAlpha(0.3f);
            mHasAddedSticker = false;
        }

        mEditStickerCtl.resetState();
        mDrawRect.SetDrawRect(null, VIEW_MODE_CAPTION);
    }

    private void readdStickerTimeSpan() {
        //重新添加NvsTimelineTimeSpan
        mTimelineEditor.deleteAllTimeSpan();
        mTimeSpanInfoList.clear();
        mCurSticker = mTimeline.getFirstAnimatedSticker();
        while (mCurSticker != null) {
            final NvsTimelineTimeSpan timeSpan = mTimelineEditor.addTimeSpan(mCurSticker.getInPoint(), mCurSticker.getOutPoint());
            TimeSpanInfo timeSpanInfo = new TimeSpanInfo(null, mCurSticker, timeSpan);
            mTimeSpanInfoList.add(timeSpanInfo);
            timeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimInChangeListener() {
                @Override
                public void onChange(long timeStamp, boolean isDragEnded) {

                    NvsTimelineAnimatedSticker sticker = null;

                    for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
                        if (mTimeSpanInfoList.get(i).timeSpan == timeSpan) {
                            sticker = mTimeSpanInfoList.get(i).sticker;
                            break;
                        }
                    }


                    if (sticker != null && isDragEnded) {
                        sticker.changeInPoint(timeStamp);
                        seekMultiThumbnailSequenceView();
                    }
                    changeStickerViewVisible();
                    seekTimeline(timeStamp, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                }
            });

            timeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimOutChangeListener() {
                @Override
                public void onChange(long timeStamp, boolean isDragEnded) {

                    NvsTimelineAnimatedSticker sticker = null;

                    for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
                        if (mTimeSpanInfoList.get(i).timeSpan == timeSpan) {
                            sticker = mTimeSpanInfoList.get(i).sticker;
                            break;
                        }
                    }

                    if (sticker != null && isDragEnded) {
                        sticker.changeOutPoint(timeStamp);
                        seekMultiThumbnailSequenceView();
                    }
                    changeStickerViewVisible();
                    seekTimeline(timeStamp - 1, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                }
            });

            mCurSticker = mTimeline.getNextAnimatedSticker(mCurSticker);
        }
        mCurSticker = mTimeline.getLastAnimatedSticker();

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        selectStickerTimeSpan();
    }

    private void readdCaptionTimeSpan() {
        //重新添加NvsTimelineTimeSpan
        mTimelineEditor.deleteAllTimeSpan();
        mTimeSpanInfoList.clear();
        mCurCaption = mTimeline.getFirstCaption();
        while (mCurCaption != null) {
            Log.d("abc: ", "in: " + mCurCaption.getInPoint());
            Log.d("abc: ", "duration: " + mTimeline.getDuration());
            Log.d("abc: ", "out: " + mCurCaption.getOutPoint());
            Log.d("abc: ", "cur: " + mStreamingContext.getTimelineCurrentPosition(mTimeline));
            final NvsTimelineTimeSpan timeSpan = mTimelineEditor.addTimeSpan(mCurCaption.getInPoint(), mCurCaption.getOutPoint());

            TimeSpanInfo timeSpanInfo = new TimeSpanInfo(mCurCaption, null, timeSpan);
            mTimeSpanInfoList.add(timeSpanInfo);

            timeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimInChangeListener() {
                @Override
                public void onChange(long timeStamp, boolean isDragEnded) {

                    NvsTimelineCaption caption = null;

                    for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
                        if (mTimeSpanInfoList.get(i).timeSpan == timeSpan) {
                            caption = mTimeSpanInfoList.get(i).caption;
                            break;
                        }
                    }

                    if (caption != null && isDragEnded) {
                        caption.changeInPoint(timeStamp);
                        seekMultiThumbnailSequenceView();
                    }
                    changeCaptionViewVisible();
                    seekTimeline(timeStamp, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                }
            });
            timeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimOutChangeListener() {
                @Override
                public void onChange(long timeStamp, boolean isDragEnded) {

                    NvsTimelineCaption caption = null;

                    for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
                        if (mTimeSpanInfoList.get(i).timeSpan == timeSpan) {
                            caption = mTimeSpanInfoList.get(i).caption;
                            break;
                        }
                    }

                    if (caption != null && isDragEnded) {
                        caption.changeOutPoint(timeStamp);
                        seekMultiThumbnailSequenceView();
                    }
                    changeCaptionViewVisible();
                    seekTimeline(timeStamp - 1, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                }
            });


            mCurCaption = mTimeline.getNextCaption(mCurCaption);
        }
        mCurCaption = mTimeline.getLastCaption();
        updateCaptionCoordinate(mCurCaption);
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        selectCaptionTimeSpan();
    }

    /*进入字幕编辑模式*/
    private void enterEditCaptionMode() {

//        mAddVideo.setEnabled(false);
        showEditCaptionView();
        updateCaptionCoordinate(mTimeline.getFirstCaption());
        addCaptionView();
        if (!isCaptionInandOut()) {
            mDrawRect.setVisibility(View.INVISIBLE);
        } else {
            mDrawRect.setVisibility(View.VISIBLE);
        }

        readdCaptionTimeSpan();

        // 获取视频中字幕的属性并初始化界面
        NvsTimelineCaption captionNow = mTimeline.getFirstCaption();
        mEditCaptionTab.SetFontPropertySwitchBtn(captionNow.getBold(), captionNow.getItalic(), captionNow.getDrawShadow());
        mEditCaptionTab.InitFontFaceSeekBar((int) captionNow.getFontSize());
    }

    /*退出字幕编辑模式*/
    private void quitEditCaptionMode() {
        hideEditCaptionView();
        mEditCaptionBtnMode = false;
//        mAddVideo.setEnabled(true);

        if (mTimeSpanInfoList.size() == 0) {
            mEditCaptionBtn.setEnabled(false);
            mEditCaptionBtnBtnImg.setAlpha(0.3f);
            mHasAddedCaption = false;
        }

        mEditCaptionTab.resetState();
        mDrawRect.SetDrawRect(null, VIEW_MODE_CAPTION);
    }


    private void addCaption(String caption) {
        //添加字幕
        long inPoint = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        long captionDuration = 5 * NS_TIME_BASE;
        long outPoint = inPoint + captionDuration;
        long duration = mTimeline.getDuration();
        if (outPoint > duration) {
            captionDuration = duration - inPoint;
        }

        mCurCaption = mTimeline.addCaption(caption, inPoint, captionDuration, null);
        if (m_fromCap) {
            mCurCaption.setFontSize(70);
        } else {
            mCurCaption.setFontSize(50);
        }

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);

        //更新界面
        if (!mHasAddedCaption) {
            mHasAddedCaption = true;
        }
        mEditCaptionBtn.setEnabled(true);
        mEditCaptionBtnBtnImg.setAlpha(1f);
    }

    private void addCaption(SubtitleBean caption) {
        //添加字幕
        long inPoint = Long.valueOf(caption.getBg()) * 1000;
        long captionDuration = Long.valueOf(caption.getEd()) * 1000 - Long.valueOf(caption.getBg()) * 1000;
        long outPoint = inPoint + captionDuration;
        long duration = mTimeline.getDuration();
        if (outPoint > duration) {
            captionDuration = duration - inPoint;
        }

        mCurCaption = mTimeline.addCaption(caption.getOnebest(), inPoint, captionDuration, null);
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);

        //更新界面
        if (!mHasAddedCaption) {
            mHasAddedCaption = true;
        }
        mEditCaptionBtn.setEnabled(true);
        mEditCaptionBtnBtnImg.setAlpha(1f);
    }


    private void addSticker() {

        long inPoint = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        long stickerDuration = 5 * NS_TIME_BASE;
        long outPoint = inPoint + stickerDuration;
        long duration = mTimeline.getDuration();
        if (outPoint > duration) {
            stickerDuration = duration - inPoint;
        }

        mCurSticker = mTimeline.addAnimatedSticker(inPoint, stickerDuration, mStickerStyles[0].toString());
        if (mCurSticker != null) {
            mEditStickerCtl.setCurStickerSize((int) (mCurSticker.getScale() * 100));
        } else {
            return;
        }

        mEditStickerBtn.setEnabled(true);
        mEditStickerBtnBtnImg.setAlpha(1f);
        mHasAddedSticker = true;

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }


    /*输入字幕*/
    private void inputCaption() {

        new CommonDialog(EditActivity.this, R.style.dialog, new CommonDialog.OnCloseListener() {
            @Override
            public void onClick(Dialog dialog, boolean ok) {

                if (ok) {
                    CommonDialog d = (CommonDialog) dialog;
                    String userInputText = d.getUserInputText();
                    addCaption(userInputText);
                } else {
                    dialog.dismiss();
                }
            }
        }).show();

    }

    /*格式化时间(us)*/
    private String formatTimeStrWithUs(int us) {
        int second = us / 1000000;
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
    protected void onResume() {
        super.onResume();
        mLiveWindowBg.setVisibility(View.INVISIBLE);
        mPlayBtnSeekBar.setEnabled(true);
        mAddAssetLayout.setVisibility(View.VISIBLE);
        showResult(arrayFilePath);
    }

    private void showResult(ArrayList<String> pathList) {
        if (null == mVideoTrack) {
            Log.e(TAG, "mVideoTrack is null");
            return;
        }

        mVideoTrack.removeAllClips();

        for (int i = 0; i < pathList.size(); i++) {
            NvsVideoClip clip = mVideoTrack.appendClip(pathList.get(i));  //添加视频片段
            if (clip == null) {
                Toast.makeText(this, "Failed to Append Clip" + pathList.get(i), Toast.LENGTH_LONG).show();
                return;
            }

            mClipList.add(clip);
        }

        if (mTimeline == null) {
            Log.e(TAG, "mTimeline is null");
            return;
        }

        if (mHasAddVideo) {
            resetCaptionAndStickerView();
        }
        initSequenceView(pathList);
        initAction();
    }

    private void initAction() {
        mSequenceView.setVisibility(View.VISIBLE);
        seekTimeline(0, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        multiThumbnailSequenceView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
        mPlayBtn.setEnabled(true);
        mAddCaptionBtn.setEnabled(true);
        mAddStickerBtn.setEnabled(true);
    }

    private void resetCaptionAndStickerView() {
        mEditCaptionBtn.setEnabled(false);
        mEditCaptionBtnBtnImg.setAlpha(0.3f);
        mHasAddedCaption = false;
        mEditCaptionBtnMode = false;

        mEditStickerBtn.setEnabled(false);
        mEditStickerBtnBtnImg.setAlpha(0.3f);
        mHasAddedSticker = false;
        mEditStickerBtnMode = false;

    }

    private void initSequenceView(List<String> paths) {
        ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> infoDescArray = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            NvsClip curClip = mVideoTrack.getClipByIndex(i);
            NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc infoDesc = new NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc();
            infoDesc.mediaFilePath = paths.get(i);
            infoDesc.trimIn = curClip.getTrimIn();
            infoDesc.trimOut = curClip.getTrimOut();
            infoDesc.inPoint = curClip.getInPoint();
            infoDesc.outPoint = curClip.getOutPoint();
            infoDesc.stillImageHint = false;
            infoDescArray.add(infoDesc);
        }
        mTimelineEditor.initTimelineEditor(infoDescArray, mTimeline.getDuration());

        mHasAddVideo = true;
        int total_duration = (int) mTimeline.getDuration();
        mCurPlayTime.setText(formatTimeStrWithUs(0));
        mAllPlayTime.setText(formatTimeStrWithUs(total_duration));
        mPlayBtnSeekBar.setMax(total_duration);
    }

    private void seekTimeline(long timestamp, int seekShowMode) {
        /* seekTimeline
         * param1: 当前时间线
         * param2: 时间戳 取值范围为  [0, timeLine.getDuration()) (左闭右开区间)
         * param3: 图像预览模式
         * param4: 引擎定位的特殊标志
         * */
        mStreamingContext.seekTimeline(mTimeline, timestamp, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, seekShowMode);
    }

    /*
     *   获取动画贴纸在视图上的原始坐标
     *
     * */
    private void updateStickerOriCoordinate(NvsTimelineAnimatedSticker sticker) {
        if (sticker != null) {
            List<PointF> list = sticker.getBoundingRectangleVertices();    // 获取动画贴纸的原始包围矩形框变换后的顶点位置
            List<PointF> newList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PointF pointF = mLiveWindow.mapCanonicalToView(list.get(i));
                newList.add(pointF);
            }
            mDrawRect.SetDrawRect(newList, VIEW_MODE_STICEER);
        }
    }


    /*
     *   更新字幕在视图上的坐标
     *
     * */
    private void updateCaptionCoordinate(NvsTimelineCaption caption) {
        if (caption != null) {
            List<PointF> list = caption.getBoundingRectangleVertices();    // 获取字幕的原始包围矩形框变换后的顶点位置

            List<PointF> newList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PointF pointF = mLiveWindow.mapCanonicalToView(list.get(i));
                newList.add(pointF);
            }
            mDrawRect.SetDrawRect(newList, VIEW_MODE_CAPTION);
        }
    }


    private void selectCaption(PointF targetPoint) {


        List<NvsTimelineCaption> captionList = mTimeline.getCaptionsByTimelinePosition(mStreamingContext.getTimelineCurrentPosition(mTimeline));

        if (captionList.size() <= 1) {
            return;
        }


        for (int j = 0; j < captionList.size(); j++) {

            NvsTimelineCaption caption = captionList.get(j);
            List<PointF> list = caption.getBoundingRectangleVertices();
            List<PointF> newList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PointF pointF = mLiveWindow.mapCanonicalToView(list.get(i));
                newList.add(pointF);
            }

            boolean isSelected = false;

            // 判断手指是否在字幕框内
            RectF r = new RectF();
            Path path = new Path();
            path.moveTo(newList.get(0).x, newList.get(0).y);
            path.lineTo(newList.get(1).x, newList.get(1).y);
            path.lineTo(newList.get(2).x, newList.get(2).y);
            path.lineTo(newList.get(3).x, newList.get(3).y);
            path.close();
            path.computeBounds(r, true);
            Region region = new Region();
            region.setPath(path, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
            isSelected = region.contains((int) targetPoint.x, (int) targetPoint.y);

            if (isSelected) {
                mDrawRect.SetDrawRect(newList, VIEW_MODE_CAPTION);
                mCurCaption = caption;
                selectCaptionTimeSpan();
                break;
            }

        }
    }


    private void selectSticker(PointF targetPoint) {

        List<NvsTimelineAnimatedSticker> stickerList = mTimeline.getAnimatedStickersByTimelinePosition(mStreamingContext.getTimelineCurrentPosition(mTimeline));

        if (stickerList.size() <= 1) {
            return;
        }

        for (int j = 0; j < stickerList.size(); j++) {
            NvsTimelineAnimatedSticker sticker = stickerList.get(j);
            List<PointF> list = sticker.getBoundingRectangleVertices();
            List<PointF> newList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                PointF pointF = mLiveWindow.mapCanonicalToView(list.get(i));
                newList.add(pointF);
            }

            boolean isSelected = false;

            // 判断手指是否在字幕框内
            RectF r = new RectF();
            Path path = new Path();
            path.moveTo(newList.get(0).x, newList.get(0).y);
            path.lineTo(newList.get(1).x, newList.get(1).y);
            path.lineTo(newList.get(2).x, newList.get(2).y);
            path.lineTo(newList.get(3).x, newList.get(3).y);
            path.close();
            path.computeBounds(r, true);
            Region region = new Region();
            region.setPath(path, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
            isSelected = region.contains((int) targetPoint.x, (int) targetPoint.y);

            if (isSelected) {
                mDrawRect.SetDrawRect(newList, VIEW_MODE_STICEER);
                mCurSticker = sticker;
                selectStickerTimeSpan();
                break;
            }

        }
    }


    @Override
    protected void onDestroy() {
        mStreamingContext = null;
//        NvsStreamingContext.close();
        super.onDestroy();
    }

    @Override
    public void onStreamingEngineStateChanged(int state) {
        if (state == mStreamingContext.STREAMING_ENGINE_STATE_STOPPED || state == mStreamingContext.STREAMING_ENGINE_STATE_SEEKING) {
            mPlayBtn.setBackgroundResource(R.mipmap.play);
            if (mHasAddedCaption) {
                mEditCaptionBtn.setEnabled(true);
                mEditCaptionBtnBtnImg.setAlpha(1f);
            }

            if (mHasAddedSticker) {
                mEditStickerBtn.setEnabled(true);
                mEditStickerBtnBtnImg.setAlpha(1f);
            }

            changeCaptionViewVisible();
            changeStickerViewVisible();

        } else if (state == mStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
            mPlayBtn.setBackgroundResource(R.mipmap.pause);
            mEditCaptionBtn.setEnabled(false);
            mEditCaptionBtnBtnImg.setAlpha(0.3f);
            mEditStickerBtn.setEnabled(false);
            mEditStickerBtnBtnImg.setAlpha(0.3f);
            if (mEditCaptionBtnMode) {
                mDrawRect.setVisibility(View.INVISIBLE);
            }
            if (mEditStickerBtnMode) {
                mDrawRect.setVisibility(View.INVISIBLE);
            }

        }
    }

    @Override
    public void onFirstVideoFramePresented(NvsTimeline timeline) {

    }

    @Override
    public void onPlaybackPreloadingCompletion(NvsTimeline var1) {

    }

    @Override
    public void onPlaybackStopped(NvsTimeline var1) {
        //seekTimeline(0, mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    @Override
    public void onPlaybackEOF(NvsTimeline var1) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                m_handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = UPDATE_PLAY_SEEKBAR;
                        m_handler.sendMessage(message);
                    }
                });
            }
        }).start();

    }

    @Override
    public void onPlaybackTimelinePosition(NvsTimeline timeline, long position) {
        if (multiThumbnailSequenceView != null) {
            isPlayOrSeekBar = true;
            multiThumbnailSequenceView.smoothScrollTo(Math.round((float) (position / (float) mTimeline.getDuration() * mTimelineEditor.getSequenceWidth())), 0);
        }
        updateCurPlayTime((int) position);
    }

    @Override
    public void setFontBlod(boolean isBlod) {
        if (isBlod) {
            if (!mCurCaption.getBold()) {
                mCurCaption.setBold(true);   // 设置字幕粗体
            }
        } else {
            if (mCurCaption.getBold()) {
                mCurCaption.setBold(false);
            }
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontItalic(boolean isItalic) {
        if (isItalic) {
            if (!mCurCaption.getItalic()) {
                mCurCaption.setItalic(true); // 设置斜体
            }
        } else {
            if (mCurCaption.getItalic()) {
                mCurCaption.setItalic(false);
            }
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontShadow(boolean isShadow) {
        if (isShadow) {
            if (!mCurCaption.getDrawShadow()) {
                /* 设置字幕阴影 */
                mCurCaption.setDrawShadow(true);
                PointF point = new PointF(3, 3);
                NvsColor shadowColor = new NvsColor(0, 1, 0, 1);
                mCurCaption.setShadowOffset(point);  //字幕阴影偏移量
                mCurCaption.setShadowColor(shadowColor); // 字幕阴影颜色
            }
        } else {
            if (mCurCaption.getDrawShadow()) {
                mCurCaption.setDrawShadow(false);
            }
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontStyle(int position) {
        /* 添加字幕样式 */
        if (position == 0) {
            mCurCaption.applyCaptionStyle("");
        } else {
            mCurCaption.applyCaptionStyle("");
            mCurCaption.applyCaptionStyle(mCaptionStyles[position].toString());
        }

        if (mTimeline.getFirstCaption() != null) {
            updateCaptionCoordinate(mTimeline.getFirstCaption());
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontColor(int color) {
        // 设置字体颜色
        mCurCaption.setTextColor(convertHexToRGB(color));
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    @Override
    public void setFontAlpha(int value) {
        // 设置字体的透明度
        NvsColor curColor = mCurCaption.getTextColor();
        curColor.a = (float) value / 100;
        mCurCaption.setTextColor(curColor);
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    @Override
    public void OnPageSelected(int position) {
        // position == 2  当前页面为设置描边页面
        if (position == 2) {

        }
    }


    @Override
    public void setFontStrokeColor(int color) {

        if (Color.parseColor("#00ffffff") == color) {
            mCurCaption.setDrawOutline(false);
            return;
        }

        if (!mCurCaption.getDrawOutline()) {
            mCurCaption.setDrawOutline(true);  //字幕描边
            mCurCaption.setOutlineWidth(6 * 50 / 100); //字幕描边宽度
            // 初始化界面描边宽度进度条
            mEditCaptionTab.InitFontStrokeWidth(50);
        }

        // 设置描边颜色
        mCurCaption.setOutlineColor(convertHexToRGB(color));
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontStrokeWidth(int width) {
        if (!mCurCaption.getDrawOutline()) {
            return;
        }

        // 设置描边宽度
        mCurCaption.setOutlineWidth(6 * width / 100); //字幕描边宽度
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontStrokeAlpha(int alpha) {
        if (!mCurCaption.getDrawOutline()) {
            return;
        }

        // 设置描边的透明度
        NvsColor curColor = mCurCaption.getOutlineColor();
        curColor.a = (float) alpha / 100;
        mCurCaption.setOutlineColor(curColor);
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontface(int pos) {
        // 设置字体
        if (pos == 1) {
            String fontPath = "assets:/font.ttf";
            mCurCaption.setFontByFilePath(fontPath);
        } else {
            mCurCaption.setFontByFilePath("");
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setFontSize(int size) {
        // 字幕字体大小设置
        int displayProgress = 0;
        if (size < mMinFont) {
            displayProgress = mMinFont;
        } else {
            displayProgress = size;
        }
        mCurCaption.setFontSize(displayProgress);
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateCaptionView();
    }

    @Override
    public void setStickerStyle(int pos) {
        long inPoint = mCurSticker.getInPoint();
        long outPoint = mCurSticker.getOutPoint();

        mTimeline.removeAnimatedSticker(mCurSticker);
        mCurSticker = mTimeline.addAnimatedSticker(inPoint, outPoint - inPoint, mStickerStyles[pos].toString());
        mEditStickerCtl.setCurStickerSize((int) mCurSticker.getScale() * 100);

        if (mCurSticker != null) {
            mCurSticker.changeInPoint(inPoint);
            mCurSticker.changeOutPoint(outPoint);
            changeStickerViewVisible();
        }

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);

        NvsTimelineAnimatedSticker sticker = mCurSticker;
        readdStickerTimeSpan();
        mCurSticker = sticker;
        selectStickerTimeSpan();

        updateStickerOriCoordinate(mCurSticker); //贴纸文本矩形框的原始坐标
    }

    @Override
    public void setStickerSize(int size) {
        float stickerScale = 0f;
        float displayScale = (float) size / 100;
        if (displayScale - mMinScale < mEspinon) {
            stickerScale = mMinScale;
        } else {
            stickerScale = displayScale;
        }

        mCurSticker.setScale(stickerScale);

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);

        if (mViewMode == VIEW_MODE_STICEER && mHasAddedSticker) {
            updateStickerOriCoordinate(mCurSticker);
        }
    }

    @Override
    public void horizontalFlip() {
        if (!mCurSticker.getHorizontalFlip()) {
            mCurSticker.setHorizontalFlip(true);   // 贴纸水平翻转
        } else {
            mCurSticker.setHorizontalFlip(false);
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateStickerOriCoordinate(mCurSticker);
    }

    @Override
    public void verticalFlip() {
        if (!mCurSticker.getVerticalFlip()) {
            mCurSticker.setVerticalFlip(true);   // 贴纸水平翻转
        } else {
            mCurSticker.setVerticalFlip(false);
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        updateStickerOriCoordinate(mCurSticker);
    }

    @Override
    public void onDrag(PointF prePointF, PointF nowPointF) {
        /* 坐标转换
         *
         * SDK接口所使用的坐标均是Canonical坐标系内的坐标，而我们在程序中所用是的
         * 一般是Android View 坐标系里面的坐标，所以在使用接口的时候需要使用SDK所
         * 提供的mapViewToCanonical函数将View坐标转换为Canonical坐标，相反的，
         * 如果想要将Canonical坐标转换为View坐标，则可以使用mapCanonicalToView
         * 函数进行转换。
         * */
        PointF pre = mLiveWindow.mapViewToCanonical(prePointF);
        PointF p = mLiveWindow.mapViewToCanonical(nowPointF);
        PointF timeLinePointF = new PointF(p.x - pre.x, p.y - pre.y);

        if (mViewMode == VIEW_MODE_CAPTION) {
            // 移动字幕
            mCurCaption.translateCaption(timeLinePointF);
            updateCaptionCoordinate(mCurCaption);

        } else if (mViewMode == VIEW_MODE_STICEER) { // 贴纸编辑
            // 移动贴纸
            mCurSticker.translateAnimatedSticker(timeLinePointF);
            updateStickerOriCoordinate(mCurSticker);
        }

        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    @Override
    public void onScaleAndRotate(float scaleFactor, PointF anchor, float angle) {
        /* 坐标转换
         *
         * SDK接口所使用的坐标均是Canonical坐标系内的坐标，而我们在程序中所用是的
         * 一般是Android View 坐标系里面的坐标，所以在使用接口的时候需要使用SDK所
         * 提供的mapViewToCanonical函数将View坐标转换为Canonical坐标，相反的，
         * 如果想要将Canonical坐标转换为View坐标，则可以使用mapCanonicalToView
         * 函数进行转换。
         * */


        // 字幕编辑
        if (mViewMode == VIEW_MODE_CAPTION) {
            // 放缩字幕
            mCurCaption.scaleCaption(scaleFactor, mLiveWindow.mapViewToCanonical(anchor));
            // 旋转字幕
            mCurCaption.rotateCaption(angle);
            updateCaptionCoordinate(mCurCaption);
        } else if (mViewMode == VIEW_MODE_STICEER) { // 贴纸编辑
            // 放缩贴纸
            mCurSticker.scaleAnimatedSticker(scaleFactor, mLiveWindow.mapViewToCanonical(anchor));
            // 旋转贴纸
            mCurSticker.rotateAnimatedSticker(angle, mLiveWindow.mapViewToCanonical(anchor));
            updateStickerOriCoordinate(mCurSticker);
        }
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
    }

    @Override
    public void onDel() {
        if (mViewMode == VIEW_MODE_CAPTION) {
            deleteCaption();
        } else if (mViewMode == VIEW_MODE_STICEER) {
            deleteSticker();
        }

    }


    @Override
    public void onTouchDown(PointF curPoint) {

        if (mViewMode == VIEW_MODE_CAPTION) {
            selectCaption(curPoint);
        } else if (mViewMode == VIEW_MODE_STICEER) {
            selectSticker(curPoint);
        }

    }

    @Override
    public void onAlignClick() {
        if (mViewMode == VIEW_MODE_CAPTION) {
            switch (mCurCaption.getTextAlignment()) {
                case NvsTimelineCaption.TEXT_ALIGNMENT_LEFT:
                    mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_CENTER);  //居中对齐
                    mDrawRect.setalignIndex(1);
                    break;
                case NvsTimelineCaption.TEXT_ALIGNMENT_CENTER:
                    mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_RIGHT);  //居右对齐
                    mDrawRect.setalignIndex(2);
                    break;

                case NvsTimelineCaption.TEXT_ALIGNMENT_RIGHT:
                    mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_LEFT);  //左对齐
                    mDrawRect.setalignIndex(0);
                    break;
            }
            seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), mStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
        }
    }

    public static void actionStart(Context context, ArrayList<String> paths, int ratio, int QR, boolean fromCap) {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putStringArrayListExtra("videoFilePathArray", paths);
        intent.putExtra("ratio", ratio);
        intent.putExtra("QR", QR);
        intent.putExtra("fromCap", fromCap);
        context.startActivity(intent);
    }

    /**
     * 分离出音频
     */
    private void muxerAudio() {
        new Thread() {
            @Override
            public void run() {
                mediaExtractor = new MediaExtractor();
                int audioIndex = -1;
                try {
                    mediaExtractor.setDataSource(arrayFilePath.get(0));
                    int trackCount = mediaExtractor.getTrackCount();
                    for (int i = 0; i < trackCount; i++) {
                        MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                        if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                            audioIndex = i;
                        }
                    }
                    mediaExtractor.selectTrack(audioIndex);
                    MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioIndex);
                    mediaMuxer = new MediaMuxer(SDCARD_PATH + "/output_audio.m4a", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    int writeAudioIndex = mediaMuxer.addTrack(trackFormat);
                    mediaMuxer.start();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                    long stampTime = 0;
                    //获取帧之间的间隔时间
                    {
                        mediaExtractor.readSampleData(byteBuffer, 0);
                        if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                            mediaExtractor.advance();
                        }
                        mediaExtractor.readSampleData(byteBuffer, 0);
                        long secondTime = mediaExtractor.getSampleTime();
                        mediaExtractor.advance();
                        mediaExtractor.readSampleData(byteBuffer, 0);
                        long thirdTime = mediaExtractor.getSampleTime();
                        stampTime = Math.abs(thirdTime - secondTime);
                        Log.e("fuck", stampTime + "");
                    }

                    mediaExtractor.unselectTrack(audioIndex);
                    mediaExtractor.selectTrack(audioIndex);
                    while (true) {
                        int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                        if (readSampleSize < 0) {
                            break;
                        }
                        mediaExtractor.advance();

                        bufferInfo.size = readSampleSize;
                        bufferInfo.flags = mediaExtractor.getSampleFlags();
                        bufferInfo.offset = 0;
                        bufferInfo.presentationTimeUs += stampTime;

                        mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
                    }
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaExtractor.release();
                    Log.e("fuck", "finish");
                    m_handler.sendEmptyMessage(UPLOAD_AUDIO);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //发送音频获取字幕
    private void getSubtitleByServer(File file) {
        progressDialog.show();
        OkHttpUtils
                .post()
                .addFile("audio", "output_audio.m4a", file)
                .url(NetWorkConfig.UPLOAD_FACE_DATA)
                .addParams("audio", "audio")
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.i(TAG, "onError----" + e.getMessage() + "");
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        isAddCaptionSuccess = false;
                        Toast.makeText(EditActivity.this, "请求字幕失败--errorCode--" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Log.i(TAG, "onResponse----" + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String code = jsonObject.getString("code");
                            String msg = jsonObject.getString("msg");
                            String result = jsonObject.getString("result");
                            if (!TextUtils.isEmpty(code) && code.equals("0")) {
                                isAddCaptionSuccess = true;
                                subtitleList = GsonUtil.jsonToList(result, SubtitleBean.class);
                                if (subtitleList != null && subtitleList.size() > 0) {
                                    m_handler.sendEmptyMessage(UPDATE_SUBTITLE);
                                }
                            } else {
                                isAddCaptionSuccess = false;
                                if (!TextUtils.isEmpty(msg)) {
                                    Log.i(TAG, "msg----" + msg);
                                    Toast.makeText(EditActivity.this, "请求字幕失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            isAddCaptionSuccess = false;
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    }
                });
    }

    private void compileVideo() {
        int state = getCurrentEngineState();
        //停止引擎
        if (state == mStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
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
        mStreamingContext.compileTimeline(mTimeline, 0, mTimeline.getDuration(), m_compilePath, NvsStreamingContext.COMPILE_VIDEO_RESOLUTION_GRADE_1080, NvsStreamingContext.COMPILE_BITRATE_GRADE_HIGH, 0);
    }

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return mStreamingContext.getStreamingEngineState();
    }

    private void stopEngine() {
        if (mStreamingContext != null) {
            mStreamingContext.stop();//停止播放
        }
    }


    public void getStorageFilePath() {
        //要生成视频的路径
        File compileDir = new File(Environment.getExternalStorageDirectory(), "NvStreamingSdk" + File.separator + "Compile");
        if (!compileDir.exists() && !compileDir.mkdirs()) {
            Log.d(TAG, "Failed to make Compile directory");
            return;
        }

        File file = new File(compileDir, "video_sub.mp4");
        if (file.exists())
            file.delete();
        m_compilePath = file.getAbsolutePath();
    }

    @Override
    public void onCompileProgress(NvsTimeline nvsTimeline, int i) {

    }

    @Override
    public void onCompileFinished(NvsTimeline nvsTimeline) {
        isAddCaptionSuccess = true;
        //Toast.makeText(this, "生成文件：NvStreamingSdk/Compile/video_sub.mp4", Toast.LENGTH_LONG).show();
       /* Intent intent = new Intent(EditActivity.this, FxManageActivity.class);
        intent.putExtra("themePackagePath", themePackagePath);
        intent.putExtra("fxPackagePath", fxPackagePath);
        intent.putStringArrayListExtra("videoFilePathArray", (ArrayList<String>) Arrays.asList(m_compilePath));
        startActivity(intent);*/
    }

    @Override
    public void onCompileFailed(NvsTimeline nvsTimeline) {

    }
}
