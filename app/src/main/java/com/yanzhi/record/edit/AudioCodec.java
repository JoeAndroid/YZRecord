package com.yanzhi.record.edit;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AudioCodec {
    private static final String TAG = "AudioCodec";
    private String encodeType;
    private String srcPath;
    private String dstPath;
    private MediaCodec mediaDecode;
    private MediaExtractor mediaExtractor;
    private ByteBuffer[] decodeInputBuffers;
    private ByteBuffer[] decodeOutputBuffers;
    private MediaCodec.BufferInfo decodeBufferInfo;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private ArrayList<byte[]> chunkPCMDataContainer;//PCM数据块容器
    private OnCompleteListener onCompleteListener;
    private OnProgressListener onProgressListener;
    private static AudioCodec mAudioCodec;

    public static AudioCodec getInstance() {
        if (mAudioCodec == null) {
            mAudioCodec = new AudioCodec();
        }
        return mAudioCodec;  //单例模式
    }

    /**
     * 设置编码器类型
     *
     * @param encodeType
     */
    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }

    /**
     * 设置输入输出文件位置
     *
     * @param srcPath
     * @param dstPath
     */
    public AudioCodec setIOPath(String srcPath, String dstPath) {
        this.srcPath = srcPath;
        this.dstPath = dstPath;
        return this;
    }

    /**
     * 此类已经过封装
     * 调用prepare方法 会初始化Decode 、Encode 、输入输出流 等一些列操作
     */
    public AudioCodec prepare() {

//        if (encodeType == null) {
//            throw new IllegalArgumentException("encodeType can't be null");
//        }

        if (srcPath == null) {
            throw new IllegalArgumentException("srcPath can't be null");
        }

        if (dstPath == null) {
            throw new IllegalArgumentException("dstPath can't be null");
        }

        try {
            fos = new FileOutputStream(new File(dstPath));
            bos = new BufferedOutputStream(fos, 200 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        chunkPCMDataContainer = new ArrayList<>();
        initMediaDecode();//解码器
        return this;
    }

    /**
     * 初始化解码器
     */
    private void initMediaDecode() {
        try {
            mediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mediaExtractor.setDataSource(srcPath);//媒体文件的位置
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);
                format.setInteger(MediaFormat.KEY_BIT_RATE, AudioFormat.ENCODING_PCM_16BIT);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mediaExtractor.selectTrack(i);//选择此音频轨道
                    mediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mediaDecode.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaDecode == null) {
            Log.e(TAG, "create mediaDecode failed");
            return;
        }
        mediaDecode.start();//启动MediaCodec ，等待传入数据
        decodeInputBuffers = mediaDecode.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        decodeOutputBuffers = mediaDecode.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        decodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
        showLog("buffers:" + decodeInputBuffers.length);
    }

    /**
     * 初始化MPEG编码器
     */
    private void initMPEGMediaEncode() {

    }

    private boolean codeOver = false;

    /**
     * 开始转码
     * 音频数据{@link #srcPath}先解码成PCM  PCM数据在编码成想要得到的{@link #encodeType}音频格式
     * mp3->PCM->aac
     */
    public void startAsync() {
        showLog("start");
        new Thread(new DecodeRunnable()).start();

    }

    public void startDecode() {
        new Thread(new DecodeRunnable()).start();
    }

    /**
     * 将PCM数据存入{@link #chunkPCMDataContainer}
     *
     * @param pcmChunk PCM数据块
     */
    private void putPCMData(byte[] pcmChunk) {
        synchronized (AudioCodec.class) {//记得加锁
            chunkPCMDataContainer.add(pcmChunk);
        }
    }

    /**
     * 在Container中{@link #chunkPCMDataContainer}取出PCM数据
     *
     * @return PCM数据块
     */
    private byte[] getPCMData() {
        synchronized (AudioCodec.class) {//记得加锁
            showLog("getPCM:" + chunkPCMDataContainer.size());
            if (chunkPCMDataContainer.isEmpty()) {
                return null;
            }

            byte[] pcmChunk = chunkPCMDataContainer.get(0);//每次取出index 0 的数据
            chunkPCMDataContainer.remove(pcmChunk);//取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
            return pcmChunk;
        }
    }


    /**
     * 解码{@link #srcPath}音频文件 得到PCM数据块
     *
     * @return 是否解码完所有数据
     */
    private void srcAudioFormatToPCM() {
        for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
            int inputIndex = mediaDecode.dequeueInputBuffer(-1);//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
            if (inputIndex < 0) {
                codeOver = true;
                return;
            }

            ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];//拿到inputBuffer
            inputBuffer.clear();//清空之前传入inputBuffer内的数据
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中
            if (sampleSize < 0) {//小于0 代表所有数据已读取完成
                codeOver = true;
            } else {
                mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);//通知MediaDecode解码刚刚传入的数据
                mediaExtractor.advance();//MediaExtractor移动到下一取样处
            }
        }

        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);

//        showLog("decodeOutIndex:" + outputIndex);
        ByteBuffer outputBuffer;
        byte[] chunkPCM;
        while (outputIndex >= 0) {//每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
            outputBuffer = decodeOutputBuffers[outputIndex];//拿到用于存放PCM数据的Buffer
            chunkPCM = new byte[decodeBufferInfo.size];//BufferInfo内定义了此数据块的大小
            outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
            outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
            putPCMData(chunkPCM);//自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
            mediaDecode.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);//再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
        }

    }

    /**
     * android 5.0以上
     */
    private void srcAudioFormatToPCMHigherApi() {
        if (Build.VERSION.SDK_INT >= 21) {
            boolean sawOutputEOS = false;
            final long kTimeOutUs = 10000;
            long presentationTimeUs = 0;
            while (!sawOutputEOS) {
                try {
                    int inputIndex = mediaDecode.dequeueInputBuffer(-1);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = mediaDecode.getInputBuffer(inputIndex);
                        if (inputBuffer != null) {
                            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                            if (sampleSize < 0) {// 小于0 代表所有数据已读取完成
                                sawOutputEOS = true;
                                codeOver = true;
                                break;
                            } else {
                                presentationTimeUs = mediaExtractor.getSampleTime();
                                mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);// 通知MediaDecode解码刚刚传入的数据
                                mediaExtractor.advance();
                            }
                        }
                    } else {
                        sawOutputEOS = true;
                        codeOver = true;
                    }
                    int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, kTimeOutUs);
                    ByteBuffer outputBuffer;//= mediaDecode.getOutputBuffer(outputIndex);// 拿到用于存放PCM数据的Buffer
                    while (outputIndex >= 0) {
                        outputBuffer = mediaDecode.getOutputBuffer(outputIndex);
                        boolean doRender = (decodeBufferInfo.size != 0);
                        if (doRender && outputBuffer != null) {
                            outputBuffer.position(decodeBufferInfo.offset);
                            outputBuffer.limit(decodeBufferInfo.offset + decodeBufferInfo.size);
                            byte[] chunkPCM = new byte[decodeBufferInfo.size];// BufferInfo内定义了此数据块的大小
                            outputBuffer.get(chunkPCM);
                            outputBuffer.clear();// 数据取出后一定记得清空此Buffer   MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
                            putPCMData(chunkPCM);// 自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
                            try {
                                bos.write(chunkPCM, 0, chunkPCM.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mediaDecode.releaseOutputBuffer(outputIndex, false);// 此操作一定要做，不然MediaCodec用完所有的Buffer后将不能向外输出数据
                            outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, kTimeOutUs);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sawOutputEOS = true;
                    codeOver = true;
                }
            }
            if (codeOver) {
                if (onCompleteListener != null) {
                    onCompleteListener.completed(chunkPCMDataContainer);
                }
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        try {
            if (bos != null) {
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bos = null;
                }
            }
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos = null;
        }

        if (mediaDecode != null) {
            mediaDecode.stop();
            mediaDecode.release();
            mediaDecode = null;
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }

        if (onCompleteListener != null) {
            onCompleteListener = null;
        }

        if (onProgressListener != null) {
            onProgressListener = null;
        }
        showLog("release");
    }

    /**
     * 解码线程
     */
    private class DecodeRunnable implements Runnable {

        @Override
        public void run() {
            while (!codeOver) {
                if (Build.VERSION.SDK_INT >= 21) {
                    srcAudioFormatToPCMHigherApi();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    srcAudioFormatToPCM();
                }
            }
        }
    }

    /**
     * 转码完成回调接口
     */
    public interface OnCompleteListener {
        void completed(ArrayList<byte[]> chunkPCMDataContainer);
    }

    /**
     * 转码进度监听器
     */
    public interface OnProgressListener {
        void progress();
    }

    /**
     * 设置转码完成监听器
     *
     * @param onCompleteListener
     */
    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private void showLog(String msg) {
        Log.e("AudioCodec", msg);
    }
}
