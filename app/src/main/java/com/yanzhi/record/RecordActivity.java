package com.yanzhi.record;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.meicam.sdk.NvsAssetPackageManager;
import com.meicam.sdk.NvsAudioResolution;
import com.meicam.sdk.NvsCaptureVideoFx;
import com.meicam.sdk.NvsColor;
import com.meicam.sdk.NvsFxDescription;
import com.meicam.sdk.NvsLiveWindow;
import com.meicam.sdk.NvsRational;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoResolution;
import com.meicam.sdk.NvsVideoTrack;
import com.yanzhi.record.edit.Clip;
import com.yanzhi.record.edit.CompileVideo;
import com.yanzhi.record.edit.EditActivity;
import com.yanzhi.record.utils.Utils;
import com.yanzhi.record.view.RecordTimelineView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RecordActivity extends Activity implements View.OnClickListener, NvsStreamingContext.CompileCallback, NvsStreamingContext.CaptureDeviceCallback, NvsStreamingContext.CaptureRecordingDurationCallback, NvsAssetPackageManager.AssetPackageManagerCallback {

    private static final String TAG = "RecordActivity";

    private static final int SCENE_COUNT = 2;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 0;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 2;

    public static final int MAX_DURATION = 5 * 60 * 1000;  //最长拍摄时长
    public static final int MIN_DURATION = 10 * 1000;  //最短拍摄时长

    private boolean m_permissionGranted;
    private NvsStreamingContext m_streamingContext;
    private NvsLiveWindow m_liveWindow;
    private NvsTimeline m_timeline = null;
    private NvsVideoTrack m_videoTrack;
    private CountDownTimer m_countDownTimer;
    private LinearLayout m_compilePage;
    private CompileVideo m_compileLinearLayout;

    private String m_compilePath;
    private volatile static long totalTime = 0L;//录制的总时长
    private long startPausedTime = 0L;//开始暂停的时间
    private long startRecordTime = 0L;//开始录制的时间
    private long proLineTime = 0;//进度条当前进度
    private static final int RECORDING = 0x11;//视频正在录制
    private static final int PAUSE = 0x22;//暂停录制
    private static final int STOP = 0x33;//视频已经停止
    private static final int CANCLE = 0x44;//视频取消
    private RecordTimelineView mRecordTimelineView;

    private LinkedList<Clip> clipList = new LinkedList<>();
    private Clip clip;//录制模块
    private static Handler mHandler;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isSelected = false;//删除

    private TextView tvSegment;
    private TextView tvSuccess;
    private Button m_buttonSwitchFacing;
    private Button buttonFlash;
    private ImageView delete_btn;
    private TextView mRecordTimeTxt;

    private Button m_buttonRecord;
    private Button buttonSelectFx;
    private Button buttonSetBeauty;

    private LinearLayout linearSegment;
    private ImageButton ibSceneNone;
    private ImageButton m_imageButton1;
    private ImageButton m_imageButton2;

    private RecyclerView recyclerview;

    private LinearLayout m_layoutBeauty;
    private Button m_buttonOpenBeauty;
    private SeekBar m_seekBarStrength;
    private TextView m_textStrengthValue;
    private SeekBar m_seekBarWhitening;
    private TextView m_textWhiteningValue;
    private SeekBar m_seekBarReddening;
    private TextView m_textReddeningValue;
    private boolean m_useBeauty = false;

    private int m_currentDeviceIndex = 0;
    private boolean m_firstInit;
    private String m_currentFxName;
    private String m_lastFxName;
    private double m_strengthValue;
    private double m_whiteningValue;
    private double m_reddeningValue;

    private StringBuilder m_fxPackageId;
    private ArrayList m_fxNameList;
    private List<FxItem> m_fxList;
    private FxAdapter m_fxAdapter;//适配器
    //是否可以选择抠像
    private boolean isCanSelectSegment;
    private ArrayList m_captureSceneIdArray;
    NvsRational m_rational = new NvsRational(9, 16);
    //是否正在录制中
    private boolean isRecording;
    private int currentOrientation;//当前屏幕方向
    private OrientationDetector orientationDetector;

    NvsVideoResolution m_videoEditRes = new NvsVideoResolution();
    private int m_ratio = 0;//宽高比  0. 16:9  9:16   1. 4:3  2. 1:1
    private int m_QR = 1;//分辨率默认720  0: 1080  1:720  2：480
    //开启录制时屏幕方向
    private boolean m_fromCap = true;//宽高对调，实现横竖屏的录制，16:9：false  9:16：true

    private ArrayList<String> recordPathList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置垂直
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //must set
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        m_streamingContext = NvsStreamingContext.init(this, null);
        setContentView(R.layout.activity_main);
        getStorageFilePath();
        initHandler();
        initView();

        m_currentFxName = "None";

        m_strengthValue = 0;
        m_whiteningValue = 0;
        m_reddeningValue = 0;

        m_currentDeviceIndex = 0;
        m_permissionGranted = false;
        m_firstInit = true;

        // 由于本示例程序需要演示虚拟场景，所以需要给拍摄添加一个抠像特技
        NvsCaptureVideoFx keyerFx = m_streamingContext.appendBuiltinCaptureVideoFx("Master Keyer");
        if (keyerFx != null) {
            // 开启溢色去除
            keyerFx.setBooleanVal("Spill Removal", true);
            // 将溢色去除强度设置为最低
            keyerFx.setFloatVal("Spill Removal Intensity", 0);
            //设置收缩边界强度
            keyerFx.setFloatVal("Shrink Intensity", 0.4);
        }

        // 将采集预览输出连接到NvsLiveWindow控件
        if (!m_streamingContext.connectCapturePreviewWithLiveWindow(m_liveWindow)) {
            Log.d(TAG, "连接预览窗口失败");
            return;
        }

        NvsFxDescription fxDescription = m_streamingContext.getVideoFxDescription("Beauty");
        List<NvsFxDescription.ParamInfoObject> paramInfo = fxDescription.getAllParamsInfo();
        for (NvsFxDescription.ParamInfoObject param : paramInfo) {
            String paramName = param.getString("paramName");
            if (paramName.equals("Strength")) {
                double maxValue = param.getFloat("floatMaxVal");
                m_strengthValue = param.getFloat("floatDefVal");
                m_seekBarStrength.setMax((int) (maxValue * 100));
                m_seekBarStrength.setProgress((int) (m_strengthValue * 100));
                m_textStrengthValue.setText(String.format(Locale.getDefault(), "%.2f", m_strengthValue));
            } else if (paramName.equals("Whitening")) {
                double maxValue = param.getFloat("floatMaxVal");
                m_whiteningValue = param.getFloat("floatDefVal");
                m_seekBarWhitening.setMax((int) (maxValue * 100));
                m_seekBarWhitening.setProgress((int) (m_whiteningValue * 100));
                m_textWhiteningValue.setText(String.format(Locale.getDefault(), "%.2f", m_whiteningValue));
            } else if (paramName.equals("Reddening")) {
                double maxValue = param.getFloat("floatMaxVal");
                m_reddeningValue = param.getFloat("floatDefVal");
                m_seekBarReddening.setMax((int) (maxValue * 100));
                m_seekBarReddening.setProgress((int) (m_reddeningValue * 100));
                m_textReddeningValue.setText(String.format(Locale.getDefault(), "%.2f", m_reddeningValue));
            }
        }

        //设置水印
        m_streamingContext.setUserWatermarkForCapture("assets:/logo.png", 0, 0, 1, NvsTimeline.NvsTimelineWatermarkPosition_TopLeft, 20, 20);

        setData();
        setControlListener();
        requestPermission();
        initOritationDetector();
    }

    /**
     * 监听屏幕方向
     */
    private void initOritationDetector() {
        orientationDetector = new OrientationDetector(this);
        orientationDetector.setOrientationChangedListener(new OrientationDetector.OrientationChangedListener() {
            @Override
            public void onOrientationChanged(int orientation) {
                if (isRecording || !recordPathList.isEmpty()) {
                    return;
                }
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }
                //只检测是否有四个角度的改变
                if (orientation > 350 || orientation < 10) { //0度
                    orientation = 0;
                    m_fromCap = true;
                } else if (orientation > 80 && orientation < 100) { //90度
                    orientation = 90;
                    m_fromCap = false;
                } else if (orientation > 170 && orientation < 190) { //180度
                    orientation = 180;
                    m_fromCap = true;
                } else if (orientation > 260 && orientation < 280) { //270度
                    orientation = 270;
                    m_fromCap = false;
                } else {
                    return;
                }
                //旋转图标
                if (currentOrientation != orientation) {
                  /*  if (orientation == 90) {
                        startPropertyAnim(0, -90);
                    } else if (orientation == 270) {
                        startPropertyAnim(0, 90);
                    } else {
                        if (currentOrientation == 90) {
                            startPropertyAnim(-90, 0);
                        } else if (currentOrientation == 270) {
                            startPropertyAnim(90, 0);
                        }
                    }*/
                    currentOrientation = orientation;
                } else if (currentOrientation != orientation) {
                   /* if (orientation == 90) {
                        startPropertyAnim(0, 90);
                    } else if (orientation == 270) {
                        startPropertyAnim(0, -90);
                    } else {
                        if (currentOrientation == 90) {
                            startPropertyAnim(90, 0);
                        } else if (currentOrientation == 270) {
                            startPropertyAnim(-90, 0);
                        }
                    }*/
                    currentOrientation = orientation;
                }
            }
        });
    }


    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)) {
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)) {
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        m_permissionGranted = true;
                        if (!startCapturePreview(false))
                            return;
                    } else {
                        setCaptureEnabled(false);
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                    }
                } else {
                    setCaptureEnabled(false);
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION_CODE);
                }
            } else {
                setCaptureEnabled(false);
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
            }
        } else {
            m_permissionGranted = true;
            if (!startCapturePreview(false))
                return;
        }
    }


    private void initView() {
        m_liveWindow = findViewById(R.id.liveWindow);
        mRecordTimeTxt = findViewById(R.id.record_time);

        mRecordTimelineView = (RecordTimelineView) findViewById(R.id.recordTimelineView);
        mRecordTimelineView.setColor(R.color.record_fill_progress, R.color.holo_red_dark, R.color.black_opacity_70pct, R.color.transparent);
        mRecordTimelineView.setMaxDuration(MAX_DURATION);
        mRecordTimelineView.setMinDuration(MIN_DURATION);

        tvSegment = findViewById(R.id.tvSegment);
        tvSuccess = findViewById(R.id.tvSuccess);
        recyclerview = findViewById(R.id.recyclerview);
        recyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        buttonSelectFx = findViewById(R.id.buttonSelectFx);
        buttonSetBeauty = findViewById(R.id.buttonSetBeauty);
        m_buttonRecord = findViewById(R.id.buttonRecord);
        delete_btn = findViewById(R.id.delete_btn);
        linearSegment = findViewById(R.id.linearSegment);
        ibSceneNone = findViewById(R.id.ibSceneNone);
        m_imageButton1 = findViewById(R.id.imageButton1);
        m_imageButton2 = findViewById(R.id.imageButton2);
        m_buttonSwitchFacing = findViewById(R.id.buttonSwitchFacing);
        buttonFlash = findViewById(R.id.buttonFlash);

        m_layoutBeauty = findViewById(R.id.beautyLayout);
        m_buttonOpenBeauty = findViewById(R.id.buttonOpenBeauty);
        m_seekBarStrength = findViewById(R.id.seekBarStrength);
        m_textStrengthValue = findViewById(R.id.textStrengthValue);
        m_seekBarWhitening = findViewById(R.id.seekBarWhitening);
        m_textWhiteningValue = findViewById(R.id.textWhiteningValue);
        m_seekBarReddening = findViewById(R.id.seekBarReddening);
        m_textReddeningValue = findViewById(R.id.textReddeningValue);

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

        tvSuccess.setOnClickListener(this);
        tvSegment.setOnClickListener(this);
        m_buttonRecord.setOnClickListener(this);
        delete_btn.setOnClickListener(this);
        ibSceneNone.setOnClickListener(this);
        m_imageButton1.setOnClickListener(this);
        m_imageButton2.setOnClickListener(this);
        m_buttonSwitchFacing.setOnClickListener(this);
        buttonSelectFx.setOnClickListener(this);
        buttonFlash.setOnClickListener(this);
        buttonSetBeauty.setOnClickListener(this);
        m_buttonOpenBeauty.setOnClickListener(this);
        initFxListView();

        m_seekBarStrength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_strengthValue = progress * 0.01;
                m_textStrengthValue.setText(String.format(Locale.getDefault(), "%.2f", m_strengthValue));

                for (int i = 0; i < m_streamingContext.getCaptureVideoFxCount(); i++) {
                    NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                    String name = fx.getBuiltinCaptureVideoFxName();
                    if (name.equals("Beauty")) {
                        //设置美颜强度值
                        fx.setFloatVal("Strength", m_strengthValue);
                        break;
                    }
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        m_seekBarWhitening.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_whiteningValue = progress * 0.01;
                m_textWhiteningValue.setText(String.format(Locale.getDefault(), "%.2f", m_whiteningValue));

                for (int i = 0; i < m_streamingContext.getCaptureVideoFxCount(); i++) {
                    NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                    String name = fx.getBuiltinCaptureVideoFxName();
                    if (name.equals("Beauty")) {
                        fx.setFloatVal("Whitening", m_whiteningValue);
                        break;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        m_seekBarReddening.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_reddeningValue = progress * 0.01;
                m_textReddeningValue.setText(String.format(Locale.getDefault(), "%.2f", m_reddeningValue));

                for (int i = 0; i < m_streamingContext.getCaptureVideoFxCount(); i++) {
                    NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                    String name = fx.getBuiltinCaptureVideoFxName();
                    if (name.equals("Beauty")) {
                        fx.setFloatVal("Reddening", m_reddeningValue);
                        break;
                    }
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
//        m_streamingContext.connectTimelineWithLiveWindow(m_timeline, m_liveWindow);
        m_streamingContext.setCompileCallback(this);//设置生成回调接口
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

    private void initFxListView() {
        int[] resImags = {
                R.mipmap.none, R.mipmap.sage, R.mipmap.maid, R.mipmap.mace,
                R.mipmap.lace, R.mipmap.mall, R.mipmap.sap, R.mipmap.sara,
                R.mipmap.pinky, R.mipmap.sweet, R.mipmap.fresh, R.mipmap.package1
        };

        // 本示例程序需要演示采集特效，所以需要安装一个视频采集特效包
        // 此处采用同步安装方式，如果资源包尺寸太大或者根据需要可使用异步安装方式
        boolean package1Valid = true;
        String package1Path = "assets:/7FFCF99A-5336-4464-BACD-9D32D5D2DC5E.videofx";
        m_fxPackageId = new StringBuilder();
        int error = m_streamingContext.getAssetPackageManager().installAssetPackage(package1Path, null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_VIDEOFX, true, m_fxPackageId);
        if (error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR
                && error != NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_ALREADY_INSTALLED) {
            Log.e(TAG, "Failed to install asset package!");
            package1Valid = false;
        }

        // 采集特效列表
        m_fxNameList = new ArrayList();
        m_fxNameList.add("None");

        m_fxNameList.addAll(m_streamingContext.getAllBuiltinCaptureVideoFxNames());
        if (package1Valid)
            m_fxNameList.add("Package1");

        m_fxList = new ArrayList<FxItem>();

        for (int i = 0; i < m_fxNameList.size(); i++) {
            if (i >= resImags.length)
                break;
            String fxName = String.valueOf(m_fxNameList.get(i));
            int fxResId = resImags[i];
            FxItem fxItem = new FxItem(fxName, fxResId);
            m_fxList.add(fxItem);
        }
        m_fxAdapter = new FxAdapter(this, m_fxList);
        recyclerview.setAdapter(m_fxAdapter);
        m_fxAdapter.setOnItemClickListener(new FxAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int pos) {
                String fxName = String.valueOf(m_fxNameList.get(pos));
                if (fxName.equals(m_currentFxName))
                    return;

                m_currentFxName = fxName;
                if (!TextUtils.isEmpty(m_lastFxName)) {
                    int count = m_streamingContext.getCaptureVideoFxCount(); //移除所有采集视频特效
                    for (int i = 0; i < count; i++) {
                        NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                        String name = fx.getBuiltinCaptureVideoFxName();
                        if (name.equals(m_lastFxName)) {
                            m_streamingContext.removeCaptureVideoFx(i);
                            break;
                        }
                    }
                }
               /* if (m_useBeauty) {
                    NvsCaptureVideoFx fx = m_streamingContext.appendBeautyCaptureVideoFx();     //添加美颜采集特效
                    fx.setFloatVal("Strength", m_strengthValue);//设置美颜强度值
                    fx.setFloatVal("Whitening", m_whiteningValue);
                    fx.setFloatVal("Reddening", m_reddeningValue);
                }*/
                if (fxName.equals("None"))
                    return;

                //添加包裹采集特效
                if (fxName.equals("Package1")) {
                    m_streamingContext.appendPackagedCaptureVideoFx(m_fxPackageId.toString());
                    return;
                }
                //添加内建采集特效
                m_streamingContext.appendBuiltinCaptureVideoFx(fxName);
                m_lastFxName = fxName;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == m_buttonOpenBeauty) {//开启和关闭美颜
            if (!m_useBeauty) {
                NvsCaptureVideoFx fx = m_streamingContext.appendBeautyCaptureVideoFx();     //添加美颜采集特效
                fx.setFloatVal("Strength", 0.5);//设置美颜强度值
                fx.setFloatVal("Whitening", 1.0);
                fx.setFloatVal("Reddening", 0.0);
/*
                fx.setFloatVal("Strength", m_strengthValue);//设置美颜强度值
                fx.setFloatVal("Whitening", m_whiteningValue);
                fx.setFloatVal("Reddening", m_reddeningValue);
*/
                m_useBeauty = true;
                m_buttonOpenBeauty.setText(R.string.closeBeauty);
            } else {
                int count = m_streamingContext.getCaptureVideoFxCount(); //移除所有采集视频特效
                for (int i = 0; i < count; i++) {
                    NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                    String name = fx.getBuiltinCaptureVideoFxName();
                    if (name.equals("Beauty")) {
                        m_streamingContext.removeCaptureVideoFx(i);
                        break;
                    }
                }
                m_buttonOpenBeauty.setText(R.string.openBeauty);
                m_useBeauty = false;
            }
        } else if (v == buttonSetBeauty) {//显示美颜
           /* if (m_layoutBeauty.getVisibility() == View.VISIBLE) {
                m_layoutBeauty.setVisibility(View.GONE);
            } else {
                m_layoutBeauty.setVisibility(View.VISIBLE);
            }*/
            if (!m_useBeauty) {
                NvsCaptureVideoFx fx = m_streamingContext.appendBeautyCaptureVideoFx();     //添加美颜采集特效
                fx.setFloatVal("Strength", 0.5);//设置美颜强度值
                fx.setFloatVal("Whitening", 1.0);
                fx.setFloatVal("Reddening", 0.0);
/*
                fx.setFloatVal("Strength", m_strengthValue);//设置美颜强度值
                fx.setFloatVal("Whitening", m_whiteningValue);
                fx.setFloatVal("Reddening", m_reddeningValue);
*/
                m_useBeauty = true;
                m_buttonOpenBeauty.setText(R.string.closeBeauty);
            } else {
                int count = m_streamingContext.getCaptureVideoFxCount(); //移除所有采集视频特效
                for (int i = 0; i < count; i++) {
                    NvsCaptureVideoFx fx = m_streamingContext.getCaptureVideoFxByIndex(i);
                    String name = fx.getBuiltinCaptureVideoFxName();
                    if (name.equals("Beauty")) {
                        m_streamingContext.removeCaptureVideoFx(i);
                        break;
                    }
                }
                m_buttonOpenBeauty.setText(R.string.openBeauty);
                m_useBeauty = false;
            }
        } else if (v == buttonFlash) {//开启闪光灯
            if (m_streamingContext.isFlashOn()) {
                m_streamingContext.toggleFlash(false);
                setLevel(buttonFlash, 1);
            } else {
                m_streamingContext.toggleFlash(true);
                setLevel(buttonFlash, 2);
            }
        } else if (v == buttonSelectFx) {//显示路径
            if (recyclerview.getVisibility() == View.VISIBLE) {
                recyclerview.setVisibility(View.GONE);
            } else {
                recyclerview.setVisibility(View.VISIBLE);
            }
        } else if (v == m_buttonSwitchFacing) {//切换摄像头
            if (m_currentDeviceIndex == 0)
                m_currentDeviceIndex = 1;
            else
                m_currentDeviceIndex = 0;
            startCapturePreview(true);
        } else if (v == tvSegment) {//显示抠像特效
            if (linearSegment.getVisibility() == View.VISIBLE) {
                linearSegment.setVisibility(View.GONE);
                isCanSelectSegment = false;
            } else {
                linearSegment.setVisibility(View.VISIBLE);
                isCanSelectSegment = true;
            }
        } else if (v == m_buttonRecord) {//录制
            // 当前在录制状态，可停止视频录制
            if (getCurrentEngineState() == m_streamingContext.STREAMING_ENGINE_STATE_CAPTURERECORDING) {
                m_streamingContext.stopRecording();
                startPausedTime = System.currentTimeMillis();
                if (null != clip && null != clipList) {
                    clip.setEndTime(System.currentTimeMillis());
                    clipList.add(clip);
                }
                isSelected = false;
                mHandler.sendEmptyMessage(PAUSE);
                setLevel(m_buttonRecord, 1);
                isRecording = false;
                m_buttonRecord.setText(R.string.record);
                if (m_streamingContext.getCaptureDeviceCount() > 1)
                    m_buttonSwitchFacing.setEnabled(true);

                return;
            }

            File captureDir = new File(Environment.getExternalStorageDirectory(), "NvStreamingSdk" + File.separator + "Record");
            if (!captureDir.exists() && !captureDir.mkdirs()) {
                Log.e(TAG, "Failed to make Record directory");
                return;
            }

            String fileName = getCharacterAndNumber() + ".mp4";
            File file = new File(captureDir, fileName);
            if (file.exists())
                file.delete();
            //当前未在视频录制状态，则启动视频录制。此处使用不带特效的录制方式，因为在采集预览时参数flags值设为0
            if (!m_streamingContext.startRecording(file.getAbsolutePath()))
                return;
            isRecording = true;

            timer = new Timer();
            //现在开始每10毫秒秒走一次
            initTask();
            timer.schedule(timerTask, 0, 5);

            mRecordTimelineView.setDuration((int) proLineTime);
            mRecordTimeTxt.setVisibility(View.VISIBLE);

            //保存录制文件路径
            recordPathList.add(file.getAbsolutePath());
            setLevel(m_buttonRecord, 2);
            m_buttonRecord.setText("");
            m_buttonSwitchFacing.setEnabled(false);
        } else if (v == ibSceneNone) {
            // 将吸取下来的背景画面颜色值设置给抠像特技
            NvsCaptureVideoFx keyerFx = m_streamingContext.getCaptureVideoFxByIndex(0);
            if (keyerFx != null) {
                keyerFx.setColorVal("Key Color", new NvsColor(0, 0, 0, 0));
            }
        } else if (v == m_imageButton1) {
            selectScene(0);
        } else if (v == m_imageButton2) {
            selectScene(1);
        } else if (v == tvSuccess) {

            if (recordPathList != null && recordPathList.size() > 0) {
                //初始化 TimeLine videoTrack
                initTimeline();
                //添加视频片段
                showResult(recordPathList);
                //执行合成视频并跳转编辑
                compileVideo();
            }
        } else if (v == delete_btn) {//删除分段
            if (recordPathList != null && recordPathList.size() > 0) {
                if (!isSelected) {
                    isSelected = true;
                    mRecordTimelineView.selectLast();
                } else {
                    isSelected = false;
                    File file = new File(recordPathList.get(recordPathList.size() - 1));
                    if (file.exists() && file.isFile()) {
                        file.delete();
                    }
                    recordPathList.remove(recordPathList.size() - 1);

                    mRecordTimelineView.deleteLast();
                    totalTime = totalTime - clipList.getLast().getDuration();
                    if (totalTime < MIN_DURATION) {
                    }
                    clipList.removeLast();
                    if (clipList.size() == 0) {
                        totalTime = 0;
                        startPausedTime = 0;
                        startRecordTime = 0;
                    }
                }
            }

        }

    }

    public void initHandler() {
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case RECORDING:
                        //正在录制
                        if (totalTime >= MAX_DURATION) { //超过最大时间 停止录制

                            mHandler.removeMessages(RECORDING);
                            mHandler.removeMessages(PAUSE);
                            mHandler.removeMessages(STOP);
                            mHandler.sendEmptyMessage(STOP);
                        } else {
                            proLineTime = System.currentTimeMillis() - startRecordTime;
                            //计算显示录制
                            int time = (int) ((totalTime + proLineTime) / 1000);
                            int min = time / 60;
                            int sec = time % 60;
                            mRecordTimeTxt.setText(String.format("%1$02d:%2$02d", min, sec));
                            if (mRecordTimeTxt.getVisibility() != View.VISIBLE) {
                                mRecordTimeTxt.setVisibility(View.VISIBLE);
                            }
                            if (0 != startRecordTime) {
                                mRecordTimelineView.setDuration((int) proLineTime);
                                //大于最小时间
                                if ((totalTime + proLineTime) >= MAX_DURATION) { //超过最大时间 停止录制
                                    totalTime += proLineTime;

                                    mHandler.removeMessages(RECORDING);
                                    mHandler.removeMessages(PAUSE);
                                    mHandler.removeMessages(STOP);
                                    mHandler.sendEmptyMessage(STOP);
                                } else if ((totalTime + proLineTime) >= MIN_DURATION) {
                                    //下一步

                                }
                            }
                        }
                        break;
                    case PAUSE:
                        if (null != timer) {
                            timer.cancel();
                            timer = null;
                        }
                        if (null != timerTask) {
                            timerTask.cancel();
                            timer = null;
                        }
                        //录制暂停
                        mRecordTimelineView.setDuration((int) (startPausedTime - startRecordTime));
                        mRecordTimelineView.clipComplete();
                        totalTime += (startPausedTime - startRecordTime);
                        mRecordTimeTxt.setVisibility(View.GONE);
                        //大于最小时间
                        if (totalTime >= MAX_DURATION) {   //超过最大时间 停止录制

                            mHandler.removeMessages(RECORDING);
                            mHandler.removeMessages(PAUSE);
                            mHandler.removeMessages(STOP);
                            mHandler.sendEmptyMessage(STOP);
                        } else if (totalTime >= MIN_DURATION) {
                            //下一步

                        }
                        //暂停计时
                        startPausedTime = System.currentTimeMillis();
                        startRecordTime = 0;
                        proLineTime = 0;
                        break;
                    case CANCLE:
                        //录制取消
                        if (null != timer) {
                            timer.cancel();
                            timer = null;
                        }
                        if (null != timerTask) {
                            timerTask.cancel();
                            timer = null;
                        }
                        mRecordTimeTxt.setVisibility(View.GONE);
                        mRecordTimelineView.setDuration(0);
                        //暂停计时
                        startPausedTime = System.currentTimeMillis();
                        startRecordTime = 0;
                        proLineTime = 0;
                        break;
                    case STOP:
                        //录制停止
                        if (null != timer) {
                            timer.cancel();
                            timer = null;
                        }
                        if (null != timerTask) {
                            timerTask.cancel();
                            timer = null;
                        }
                        mRecordTimeTxt.setVisibility(View.GONE);
                        startRecordTime = 0;
                        proLineTime = 0;
                        isRecording = false;
                        if (getCurrentEngineState() == m_streamingContext.STREAMING_ENGINE_STATE_CAPTURERECORDING) {
                            m_streamingContext.stopRecording();
                            setLevel(m_buttonRecord, 1);
                            m_buttonRecord.setText(R.string.record);
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 初始化计时器
     */

    private void initTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (isRecording) {
                    //播放计时
                    if (0 == startRecordTime) {
                        startRecordTime = System.currentTimeMillis();
                        clip = new Clip();
                        clip.setStartTime(startRecordTime);
                    }
                    boolean isRecord = (totalTime < MAX_DURATION);
                    if (isRecord) {
                        //发送正在录制
                        mHandler.removeMessages(RECORDING);
                        mHandler.removeMessages(PAUSE);
                        mHandler.sendEmptyMessage(RECORDING);
                    } else {
                        //时间已经到最大值
                        mHandler.removeMessages(RECORDING);
                        mHandler.removeMessages(PAUSE);
                        mHandler.removeMessages(STOP);
                        mHandler.sendEmptyMessage(STOP);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        //采集设备的数量判定
        if (m_streamingContext.getCaptureDeviceCount() > 1)
            m_buttonSwitchFacing.setEnabled(true);

        startCapturePreview(false);
        //开启屏幕方向监听
        if (orientationDetector != null && orientationDetector.canDetectOrientation()) {
            orientationDetector.enable();
        }
        super.onResume();
    }

    private void setData() {
        isCanSelectSegment = false;
        m_captureSceneIdArray = new ArrayList(SCENE_COUNT);
        String sceneId = "897F4258-74C0-4F89-884E-3E6C07E3EE0E";
        m_captureSceneIdArray.add(sceneId);
        sceneId = "8FB5A4C7-BAFC-4FCD-9994-F496A78F47C3";
        m_captureSceneIdArray.add(sceneId);
    }

    private void setControlListener() {
        // 给streaming context设置回调接口
        m_streamingContext.setCaptureRecordingDurationCallback(this);
        m_streamingContext.setCaptureDeviceCallback(this);
        m_streamingContext.getAssetPackageManager().setCallbackInterface(this);

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

        if (m_streamingContext.getCaptureDeviceCount() == 0)
            return;

        m_liveWindow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isCanSelectSegment)
                    return false;
                //
                //从NvsLiveWindow控件的点击位置吸取背景画面的颜色值
                //
                int sampleWidth = 20, sampleHeight = 20;
                RectF sampleRect = new RectF();
                sampleRect.left = (int) (event.getX() - sampleWidth / 2);
                if (sampleRect.left < 0) {
                    sampleRect.left = 0;
                } else if (sampleRect.left + sampleWidth > m_liveWindow.getWidth()) {
                    sampleRect.left = m_liveWindow.getWidth() - sampleWidth;
                }

                sampleRect.top = (int) (event.getY() - sampleHeight / 2);
                if (sampleRect.top < 0) {
                    sampleRect.top = 0;
                } else if (sampleRect.top + sampleHeight > m_liveWindow.getHeight()) {
                    sampleRect.top = m_liveWindow.getHeight() - sampleHeight;
                }
                sampleRect.right = sampleRect.left + sampleWidth;
                sampleRect.bottom = sampleRect.top + sampleHeight;
                NvsColor sampledColor = m_streamingContext.sampleColorFromCapturedVideoFrame(sampleRect);

                // 将吸取下来的背景画面颜色值设置给抠像特技
                NvsCaptureVideoFx keyerFx = m_streamingContext.getCaptureVideoFxByIndex(0);
                if (keyerFx == null) {
                    return false;
                }
                keyerFx.setColorVal("Key Color", sampledColor);
                return true;
            }
        });

    }

    private void selectScene(int sceneIndex) {

        String sceneId = (String) m_captureSceneIdArray.get(sceneIndex);
        String scenePackageFilePath = getScenePackageFilePath(sceneId);

        // 首先检查改拍摄场景的资源包是否已经安装
        NvsAssetPackageManager assetPackageManager = m_streamingContext.getAssetPackageManager();
        int packageStatus = assetPackageManager.getAssetPackageStatus(sceneId, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_CAPTURESCENE);
        if (packageStatus == NvsAssetPackageManager.ASSET_PACKAGE_STATUS_NOTINSTALLED) {
            // 该拍摄场景的资源包尚未安装，现在安装该资源包，由于拍摄场景的资源包尺寸较大
            // 为了不freeze UI，我们采用异步安装模式
            assetPackageManager.installAssetPackage(scenePackageFilePath, null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_CAPTURESCENE, false, null);
        } else {
            // 该拍摄场景的资源包已经安装，应用其效果
            m_streamingContext.applyCaptureScene(sceneId);
        }

    }

    private boolean startCapturePreview(boolean deviceChanged) {
        // 判断当前引擎状态是否为采集预览状态
        if (m_permissionGranted && (deviceChanged || getCurrentEngineState() != NvsStreamingContext.STREAMING_ENGINE_STATE_CAPTUREPREVIEW)) {
            //此样例使用高质量、横纵比为1:1的设置启动采集预览
            if (!m_streamingContext.startCapturePreview(m_currentDeviceIndex, NvsStreamingContext.VIDEO_CAPTURE_RESOLUTION_GRADE_HIGH, NvsStreamingContext.STREAMING_ENGINE_CAPTURE_FLAG_GRAB_CAPTURED_VIDEO_FRAME | NvsStreamingContext.STREAMING_ENGINE_CAPTURE_FLAG_DONT_USE_SYSTEM_RECORDER, m_rational)) {
                Log.e(TAG, "Failed to start capture preview!");
                m_buttonRecord.setEnabled(false);
                return false;
            }
            m_buttonRecord.setEnabled(true);
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return;

        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)) {
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        m_permissionGranted = true;
                        startCapturePreview(false);
                    } else
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION_CODE);
                }
                break;
            case REQUEST_RECORD_AUDIO_PERMISSION_CODE:
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    m_permissionGranted = true;
                    startCapturePreview(false);
                } else
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE:
                m_permissionGranted = true;
                startCapturePreview(false);
                break;
        }
    }

    @Override
    public void onFinishAssetPackageInstallation(String assetPackageId, String assetPackageFilePath, int assetPackageType, int error) {
        if (error == NvsAssetPackageManager.ASSET_PACKAGE_MANAGER_ERROR_NO_ERROR) {
            m_imageButton1.setEnabled(true);
            m_imageButton2.setEnabled(true);
            m_streamingContext.applyCaptureScene(assetPackageId);
        }
    }

    public void onFinishAssetPackageUpgrading(String assetPackageId, String assetPackageFilePath, int assetPackageType, int error) {

    }


    @Override
    public void onCaptureDeviceCapsReady(int captureDeviceIndex) {
        if (captureDeviceIndex != m_currentDeviceIndex)
            return;
        if (m_firstInit) {
            setCaptureEnabled(true);
            m_firstInit = false;
        }
    }


    @Override
    public void onCaptureDevicePreviewResolutionReady(int i) {

    }

    @Override
    public void onCaptureDevicePreviewStarted(int i) {

    }

    @Override
    public void onCaptureDeviceError(int i, int i1) {

    }

    @Override
    public void onCaptureDeviceStopped(int i) {

    }

    @Override
    public void onCaptureDeviceAutoFocusComplete(int i, boolean b) {

    }

    @Override
    public void onCaptureRecordingFinished(int i) {

    }

    @Override
    public void onCaptureRecordingError(int i) {

    }


    private String getScenePackageFilePath(String sceneId) {
        String packageFilePath = String.format("assets:/%s.capturescene", sceneId);
        return packageFilePath;
    }

    private void setCaptureEnabled(boolean enabled) {
        m_buttonRecord.setEnabled(enabled);
        if (enabled && m_streamingContext.getCaptureDeviceCount() > 1) {
            m_buttonSwitchFacing.setEnabled(true);
        } else
            m_buttonSwitchFacing.setEnabled(false);
    }

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return m_streamingContext.getStreamingEngineState();
    }

    private void setLevel(Button button, int level) {
        button.getBackground().setLevel(level);
    }

    private String getCharacterAndNumber() {
        String rel = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        rel = formatter.format(curDate);
        return rel;
    }

    @Override
    protected void onDestroy() {
        //streamingContext销毁
        m_streamingContext = null;
        NvsStreamingContext.close();
        if (orientationDetector != null) {
            orientationDetector.setOrientationChangedListener(null);
        }
        orientationDetector.disable();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        setLevel(m_buttonRecord, 1);
        m_buttonRecord.setText(R.string.record);
        //停止引擎
        m_streamingContext.stop();
        super.onPause();
    }

    @Override
    public void onCaptureRecordingDuration(int i, long l) {

    }

    private void compileVideo() {
        m_compilePage.setVisibility(View.VISIBLE);
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

    private void showResult(ArrayList<String> pathList) {
        if (null == m_videoTrack) {
            Log.e(TAG, "mVideoTrack is null");
            return;
        }

        m_videoTrack.removeAllClips();

        for (int i = 0; i < pathList.size(); i++) {
            NvsVideoClip clip = m_videoTrack.appendClip(pathList.get(i));  //添加视频片段
            if (clip == null) {
                Toast.makeText(this, "Failed to Append Clip" + pathList.get(i), Toast.LENGTH_LONG).show();
                return;
            }

        }
    }

    @Override
    public void onCompileProgress(NvsTimeline nvsTimeline, int progress) {
        m_compileLinearLayout.updateCompileProgress(progress);
    }

    @Override
    public void onCompileFinished(NvsTimeline nvsTimeline) {
        m_compileLinearLayout.compileVideoFinishState();
        m_countDownTimer.start();
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(m_compilePath);
        EditActivity.actionStart(this, arrayList, m_ratio, m_QR, m_fromCap);
    }

    @Override
    public void onCompileFailed(NvsTimeline nvsTimeline) {
        m_compilePage.setVisibility(View.GONE);
        m_compileLinearLayout.compileVideoInitState();
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
}
