package com.yanzhi.record.utils;

/**
 * Created by Administrator on 2017/8/7.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kyleduo.switchbutton.SwitchButton;
import com.yanzhi.record.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditCaptionTab extends LinearLayout {
    private CustomViewPager mPager;
    private TabLayout mTabLayout;
    private Context mContext;
    private View mFontPropertyView;
    private View mFontColorView;
    private View mFontStrokeView;
    private View mFontFaceView;
    private FontColorGridViewAdapter  mGridViewAdapter;
    private FontStrokeGridViewAdapter mFontStrokeGridViewAdapter;
    private RecyclerViewAdapter mFontFaceRecyclerViewAdapter;
    private RecyclerViewAdapter mFontPropertyRecyclerViewAdapter;
    private ColorPickerCtl mFontColorPickerCtl;
    private ColorPickerCtl mFontStrokeColorPickerCtl;
    private LinearLayout mSelectedColorLayout;
    private LinearLayout mFontStrokeSelectedColorLayout;
    private LinearLayout mTransparencySeekBar;
    private LinearLayout mFontStrokeTransparencySeekBarLayout;
    private SeekBar mFontColorAlphaSeekBar;
    private SeekBar mFontStrokeAlphaSeekBar;
    private int mCurFontColorAlpha = 100;
    private int mCurFontStrokeAlpha = 100;


    private int mFontColorGridViewSelectedPos = 0;
    private int mFontStrikeGridViewSelectedPos = 0;

    private String[] fontColors = {"#ffffff", "#000000", "#999999", "#703800", "#f3382c", "#ff852b"
            , "#ffbf00", "#fff52f", "#95ed31", "#2cc542", "#3fddc1", "#436cff", "#7319c9", "#c11cb4", "#ff4fad"};

    private String[] fontStrokeColors = {"#00ffffff", "#ffffff", "#000000", "#999999", "#703800", "#f3382c", "#ff852b"
            , "#ffbf00", "#fff52f", "#95ed31", "#2cc542", "#3fddc1", "#436cff", "#7319c9", "#c11cb4", "#ff4fad"};

    private static final String TAG = "EditCaptionTab";

    private OnEditCaptionListener mListener;



    public EditCaptionTab(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.edit_caption_tab, this);
        InitView();
        InitViewPager();
        InitTabLayout();
    }

    public void setEditCaptionListener(OnEditCaptionListener listener){
        mListener = listener;
    }

    public void resetState(){
        mFontColorGridViewSelectedPos = 0;
        mFontStrikeGridViewSelectedPos = 0;
        mGridViewAdapter.notifyDataSetChanged();
        mFontStrokeGridViewAdapter.notifyDataSetChanged();
        mFontFaceRecyclerViewAdapter.resetState();
        mFontPropertyRecyclerViewAdapter.resetState();
    }

    private void InitView(){
        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
        mPager = (CustomViewPager) findViewById(R.id.vPager);
    }

    private void InitTabLayout(){
        mTabLayout.setupWithViewPager(mPager);
        mTabLayout.getTabAt(0).setText("属性");
        mTabLayout.getTabAt(1).setText("颜色");
        mTabLayout.getTabAt(2).setText("描边");
        mTabLayout.getTabAt(3).setText("字体");
    }

    private void InitViewPager(){
        // view pager
        LayoutInflater mInflater = LayoutInflater.from(mContext);

        mFontPropertyView = mInflater.inflate(R.layout.font_property, null);
        mFontColorView = mInflater.inflate(R.layout.font_color, null);
        mFontStrokeView = mInflater.inflate(R.layout.font_stroke, null);
        mFontFaceView = mInflater.inflate(R.layout.font_face, null);

        List<View> viewContainer = new ArrayList<>();
        viewContainer.add(mFontPropertyView);
        viewContainer.add(mFontColorView);
        viewContainer.add(mFontStrokeView);
        viewContainer.add(mFontFaceView);

        mPager.setAdapter(new ViewPagerAdapter(viewContainer));
        mPager.setCurrentItem(0);
        mPager.setPagingEnabled(false);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                
            }

            @Override
            public void onPageSelected(int position) {
                mListener.OnPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // 属性
        InitPropertyTabRecyclerView();
        InitPropertyTabSwitchBtn();

        // 颜色
        InitFontColorPickerCtl();
        InitColorTabGridView();
        InitColorTabSeekBar();


        // 描边
        InitFontStrokeColorPickerCtl();
        InitStrokeTabGridView();
        InitStrokeTabSeekBar();

        // 字体
        InitFontFaceTabRecyclerView();
        InitFontFaceTabSeekBar();
    }

    private void InitPropertyTabRecyclerView(){
        RecyclerView fontPropertyRecyclerView = (RecyclerView) mFontPropertyView.findViewById(R.id.font_property_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fontPropertyRecyclerView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        fontPropertyRecyclerView.setLayoutManager(layoutManager);
        mFontPropertyRecyclerViewAdapter = new RecyclerViewAdapter(getFontStyles());
        fontPropertyRecyclerView.setAdapter(mFontPropertyRecyclerViewAdapter);
        fontPropertyRecyclerView.addItemDecoration(new SpaceItemDecoration(2));
        mFontPropertyRecyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(View view , int position){
                // 设置字体样式
                mListener.setFontStyle(position);
            }
        });
    }

    public void SetFontPropertySwitchBtn(boolean isBlod, boolean isItalic, boolean isShadow){
        SwitchButton font_blod = (SwitchButton) mFontPropertyView.findViewById(R.id.font_bold);
        SwitchButton font_italic = (SwitchButton) mFontPropertyView.findViewById(R.id.font_italic);
        SwitchButton font_shadow = (SwitchButton) mFontPropertyView.findViewById(R.id.font_shadow);
        font_blod.setChecked(isBlod);
        font_italic.setChecked(isItalic);
        font_shadow.setChecked(isShadow);
    }

    public void InitFontFaceSeekBar(int progress){
        SeekBar seekBar = (SeekBar) mFontFaceView.findViewById(R.id.font_size_seekBar);
        seekBar.setProgress(progress);
    }

    private void InitFontColorPickerCtl(){
        mFontColorAlphaSeekBar = (SeekBar) mFontColorView.findViewById(R.id.transparency_seekBar);
        final View customSelectedColorView = mFontColorView.findViewById(R.id.custom_selected_color_view);
        mFontColorPickerCtl = (ColorPickerCtl) mFontColorView.findViewById(R.id.font_color_pickerctl);
        mFontColorPickerCtl.SetSelectedColorChangedListener(new ColorPickerCtl.OnColorStateListener() {
            @Override
            public void selectedColor(int color) {
                mListener.setFontColor(color);


                mSelectedColorLayout.setVisibility(VISIBLE);
                mFontColorPickerCtl.setVisibility(GONE);
                mTransparencySeekBar.setVisibility(VISIBLE);
                customSelectedColorView.setBackgroundColor(color);
                mFontColorAlphaSeekBar.setProgress(mCurFontColorAlpha);
            }

            @Override
            public void selectedAlpha(float alpha) {
                mCurFontColorAlpha = (int)(alpha*100);
            }
        });
    }

    private void InitFontStrokeColorPickerCtl(){
        mFontStrokeAlphaSeekBar = (SeekBar) mFontStrokeView.findViewById(R.id.stroke_transparency_seekBar);
        final View customSelectedColorView = mFontStrokeView.findViewById(R.id.custom_selected_color_view);
        mFontStrokeColorPickerCtl = (ColorPickerCtl) mFontStrokeView.findViewById(R.id.font_color_pickerctl);
        mFontStrokeColorPickerCtl.SetSelectedColorChangedListener(new ColorPickerCtl.OnColorStateListener() {
            @Override
            public void selectedColor(int color) {

                // 设置描边颜色
                mListener.setFontStrokeColor(color);

                mFontStrokeSelectedColorLayout.setVisibility(VISIBLE);
                mFontStrokeColorPickerCtl.setVisibility(GONE);
                mFontStrokeTransparencySeekBarLayout.setVisibility(VISIBLE);
                customSelectedColorView.setBackgroundColor(color);
                mFontStrokeAlphaSeekBar.setProgress(mCurFontStrokeAlpha);
            }

            @Override
            public void selectedAlpha(float alpha) {
                mCurFontStrokeAlpha = (int) (alpha*100);
            }
        });
    }


    public void InitFontStrokeWidth(int width){
        SeekBar seekBar = (SeekBar) mFontStrokeView.findViewById(R.id.width_seekBar);
        seekBar.setProgress(width);
    }

    private void InitPropertyTabSwitchBtn(){
        SwitchButton font_blod = (SwitchButton) mFontPropertyView.findViewById(R.id.font_bold);
        font_blod.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // 设置是否粗体
                mListener.setFontBlod(isChecked);
            }
        });

        SwitchButton font_italic = (SwitchButton) mFontPropertyView.findViewById(R.id.font_italic);
        font_italic.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // 设置是否斜体
                mListener.setFontItalic(isChecked);
            }
        });

        SwitchButton font_shadow = (SwitchButton) mFontPropertyView.findViewById(R.id.font_shadow);
        font_shadow.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // 设置是否有阴影
                mListener.setFontShadow(isChecked);
            }
        });
    }

    private void InitColorTabGridView(){

        mSelectedColorLayout = (LinearLayout) mFontColorView.findViewById(R.id.select_color_layout);
        Button customColorBtn = (Button) mFontColorView.findViewById(R.id.custom_color_btn);
        customColorBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedColorLayout.setVisibility(View.GONE);
                mFontColorPickerCtl.setVisibility(View.VISIBLE);
                mTransparencySeekBar.setVisibility(GONE);
            }
        });

        final GridView gridView = (GridView) mFontColorView.findViewById(R.id.font_color_grid_view);
        mGridViewAdapter = new FontColorGridViewAdapter();
        gridView.setAdapter(mGridViewAdapter);
        final SeekBar seekBar = (SeekBar) mFontColorView.findViewById(R.id.transparency_seekBar);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mFontColorGridViewSelectedPos = i;
                mGridViewAdapter.notifyDataSetChanged();
                //设置字体的颜色
                mListener.setFontColor(Color.parseColor(fontColors[i]));
                seekBar.setProgress(100);
            }
        });
    }

    private void InitColorTabSeekBar(){
        mTransparencySeekBar = (LinearLayout) mFontColorView.findViewById(R.id.transparency_seekBar_layout);
        final TextView curValueText = (TextView) mFontColorView.findViewById(R.id.cur_value_text);
        mFontColorAlphaSeekBar.setMax(100);
        mFontColorAlphaSeekBar.setProgress(100);
        curValueText.setText("100%");
        mFontColorAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                curValueText.setText(i + "%");

                // 设置透明度
                mListener.setFontAlpha(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void InitStrokeTabGridView(){
        mFontStrokeSelectedColorLayout = (LinearLayout) mFontStrokeView.findViewById(R.id.font_stroke_color_layout);
        GridView gridView = (GridView) mFontStrokeView.findViewById(R.id.font_stroke_grid_view);
        mFontStrokeGridViewAdapter = new FontStrokeGridViewAdapter();
        gridView.setAdapter(mFontStrokeGridViewAdapter);

        final SeekBar transparencySeekBar = (SeekBar) mFontStrokeView.findViewById(R.id.stroke_transparency_seekBar);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mFontStrikeGridViewSelectedPos = i;
                mFontStrokeGridViewAdapter.notifyDataSetChanged();

                // 设置描边颜色
                mListener.setFontStrokeColor(Color.parseColor(fontStrokeColors[i]));
                transparencySeekBar.setProgress(100);
            }
        });

        Button customBtn = (Button) mFontStrokeView.findViewById(R.id.custom_color_btn);
        customBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mFontStrokeSelectedColorLayout.setVisibility(View.GONE);
                mFontStrokeColorPickerCtl.setVisibility(View.VISIBLE);
                mFontStrokeTransparencySeekBarLayout.setVisibility(GONE);
            }
        });
    }

    private void InitStrokeTabSeekBar(){
        mFontStrokeTransparencySeekBarLayout = (LinearLayout) mFontStrokeView.findViewById(R.id.transparency_seekBar_layout);

        mFontStrokeAlphaSeekBar.setMax(100);
        mFontStrokeAlphaSeekBar.setProgress(100);
        final TextView stransparentcyTextView = (TextView) mFontStrokeView.findViewById(R.id.cur_transparency_value_text);
        stransparentcyTextView.setText("100%");
        mFontStrokeAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                stransparentcyTextView.setText(i + "%");

                // 设置描边透明度
                mListener.setFontStrokeAlpha(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar widthSeekBar = (SeekBar) mFontStrokeView.findViewById(R.id.width_seekBar);
        widthSeekBar.setMax(100);
        final TextView widthTextView = (TextView) mFontStrokeView.findViewById(R.id.cur_width_value_text);
        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                widthTextView.setText(i + "%");
                // 设置描边宽度
                mListener.setFontStrokeWidth(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void InitFontFaceTabRecyclerView(){
        RecyclerView fontFaceRecyclerView = (RecyclerView) mFontFaceView.findViewById(R.id.font_face_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fontFaceRecyclerView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        fontFaceRecyclerView.setLayoutManager(layoutManager);
        mFontFaceRecyclerViewAdapter = new RecyclerViewAdapter(getFontFaces());
        fontFaceRecyclerView.setAdapter(mFontFaceRecyclerViewAdapter);
        fontFaceRecyclerView.addItemDecoration(new SpaceItemDecoration(2));
        mFontFaceRecyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(View view , int position){
                // 设置字体
                mListener.setFontface(position);
            }
        });
    }

    private void InitFontFaceTabSeekBar(){
        SeekBar fontSizeSeekBar = (SeekBar) mFontFaceView.findViewById(R.id.font_size_seekBar);
        fontSizeSeekBar.setMax(100);
        final TextView fontSizeTextView = (TextView) mFontFaceView.findViewById(R.id.cur_font_size_text);
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fontSizeTextView.setText(i + "");

                // 设置字体大小
                mListener.setFontSize(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private class FontStrokeGridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return fontStrokeColors.length;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View item;
            if(convertView == null){
                holder = new ViewHolder();
                item = LayoutInflater.from(mContext).inflate(R.layout.font_stroke_grid_view_item, parent, false);
                holder.bg = (View) item.findViewById(R.id.grid_view_strike_item_bg);
                holder.centerItem = (View) item.findViewById(R.id.grid_view_strike_item);
                holder.imageView = (ImageView) item.findViewById(R.id.grid_view_strike_image_view);
                item.setTag(holder);
            }else{
                item = (View) convertView;
                holder = (ViewHolder) item.getTag();
            }
            holder.bg.setBackgroundColor(Color.parseColor(fontStrokeColors[position]));
            if(position == 0){
                holder.imageView.setImageResource(R.mipmap.edit_contour);
            }

            if(mFontStrikeGridViewSelectedPos == position){
                item.setBackgroundColor(Color.parseColor("#000000"));
                if(position == 0){
                    holder.imageView.setImageResource(R.mipmap.edit_contour_check);
                }
            }else{
                item.setBackgroundColor(Color.parseColor("#ffffff"));
                if(position == 1){
                    holder.centerItem.setBackgroundColor(Color.parseColor("#d6d6d6"));
                    item.setBackgroundColor(Color.parseColor("#d6d6d6"));
                }
            }
            return item;
        }

        class ViewHolder{
            View bg;
            View centerItem;
            ImageView imageView;
        }
    }

    private class FontColorGridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return fontColors.length;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View item;
            if(convertView == null){
                holder = new ViewHolder();
                item = LayoutInflater.from(mContext).inflate(R.layout.font_color_grid_view_item, parent, false);
                holder.bg = (View) item.findViewById(R.id.grid_view_color_item_bg);
                holder.item = (View) item.findViewById(R.id.grid_view_color_item);
                item.setTag(holder);
            }else{
                item = (View) convertView;
                holder = (ViewHolder) item.getTag();
            }

            holder.item.setBackgroundColor(Color.parseColor(fontColors[position]));
            if(mFontColorGridViewSelectedPos == position){
                holder.bg.setBackgroundColor(Color.parseColor("#000000"));
            }else{
                if(position == 0){
                    holder.bg.setBackgroundColor(Color.parseColor("#00ffffff"));
                }else{
                    holder.bg.setBackgroundColor(Color.parseColor("#ffffff"));
                }

            }

            return item;
        }

        class ViewHolder{
            View bg;
            View item;
        }
    }

    private class ViewPagerAdapter extends PagerAdapter {

        public List<View> mListViews;

        public ViewPagerAdapter(List<View> mListViews){
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager)container).removeView(mListViews.get(position));
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ((ViewPager)container).addView(mListViews.get(position));
            return mListViews.get(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> implements View.OnClickListener{
        private Map<Integer, FontStyle> mFontStyles = null;
        private OnItemClickListener mOnItemClickListener = null;
        private int selected_position = 0;

        public void resetState(){
            selected_position = 0;
            notifyDataSetChanged();
        }

        public RecyclerViewAdapter(Map<Integer, FontStyle> data) {
            mFontStyles = data;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.font_property_recyclerview_item, parent, false);
            v.setOnClickListener(this);
            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            holder.mTextView.setText(mFontStyles.get(position).mStyleName);
            holder.mImageView.setImageResource(mFontStyles.get(position).mStyleResourceId);
            holder.itemView.setTag(position);

            if(selected_position == position){
                holder.itemView.setBackgroundColor(Color.parseColor("#5db5ff"));
                holder.mTextView.setTextColor(Color.parseColor("#ffffff"));
            }else{
                holder.itemView.setBackgroundColor(Color.parseColor("#d6d6d6"));
                holder.mTextView.setTextColor(Color.parseColor("#535353"));
            }

        }

        @Override
        public int getItemCount() {
            return mFontStyles == null ? 0 :  mFontStyles.size();
        }

        @Override
        public void onClick(View view) {

            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view,(int)view.getTag());

            }
            notifyItemChanged(selected_position);
            selected_position = (int)view.getTag();
            notifyItemChanged(selected_position);
        }

        public interface OnItemClickListener{
            void onItemClick(View view, int pos);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        public  class ViewHolder extends RecyclerView.ViewHolder{
            TextView mTextView;
            ImageView mImageView;

            public ViewHolder(View itemView){
                super(itemView);
                mImageView = (ImageView) itemView.findViewById(R.id.font_property_image_view);
                mTextView = (TextView) itemView.findViewById(R.id.font_property_text_view);
            }
        }
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration{
        private int mSpace;

        public SpaceItemDecoration(int space){
            mSpace = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);

            outRect.left = mSpace;
            outRect.right = mSpace;
            outRect.bottom = mSpace;
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = mSpace;
            }
        }

    }

    private Map<Integer, FontStyle> getFontStyles(){
        Map<Integer, FontStyle> map = new HashMap<>();

        FontStyle fs = new FontStyle();
        fs.mStyleName = "默认";
        fs.mStyleResourceId = R.mipmap.edit_default;
        map.put(0,fs);
        FontStyle fs1 = new FontStyle();
        fs1.mStyleName = "行摄之旅";
        fs1.mStyleResourceId = R.mipmap.caption_1;
        map.put(1,fs1);
        FontStyle fs2 = new FontStyle();
        fs2.mStyleName = "火焰喷射";
        fs2.mStyleResourceId = R.mipmap.caption_2;
        map.put(2,fs2);
        FontStyle fs3 = new FontStyle();
        fs3.mStyleName="逐字掉落";
        fs3.mStyleResourceId=R.mipmap.caption_3;
        map.put(3,fs3);
        return map;
    }

    private Map<Integer, FontStyle> getFontFaces(){
        Map<Integer, FontStyle> map = new HashMap<>();

        FontStyle fs = new FontStyle();
        fs.mStyleName = "默认";
        fs.mStyleResourceId = R.mipmap.edit_default;
        map.put(0,fs);
        FontStyle fs1 = new FontStyle();
        fs1.mStyleName = "楷体";
        fs1.mStyleResourceId = R.mipmap.font1;
        map.put(1,fs1);
        return map;
    }

    private class FontStyle{
        public String mStyleName;
        public Integer mStyleResourceId;
    }

    public interface OnEditCaptionListener{
        void setFontBlod(boolean isBlod);
        void setFontItalic(boolean isItalic);
        void setFontShadow(boolean isShadow);
        void setFontStyle(int pos);
        void setFontColor(int color);
        void setFontAlpha(int value);
        void setFontStrokeColor(int color);
        void setFontStrokeWidth(int width);
        void setFontStrokeAlpha(int alpha);
        void setFontface(int pos);
        void setFontSize(int size);
        void OnPageSelected(int position);
    }

}
