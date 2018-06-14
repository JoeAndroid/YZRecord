package com.yanzhi.record.edit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meicam.sdk.NvsAssetPackageManager;
import com.meicam.sdk.NvsAudioResolution;
import com.meicam.sdk.NvsClip;
import com.meicam.sdk.NvsLiveWindow;
import com.meicam.sdk.NvsMultiThumbnailSequenceView;
import com.meicam.sdk.NvsRational;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoResolution;
import com.meicam.sdk.NvsVideoTrack;
import com.yanzhi.record.R;
import com.yanzhi.record.edit.adapter.FilterRecyclerViewAdapter;
import com.yanzhi.record.edit.adapter.ThemeRecyclerViewAdapter;
import com.yanzhi.record.edit.adapter.TransitionRecyclerViewAdapter;
import com.yanzhi.record.edit.timelineeditor.NvsTimelineEditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FxManageActivity extends AppCompatActivity implements NvsStreamingContext.PlaybackCallback, NvsStreamingContext.CompileCallback, NvsStreamingContext.PlaybackCallback2 {
    private static final String TAG = "FxAply";
    private static final int UPDATE_PLAYSTATE = 2;
    private NvsStreamingContext m_streamingContext;
    private LiveWindow m_liveWindow;
    private NvsTimeline m_timeline;
    private NvsVideoTrack m_videoTrack;
    private String themePackagePath;
    private String fxPackagePath;
    //
    private ImageView m_imageBack;
    private TextView m_buttonAddVideo;
    private ImageView m_buttonPlay;
    private NvsTimelineEditor m_timeLineEditor;
    private TextView m_currentPalyTime;
    private TextView m_totalPalyTime;

    private RelativeLayout m_themeBgImage;
    private RelativeLayout m_filterBgImage;
    private RelativeLayout m_transitionBgImage;
    private TextView m_themeTextView;
    private TextView m_filterTextView;
    private TextView m_transitionTextView;
    private RelativeLayout m_playRelativeLayout;

    private RecyclerView m_themeRecycleViewList;
    private RecyclerView m_filterRecycleViewList;
    private RecyclerView m_transitionRecycleViewList;

    private LinearLayout m_compilePage;
    private CompileVideo m_compileLinearLayout;
    private CountDownTimer m_countDownTimer;

    private NvsMultiThumbnailSequenceView m_multiThumbnailSequenceView = null;
    List<String> m_arrayPathList;
    private boolean isPlayState = false;
    private ThemeRecyclerViewAdapter m_themeRecycleAdapter;
    private FilterRecyclerViewAdapter m_filterRecycleAdapter;
    private TransitionRecyclerViewAdapter m_transitionRecycleAdapter;
    private ArrayList<FxInfoDescription> m_arrayThemeFxInfo;
    private ArrayList<FxInfoDescription> m_arrayFilterFxInfo;
    private ArrayList<FxInfoDescription> m_arrayTransitionFxInfo;
    private int m_defaultTotalClipCount = 0;

    NvsVideoResolution m_videoEditRes = new NvsVideoResolution();

    private int m_ratio = 0;
    private int m_QR = 0;
    private boolean m_fromCap = true;

    private String m_compilePath;

    private Handler m_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_PLAYSTATE: {
                    m_multiThumbnailSequenceView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                    seekTimeline(0, m_streamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    m_buttonPlay.setBackgroundResource(R.mipmap.play);
                    updateCurPlayTime(0);
                }
                break;
            }
        }
    };

    public static class FxInfoDescription {
        public String mFxName;
        public Bitmap mFxImage;
        public String mFxPackageId;

        public FxInfoDescription() {

        }
    }

    public static void actionStart(Context context, String themePackagePath, String fxPackagePath, ArrayList<String> paths, int ratio, int QR, boolean fromCap) {
        Intent intent = new Intent(context, FxManageActivity.class);
        intent.putExtra("themePackagePath", themePackagePath);
        intent.putExtra("fxPackagePath", fxPackagePath);
        intent.putStringArrayListExtra("videoFilePathArray", paths);
        intent.putExtra("ratio", ratio);
        intent.putExtra("QR", QR);
        intent.putExtra("fromCap", fromCap);
        context.startActivity(intent);
    }

    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_streamingContext = NvsStreamingContext.getInstance();
        setContentView(R.layout.activity_applyfx);
        getDataFromIntent();
        m_streamingContext.setThemeEndingEnabled(false);
        getStorageFilePath();
        initUI();

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
        createTimeline();
        addVideoClip();
        setControlListener();
        initThemeRecycleAdapter();
        initFilterRecycleAdapter();
        initTransitionRecycleAdapter();
    }

    private void initUI() {
        m_imageBack = (ImageView) findViewById(R.id.back);
        m_buttonAddVideo = (TextView) findViewById(R.id.redo_addvideo);
        m_liveWindow = (LiveWindow) findViewById(R.id.liveWindow);
        m_buttonPlay = (ImageView) findViewById(R.id.button_Play);
        m_playRelativeLayout = (RelativeLayout) findViewById(R.id.play_relativeLayout);
        m_timeLineEditor = (NvsTimelineEditor) findViewById(R.id.timelineEditor);
        m_currentPalyTime = (TextView) findViewById(R.id.curPlayTime);
        m_totalPalyTime = (TextView) findViewById(R.id.totoalDuration);

        m_themeBgImage = (RelativeLayout) findViewById(R.id.theme_BgImage);
        m_filterBgImage = (RelativeLayout) findViewById(R.id.filter_BgImage);
        m_transitionBgImage = (RelativeLayout) findViewById(R.id.transition_BgImage);
        m_themeTextView = (TextView) findViewById(R.id.theme_textView);
        m_themeTextView.setSelected(true);
        m_themeTextView.getPaint().setFakeBoldText(true);
        m_filterTextView = (TextView) findViewById(R.id.filter_textView);
        m_transitionTextView = (TextView) findViewById(R.id.transition_textView);

        m_themeRecycleViewList = (RecyclerView) findViewById(R.id.theme_recycleview);
        m_filterRecycleViewList = (RecyclerView) findViewById(R.id.filter_recycleview);
        m_transitionRecycleViewList = (RecyclerView) findViewById(R.id.transition_recycleview);

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
        m_multiThumbnailSequenceView = m_timeLineEditor.getMultiThumbnailSequenceView();

        // 本示例程序需要演示特效，所以需要安装一些特效包，包括主题，视频特效
        // 注意在安装过程中，如果资源包尺寸过大或者根据需要，可选择异步安装。此处我们选择同步安装。
        m_arrayFilterFxInfo = installPackage("filter", NvsAssetPackageManager.ASSET_PACKAGE_TYPE_VIDEOFX, ".videofx", ".jpg");
        m_arrayThemeFxInfo = installPackage("theme", NvsAssetPackageManager.ASSET_PACKAGE_TYPE_THEME, ".theme", ".jpg");
        m_arrayTransitionFxInfo = installPackage("transition", NvsAssetPackageManager.ASSET_PACKAGE_TYPE_VIDEOTRANSITION, ".videotransition", ".png");

        boolean package1Valid = true;
        // 安装主题资源包
        StringBuilder themeId = new StringBuilder();
        int error = m_streamingContext.getAssetPackageManager().installAssetPackage(themePackagePath, null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_THEME, true, themeId);
        if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
            Log.d(TAG, "Failed to install theme package!");
            package1Valid = false;
        }
        if (package1Valid) {
            FxInfoDescription themeInfo = createFxInfo("自选包裹", themeId.toString(), "ED51E571-2650-4754-A45D-AA1CFEA14A81.jpg");
            m_arrayThemeFxInfo.add(0, themeInfo);
        }
        //添加无主题
        FxInfoDescription themeNoneInfoDes = createFxInfo("无", "", "theme_none.png");
        m_arrayThemeFxInfo.add(0, themeNoneInfoDes);

        // 安装滤镜资源包
        package1Valid = true;
        StringBuilder fxPackageId = new StringBuilder();
        error = m_streamingContext.getAssetPackageManager().installAssetPackage(fxPackagePath, null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_VIDEOFX, true, fxPackageId);
        if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
            Log.d(TAG, "Failed to install fx package!");
            package1Valid = false;
        }
        if (package1Valid) {
            FxInfoDescription filterInfo = createFxInfo("自选包裹", fxPackageId.toString(), "7FFCF99A-5336-4464-BACD-9D32D5D2DC5E.jpg");
            m_arrayFilterFxInfo.add(0, filterInfo);
        }
        //添加无滤镜
        FxInfoDescription filterNoneInfoDes = createFxInfo("无", "", "filter_none.png");
        m_arrayFilterFxInfo.add(0, filterNoneInfoDes);
        //添加无转场
        FxInfoDescription transitionNoneInfoDes = createFxInfo("无", "", "transition_none.png");
        m_arrayTransitionFxInfo.add(0, transitionNoneInfoDes);
    }

    private FxInfoDescription createFxInfo(String fxName, String fxPackageId, String imagePath) {
        FxInfoDescription fxInfoDes = new FxInfoDescription();
        fxInfoDes.mFxName = fxName;
        fxInfoDes.mFxPackageId = fxPackageId;
        fxInfoDes.mFxImage = readImageBitmap(imagePath);
        return fxInfoDes;
    }

    private Bitmap readImageBitmap(String imageFilePath) {
        Bitmap bitmap = null;
        try {
            InputStream inStream = getAssets().open(imageFilePath);
            bitmap = BitmapFactory.decodeStream(inStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private ArrayList<FxInfoDescription> installPackage(String filePath,
                                                        int packageType,
                                                        String packageSuffixType,
                                                        String imageSuffix) {
        ArrayList<FxInfoDescription> arrayInfoList = new ArrayList<>();
        try {
            String[] fileList = this.getAssets().list(filePath);
            StringBuilder packageId = new StringBuilder();

            for (int index = 0; index < fileList.length; ++index) {
                if (fileList[index].endsWith(packageSuffixType)) {
                    FxManageActivity.FxInfoDescription fxInfoDes = new FxManageActivity.FxInfoDescription();
                    String tmpPackagePath = "assets:/" + filePath + "/" + fileList[index];
                    int error = m_streamingContext.getAssetPackageManager().installAssetPackage(tmpPackagePath, null, packageType, true, packageId);
                    if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                            && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
                        Log.d(TAG, "Failed to install theme package!" + packageId.toString());
                        continue;
                    }
                    fxInfoDes.mFxPackageId = packageId.toString();
                    //read theme bitmap
                    String themeImagePath = filePath + "/" + packageId.toString() + imageSuffix;
                    InputStream inStream = getAssets().open(themeImagePath);
                    fxInfoDes.mFxImage = BitmapFactory.decodeStream(inStream);
                    arrayInfoList.add(fxInfoDes);
                }
            }

            //read nsme list
            String nameListPath = filePath + "/" + "nameList.txt";
            InputStream nameListStream = this.getAssets().open(nameListPath);
            BufferedReader nameListBuffer = new BufferedReader(new InputStreamReader(nameListStream, "GBK"));

            String strLine;
            while ((strLine = nameListBuffer.readLine()) != null) {
                String[] strNameArray = strLine.split(",");
                for (int i = 0; i < arrayInfoList.size(); ++i) {
                    if (arrayInfoList.get(i).mFxPackageId.compareTo(strNameArray[0]) == 0) {
                        arrayInfoList.get(i).mFxName = strNameArray[1];
                        break;
                    }
                }
            }
            nameListBuffer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return arrayInfoList;
    }

    private void setControlListener() {
        m_imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroy();
            }
        });
        m_buttonAddVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopEngine();
                //合成视频
                compileVideo();
            }
        });

        m_themeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_themeTextView.setSelected(true);
                m_themeTextView.getPaint().setFakeBoldText(true);
                m_themeBgImage.setVisibility(View.VISIBLE);
                m_filterTextView.setSelected(false);
                m_filterTextView.getPaint().setFakeBoldText(false);
                m_filterBgImage.setVisibility(View.INVISIBLE);
                m_transitionTextView.setSelected(false);
                m_transitionTextView.getPaint().setFakeBoldText(false);
                m_transitionBgImage.setVisibility(View.INVISIBLE);

                m_themeRecycleViewList.setVisibility(View.VISIBLE);
                m_filterRecycleViewList.setVisibility(View.INVISIBLE);
                m_transitionRecycleViewList.setVisibility(View.INVISIBLE);
            }
        });
        m_filterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_themeTextView.setSelected(false);
                m_themeTextView.getPaint().setFakeBoldText(false);
                m_themeBgImage.setVisibility(View.INVISIBLE);
                m_filterTextView.setSelected(true);
                m_filterTextView.getPaint().setFakeBoldText(true);
                m_filterBgImage.setVisibility(View.VISIBLE);
                m_transitionTextView.setSelected(false);
                m_transitionTextView.getPaint().setFakeBoldText(false);
                m_transitionBgImage.setVisibility(View.INVISIBLE);

                m_themeRecycleViewList.setVisibility(View.INVISIBLE);
                m_filterRecycleViewList.setVisibility(View.VISIBLE);
                m_transitionRecycleViewList.setVisibility(View.INVISIBLE);
            }
        });
        m_transitionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_themeTextView.setSelected(false);
                m_themeTextView.getPaint().setFakeBoldText(false);
                m_themeBgImage.setVisibility(View.INVISIBLE);
                m_filterTextView.setSelected(false);
                m_filterTextView.getPaint().setFakeBoldText(false);
                m_filterBgImage.setVisibility(View.INVISIBLE);
                m_transitionTextView.setSelected(true);
                m_transitionTextView.getPaint().setFakeBoldText(true);
                m_transitionBgImage.setVisibility(View.VISIBLE);

                m_themeRecycleViewList.setVisibility(View.INVISIBLE);
                m_filterRecycleViewList.setVisibility(View.INVISIBLE);
                m_transitionRecycleViewList.setVisibility(View.VISIBLE);
            }
        });

        m_buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断当前引擎状态是否是播放状态
                if (getCurrentEngineState() != m_streamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    long startTime = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                    // 播放视频
                    m_streamingContext.playbackTimeline(m_timeline, startTime, m_timeline.getDuration(), NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
                    m_buttonPlay.setBackgroundResource(R.mipmap.pause);
                } else {
                    stopEngine();
                }
            }
        });

        /* 对ThumbnailSequenceView的滑动监听 */
        m_timeLineEditor.setOnScrollListener(new NvsTimelineEditor.OnScrollChangeListener() {
            @Override
            public void onScrollX(long timeStamp) {
                if (isPlayState)
                    return;

                seekTimeline(timeStamp, m_streamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                updateCurPlayTime((int) timeStamp);
            }
        });
        m_multiThumbnailSequenceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isPlayState = false;
                return false;
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

    private void initThemeRecycleAdapter() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(FxManageActivity.this, LinearLayoutManager.HORIZONTAL, false);
        m_themeRecycleViewList.setLayoutManager(layoutManager);
        m_themeRecycleAdapter = new ThemeRecyclerViewAdapter(FxManageActivity.this, m_arrayThemeFxInfo);
        m_themeRecycleViewList.setAdapter(m_themeRecycleAdapter);
        m_themeRecycleViewList.addItemDecoration(new SpaceItemDecoration(40, 16));
        m_themeRecycleAdapter.setOnItemClickListener(new ThemeRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                String themeId = m_timeline.getCurrentThemeId();
                if (themeId != "") {
                    m_timeline.removeCurrentTheme();
                }
                if (pos != 0) {
                    m_timeline.applyTheme(m_arrayThemeFxInfo.get(pos).mFxPackageId);
                }

                updateTotalDuration();
                int curClipCount = m_videoTrack.getClipCount();
                long curStamp = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                if (m_defaultTotalClipCount != curClipCount) {
                    ArrayList<String> arrayListPath = new ArrayList<String>();
                    for (int index = 0; index < curClipCount; ++index) {
                        NvsVideoClip clip = m_videoTrack.getClipByIndex(index);
                        arrayListPath.add(clip.getFilePath());
                    }
                    initTimelineEditor(arrayListPath);
                    curStamp = 0;
                    m_defaultTotalClipCount = curClipCount;
                    updateCurPlayTime(0);
                }

                //定位预览，使添加的主题显示在预览窗口上
                seekTimeline(curStamp, m_streamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });
    }

    private void initFilterRecycleAdapter() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(FxManageActivity.this, LinearLayoutManager.HORIZONTAL, false);
        m_filterRecycleViewList.setLayoutManager(layoutManager);
        m_filterRecycleAdapter = new FilterRecyclerViewAdapter(FxManageActivity.this, m_arrayFilterFxInfo);
        m_filterRecycleViewList.setAdapter(m_filterRecycleAdapter);
        m_filterRecycleViewList.addItemDecoration(new SpaceItemDecoration(40, 16));
        m_filterRecycleAdapter.setOnItemClickListener(new FilterRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                for (int i = 0; i < m_videoTrack.getClipCount(); i++) {
                    // 获取片段
                    NvsVideoClip videoClip = m_videoTrack.getClipByIndex(i);
                    // 移除片段的所有特效
                    videoClip.removeAllFx();
                    if (pos != 0) {
                        videoClip.appendPackagedFx(m_arrayFilterFxInfo.get(pos).mFxPackageId);//添加包裹特效
                    }
                }
                //定位预览，使添加的包裹式滤镜显示在预览窗口上
                long curStamp = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                seekTimeline(curStamp, m_streamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });
    }

    private void initTransitionRecycleAdapter() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(FxManageActivity.this, LinearLayoutManager.HORIZONTAL, false);
        m_transitionRecycleViewList.setLayoutManager(layoutManager);
        m_transitionRecycleAdapter = new TransitionRecyclerViewAdapter(FxManageActivity.this, m_arrayTransitionFxInfo);
        m_transitionRecycleViewList.setAdapter(m_transitionRecycleAdapter);
        m_transitionRecycleViewList.addItemDecoration(new SpaceItemDecoration(40, 16));
        m_transitionRecycleAdapter.setOnItemClickListener(new TransitionRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                //设置视频转场
                for (int i = 0; i < m_videoTrack.getClipCount(); i++) {
                    if (pos == 0) {
                        m_videoTrack.setPackagedTransition(i, "");
                        continue;
                    }
                    m_videoTrack.setPackagedTransition(i, m_arrayTransitionFxInfo.get(pos).mFxPackageId);
                }
                //定位预览，使添加的包裹式转场显示在预览窗口上
                long curStamp = m_streamingContext.getTimelineCurrentPosition(m_timeline);
                seekTimeline(curStamp, m_streamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
            }
        });
    }

    private void initTimelineEditor(List<String> paths) {
        ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> infoDescArray = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            NvsClip curClip = m_videoTrack.getClipByIndex(i);
            NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc infoDesc = new NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc();
            infoDesc.mediaFilePath = paths.get(i);
            infoDesc.trimIn = curClip.getTrimIn();
            infoDesc.trimOut = curClip.getTrimOut();
            infoDesc.inPoint = curClip.getInPoint();
            infoDesc.outPoint = curClip.getOutPoint();
            infoDesc.stillImageHint = false;
            infoDescArray.add(infoDesc);
        }
        int paddingWidth = m_playRelativeLayout.getLayoutParams().width;
        m_timeLineEditor.initTimelineEditor(infoDescArray, m_timeline.getDuration(), paddingWidth);
    }

    private void destroy() {
        if (m_streamingContext != null) {
            m_streamingContext.removeTimeline(m_timeline);
        }
        finish();
    }

    private void stopEngine() {
        m_buttonPlay.setBackgroundResource(R.mipmap.pause);
        if (m_streamingContext != null) {
            m_streamingContext.stop();//停止播放
        }
    }

    @Override
    public void onBackPressed() {
        destroy();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        themePackagePath = intent.getStringExtra("themePackagePath");
        fxPackagePath = intent.getStringExtra("fxPackagePath");
        m_arrayPathList = intent.getStringArrayListExtra("videoFilePathArray");
        m_ratio = intent.getIntExtra("ratio", 0);
        m_QR = intent.getIntExtra("QR", 0);
        m_fromCap = intent.getBooleanExtra("fromCap", true);
    }

    private void createTimeline() {
        if (null == m_streamingContext) {
            Log.e(TAG, "mStreamingContext is null!");
            return;
        }
        setRatio();

        NvsRational videoFps = new NvsRational(25, 1);
        NvsAudioResolution audioEditRes = new NvsAudioResolution();
        audioEditRes.sampleRate = 48000;
        audioEditRes.channelCount = 2;
        // 创建timeline
        m_timeline = m_streamingContext.createTimeline(m_videoEditRes, videoFps, audioEditRes);
        if (m_timeline == null) {
            Log.d(TAG, "创建timeline失败");
            return;
        }
//        m_timeline.setTimelineEndingLogo("assets:/water-mark.png", 0, 0, 0, 100);
        // 将timeline连接到NvsLiveWindow控件
        m_streamingContext.connectTimelineWithLiveWindow(m_timeline, m_liveWindow);
        m_streamingContext.setCompileCallback(this);//设置生成回调接口
        //给Streaming context 设置播放回调接口
        m_streamingContext.setPlaybackCallback(this);
        m_streamingContext.setPlaybackCallback2(this);

        m_videoTrack = m_timeline.appendVideoTrack();//添加视频轨道
        if (m_videoTrack == null) {
            Log.d(TAG, "添加videoTrack失败");
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


    private void addVideoClip() {
        for (int index = 0; index < m_arrayPathList.size(); ++index) {
            String videoPath = m_arrayPathList.get(index);
            NvsVideoClip clip = m_videoTrack.appendClip(videoPath);
            if (clip == null) {
                Toast.makeText(this, "视频片段错误" + videoPath, Toast.LENGTH_LONG).show();
            }
        }

        updateTotalDuration();
        initTimelineEditor(m_arrayPathList);
        m_defaultTotalClipCount = m_videoTrack.getClipCount();
    }

    private void updateTotalDuration() {
        long duration = m_timeline.getDuration();
        m_totalPalyTime.setText(formatTimeStrWithUs((int) duration));
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

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return m_streamingContext.getStreamingEngineState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPlaybackPreloadingCompletion(NvsTimeline var1) {

    }

    @Override
    public void onPlaybackStopped(NvsTimeline var1) {
        m_buttonPlay.setBackgroundResource(R.mipmap.play);
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
    public void onPlaybackTimelinePosition(NvsTimeline timeline, long position) {
        if (m_multiThumbnailSequenceView != null) {
            isPlayState = true;
            m_multiThumbnailSequenceView.smoothScrollTo(Math.round((float) (position / (float) m_timeline.getDuration() * m_timeLineEditor.getSequenceWidth())), 0);
        }
        updateCurPlayTime((int) position);
    }

    private void updateCurPlayTime(int time) {
        m_currentPalyTime.setText(formatTimeStrWithUs(time));
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
    public void onCompileProgress(NvsTimeline nvsTimeline, int progress) {
        m_compileLinearLayout.updateCompileProgress(progress);
    }

    @Override
    public void onCompileFinished(NvsTimeline nvsTimeline) {
        m_countDownTimer.start();
        Toast.makeText(this, "生成文件：NvStreamingSdk/Compile/video_mv.mp4", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(m_compilePath);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "video/mp4");
        startActivity(intent);
    }

    @Override
    public void onCompileFailed(NvsTimeline nvsTimeline) {
        m_compilePage.setVisibility(View.GONE);
        m_compileLinearLayout.compileVideoFinishState();
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


    public void getStorageFilePath() {
        //要生成视频的路径
        File compileDir = new File(Environment.getExternalStorageDirectory(), "NvStreamingSdk" + File.separator + "Compile");
        if (!compileDir.exists() && !compileDir.mkdirs()) {
            Log.d(TAG, "Failed to make Compile directory");
            return;
        }

        File file = new File(compileDir, "video_mv.mp4");
        if (file.exists())
            file.delete();
        m_compilePath = file.getAbsolutePath();
    }

}

