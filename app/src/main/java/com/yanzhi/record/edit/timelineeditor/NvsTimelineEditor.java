package com.yanzhi.record.edit.timelineeditor;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.meicam.sdk.NvsMultiThumbnailSequenceView;

import java.util.ArrayList;

public class NvsTimelineEditor extends RelativeLayout {
    private String TAG = "TimelineEditor";
    private static final float TIMEBASE = 1000000f;
    private double m_pixelPerMicrosecond = 0D;
    private Context m_context;
    private RelativeLayout m_timeSpanRelativeLayout;
    private LinearLayout m_sequenceLinearLayout;

    private OnScrollChangeListener m_scrollchangeListener;
    private ArrayList<NvsTimelineTimeSpan> m_timelineTimeSpanArray;
    private long m_timelineDuration = 0;
    private NvsMultiThumbnailSequenceView m_multiThumbnailSequenceView;
    private int m_padding = 0;

    public NvsTimelineEditor(Context context, AttributeSet attrs){
        super(context, attrs);
        m_context = context;
        m_timeSpanRelativeLayout = new RelativeLayout(context);
        m_sequenceLinearLayout = new LinearLayout(context);
        m_sequenceLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        m_multiThumbnailSequenceView = new NvsMultiThumbnailSequenceView(m_context);
        init();
    }
    //初始化TimelineEditor
    public void initTimelineEditor(ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> sequenceDesc, final long timelineDuration){
        int len = sequenceDesc.size();
        if(0 == len)
            return;

        //删除所有布局
        removeAllLayout();
        m_sequenceLinearLayout.scrollTo(0,0);
        m_multiThumbnailSequenceView.setThumbnailSequenceDescArray(sequenceDesc);
        m_multiThumbnailSequenceView.setPixelPerMicrosecond(m_pixelPerMicrosecond);
        m_multiThumbnailSequenceView.setStartPadding(m_padding);
        m_multiThumbnailSequenceView.setEndPadding(m_padding);
        m_multiThumbnailSequenceView.setBackgroundColor(Color.LTGRAY);

        m_timelineDuration = timelineDuration;
        RelativeLayout.LayoutParams sequenceViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        this.addView(m_multiThumbnailSequenceView,sequenceViewParams);
        m_multiThumbnailSequenceView.setOnScrollChangeListenser(new NvsMultiThumbnailSequenceView.OnScrollChangeListener(){

            @Override
            public void onScrollChanged(NvsMultiThumbnailSequenceView nvsMultiThumbnailSequenceView, int i, int i1) {
                if(m_scrollchangeListener != null){
                    long tmpTimeStamp = (long) Math.floor(i /  m_pixelPerMicrosecond + 0.5D);
                    m_scrollchangeListener.onScrollX(tmpTimeStamp);
                }

                m_sequenceLinearLayout.scrollTo(i,0);
                int timeSpanCount = m_timelineTimeSpanArray.size();
                for (int index = 0; index < timeSpanCount;++index) {
                    boolean add = true;
                    NvsTimelineTimeSpan timeSpan = m_timelineTimeSpanArray.get(index);
                    long inPoint = timeSpan.getInPoint();
                    long outPoint = timeSpan.getOutPoint();
                    if (outPoint <= m_multiThumbnailSequenceView.mapTimelinePosFromX(0))
                        add = false;
                    if (inPoint >= m_multiThumbnailSequenceView.mapTimelinePosFromX(m_multiThumbnailSequenceView.getWidth()))
                        add = false;
                    if (add) {
                        if(timeSpan.getParent() == null) {
                            m_timeSpanRelativeLayout.addView(timeSpan);
                        }
                        updateTimeSpanShadow(timeSpan);
                    } else {
                        if(timeSpan.getParent() != null) {
                            m_timeSpanRelativeLayout.removeView(timeSpan);
                        }
                    }
                }

                //判断当前timeSpan是否选中
                long cursorPosition = m_multiThumbnailSequenceView.mapTimelinePosFromX(m_padding);
                ArrayList<NvsTimelineTimeSpan> timeSpanArray = getTimeSpanArrayByCursorPos(cursorPosition);
                int selectCount = timeSpanArray.size();
                if(selectCount > 1){//当前位置有多个timeSpan不作处理，由用户自己选择处理
                    return;
                }
                if(selectCount == 1){//当前位置仅有一个timeSpan则选中
                    boolean isSelectTimeSpan = isSelectTimeSpanByCursorPos(timeSpanArray.get(0),cursorPosition);
                    if(isSelectTimeSpan) {
                        timeSpanArray.get(0).setHasSelected(true);
                    }
                    return;
                }
                //selectCount为0，对所有timeSpan不选中
                unSelectAllTimeSpan();
           }
        });
    }

    //初始化TimelineEditor
    public void initTimelineEditor(ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> sequenceDesc, final long timelineDuration,int paddingWidth){
        int len = sequenceDesc.size();
        if(0 == len)
            return;

        //删除所有布局
        removeAllLayout();
        m_sequenceLinearLayout.scrollTo(0,0);
        m_multiThumbnailSequenceView.setThumbnailSequenceDescArray(sequenceDesc);
        m_multiThumbnailSequenceView.setPixelPerMicrosecond(m_pixelPerMicrosecond);
        m_multiThumbnailSequenceView.setStartPadding(m_padding- paddingWidth);
        m_multiThumbnailSequenceView.setEndPadding(m_padding);
        m_multiThumbnailSequenceView.setBackgroundColor(Color.parseColor("#e7f5ff"));

        m_timelineDuration = timelineDuration;
        RelativeLayout.LayoutParams sequenceViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        this.addView(m_multiThumbnailSequenceView,sequenceViewParams);
        m_multiThumbnailSequenceView.setOnScrollChangeListenser(new NvsMultiThumbnailSequenceView.OnScrollChangeListener(){

            @Override
            public void onScrollChanged(NvsMultiThumbnailSequenceView nvsMultiThumbnailSequenceView, int i, int i1) {
                if(m_scrollchangeListener != null){
                    long tmpTimeStamp = (long)Math.floor(i /  m_pixelPerMicrosecond + 0.5D);
                    m_scrollchangeListener.onScrollX(tmpTimeStamp);
                }

                m_sequenceLinearLayout.scrollTo(i,0);
                int timeSpanCount = m_timelineTimeSpanArray.size();
                for (int index = 0; index < timeSpanCount;++index) {
                    boolean add = true;
                    NvsTimelineTimeSpan timeSpan = m_timelineTimeSpanArray.get(index);
                    long inPoint = timeSpan.getInPoint();
                    long outPoint = timeSpan.getOutPoint();
                    if (outPoint <= m_multiThumbnailSequenceView.mapTimelinePosFromX(0))
                        add = false;
                    if (inPoint >= m_multiThumbnailSequenceView.mapTimelinePosFromX(m_multiThumbnailSequenceView.getWidth()))
                        add = false;
                    if (add) {
                        if(timeSpan.getParent() == null) {
                            m_timeSpanRelativeLayout.addView(timeSpan);
                        }
                        updateTimeSpanShadow(timeSpan);
                    } else {
                        if(timeSpan.getParent() != null) {
                            m_timeSpanRelativeLayout.removeView(timeSpan);
                        }
                    }
                }

                //判断当前timeSpan是否选中
                long cursorPosition = m_multiThumbnailSequenceView.mapTimelinePosFromX(m_padding);
                ArrayList<NvsTimelineTimeSpan> timeSpanArray = getTimeSpanArrayByCursorPos(cursorPosition);
                int selectCount = timeSpanArray.size();
                if(selectCount > 1){//当前位置有多个timeSpan不作处理，由用户自己选择处理
                    return;
                }
                if(selectCount == 1){//当前位置仅有一个timeSpan则选中
                    boolean isSelectTimeSpan = isSelectTimeSpanByCursorPos(timeSpanArray.get(0),cursorPosition);
                    if(isSelectTimeSpan) {
                        timeSpanArray.get(0).setHasSelected(true);
                    }
                    return;
                }
                //selectCount为0，对所有timeSpan不选中
                unSelectAllTimeSpan();
            }
        });
    }


    public NvsTimelineTimeSpan addTimeSpan(long inPoint,long outPoint){
        if (inPoint >= outPoint)
            return null;
        if (inPoint < 0 || outPoint > m_timelineDuration)
            return null;

        NvsTimelineTimeSpan mTimelineTimeSpan = new NvsTimelineTimeSpan(m_context);
        mTimelineTimeSpan.setInPoint(inPoint);
        mTimelineTimeSpan.setOutPoint(outPoint);
        mTimelineTimeSpan.setPixelPerMicrosecond(m_pixelPerMicrosecond);
        int handleWidth = mTimelineTimeSpan.getHandleWidth();
        int maxWidth = getSequenceWidth() + 2 * handleWidth;
        mTimelineTimeSpan.setTotalWidth(maxWidth);
        //
        addTimeSpanLayout(handleWidth);
        //
        double xLeft = inPoint * m_pixelPerMicrosecond;
        double width = (outPoint - inPoint) * m_pixelPerMicrosecond;
        width += 2 * handleWidth;
        int spanWidth = (int) Math.floor(width + 0.5D);
        int leftMargin = (int) Math.floor(xLeft + 0.5D);
        RelativeLayout.LayoutParams spanItemParams = new RelativeLayout.LayoutParams(spanWidth, RelativeLayout.LayoutParams.MATCH_PARENT);
        spanItemParams.setMargins(leftMargin, RelativeLayout.LayoutParams.MATCH_PARENT,maxWidth - (leftMargin + spanWidth),0);
        mTimelineTimeSpan.setLayoutParams(spanItemParams);

        boolean add = true;
        if (outPoint <= m_multiThumbnailSequenceView.mapTimelinePosFromX(0)) {
            add = false;
        }
        if (inPoint >= m_multiThumbnailSequenceView.mapTimelinePosFromX(2 * m_padding)) {
            add = false;
        }

        if (add) {
            m_timeSpanRelativeLayout.addView(mTimelineTimeSpan);
            updateTimeSpanShadow(mTimelineTimeSpan);
        }
        m_timelineTimeSpanArray.add(mTimelineTimeSpan);
        long cursorPositon = m_multiThumbnailSequenceView.mapTimelinePosFromX(m_padding);
        ArrayList<NvsTimelineTimeSpan> timeSpanArray = getTimeSpanArrayByCursorPos(cursorPositon);
        int selectCount = timeSpanArray.size();
        if(selectCount > 1){
            for (int index = 0;index < selectCount;++index){
                if(timeSpanArray.get(index).isHasSelected()){
                    timeSpanArray.get(index).setHasSelected(false);
                    timeSpanArray.get(index).requestLayout();
                }
            }
        }
        return mTimelineTimeSpan;
    }
    //通过用户选择timeSpan选中
    public void selectTimeSpan(NvsTimelineTimeSpan timeSpan){
        unSelectAllTimeSpan();
        timeSpan.setHasSelected(true);
        timeSpan.requestLayout();
        timeSpan.bringToFront();
    }
    public int getSequenceWidth(){
        return (int) Math.floor(m_timelineDuration * m_pixelPerMicrosecond + 0.5D);
    }
    public NvsMultiThumbnailSequenceView getMultiThumbnailSequenceView(){
        return m_multiThumbnailSequenceView;
    }

    public int timeSpanCount(){
        return m_timeSpanRelativeLayout.getChildCount();
    }

    public void deleteSelectedTimeSpan(NvsTimelineTimeSpan timeSpan){
        int count = m_timeSpanRelativeLayout.getChildCount();
        if(count > 0) {
            m_timeSpanRelativeLayout.removeView(timeSpan);
            m_timelineTimeSpanArray.remove(timeSpan);
        }
    }
    public void deleteAllTimeSpan(){
        int countTimeSpan = m_timeSpanRelativeLayout.getChildCount();
        if(countTimeSpan > 0){
            m_timeSpanRelativeLayout.removeAllViews();
        }
        int arraySize = m_timelineTimeSpanArray.size();
        if(arraySize > 0){
            m_timelineTimeSpanArray.clear();
        }
    }

    private void updateTimeSpanShadow(NvsTimelineTimeSpan timeSpan){
        long inPoint = timeSpan.getInPoint();
        long outPoint = timeSpan.getOutPoint();
        long newIn = inPoint;
        long newOut = outPoint;
        long left = m_multiThumbnailSequenceView.mapTimelinePosFromX(0);
        if (inPoint < left){
            newIn = left;
        }

        long right = m_multiThumbnailSequenceView.mapTimelinePosFromX(2 * m_padding);
        if (outPoint > right) {
            newOut = right;
        }

        double xLeft = newIn * m_pixelPerMicrosecond;
        double width = (newOut - newIn) * m_pixelPerMicrosecond;
        int spanWidth = (int) Math.floor(width + 0.5D);
        int leftValue = (int) Math.floor(xLeft + 0.5D);
        int timeSpanShadowWidth = timeSpan.getLayoutParams().width - 2 * timeSpan.getHandleWidth();
        int subViewScrollValue = (int) Math.floor(inPoint * m_pixelPerMicrosecond + 0.5D);
        leftValue = leftValue - subViewScrollValue;
        RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams) timeSpan.getTimeSpanshadowView().getLayoutParams();
        relativeParams.width = spanWidth;
        relativeParams.setMargins(leftValue, RelativeLayout.LayoutParams.MATCH_PARENT,timeSpanShadowWidth - (leftValue + spanWidth),0);
        timeSpan.getTimeSpanshadowView().setLayoutParams(relativeParams);
    }

    private  void addTimeSpanLayout(int handleWidth){
        if(m_sequenceLinearLayout.getParent() != null){
            return;
        }
        // start padding
        addPadding(m_padding - handleWidth);
        int durationWidth = (int) Math.floor(m_timelineDuration * m_pixelPerMicrosecond + 0.5D) + 2 * handleWidth;
        LinearLayout.LayoutParams timeSpanRelativeParams = new LinearLayout.LayoutParams(durationWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        m_sequenceLinearLayout.addView(m_timeSpanRelativeLayout,timeSpanRelativeParams);
        RelativeLayout.LayoutParams itemParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        this.addView(m_sequenceLinearLayout,itemParams);
    }
    private ArrayList<NvsTimelineTimeSpan> getTimeSpanArrayByCursorPos(long cursorPos){
        ArrayList<NvsTimelineTimeSpan> timeSpanArray = new ArrayList<>();
        int countTimeSpan = m_timelineTimeSpanArray.size();
        for (int index = 0; index < countTimeSpan;++index){
            NvsTimelineTimeSpan timeSpan = m_timelineTimeSpanArray.get(index);
            boolean isSelcet = isSelectTimeSpanByCursorPos(timeSpan,cursorPos);
            if(isSelcet){
                timeSpanArray.add(timeSpan);
            }
        }
        return timeSpanArray;
    }
    private void unSelectAllTimeSpan(){
        for (int index = 0; index < m_timelineTimeSpanArray.size();++index){
            NvsTimelineTimeSpan timeSpan = m_timelineTimeSpanArray.get(index);
            if(timeSpan.isHasSelected()) {
                timeSpan.setHasSelected(false);
            }
        }
    }
    private boolean isSelectTimeSpanByCursorPos(NvsTimelineTimeSpan timeSpan,long cursorPos){
        if(timeSpan.getInPoint() <= cursorPos && cursorPos <= timeSpan.getOutPoint()){
            return true;
        }
        return false;
    }

    private void removeAllLayout(){
        deleteAllTimeSpan();
        int countLinearChild = m_sequenceLinearLayout.getChildCount();
        if(countLinearChild > 0){
            m_sequenceLinearLayout.removeAllViews();
        }
        int countEditorChild = this.getChildCount();
        if(countEditorChild > 0){
            this.removeAllViews();
        }
    }

    public interface OnScrollChangeListener{
         void onScrollX(long timeStamp);
    }
    public void  setOnScrollListener(OnScrollChangeListener listener){m_scrollchangeListener = listener;}


    private void init(){
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        m_pixelPerMicrosecond = screenWidth / 20 / TIMEBASE;
        m_padding = (int) Math.floor(screenWidth / 2 + 0.5D);
        m_timelineTimeSpanArray = new ArrayList<>();
    }
    private void addPadding(int padding){
        LinearLayout.LayoutParams paddinParams = new LinearLayout.LayoutParams(padding, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout paddingLayout = new LinearLayout(m_context);
        m_sequenceLinearLayout.addView(paddingLayout,paddinParams);
    }
}
