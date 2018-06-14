package com.yanzhi.record.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yanzhi.record.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/8/9.
 */

public class EditStickerCtl extends LinearLayout {

    private static final String TAG = "EditStickerCtl";

    private OnEditStickerListener mListener;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    public EditStickerCtl(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.edit_sticker_ctl,this);

        InitRecyclerView();

        InitBtnAndSeekBar();

    }

    public void setCurStickerSize(int size){
        SeekBar seekBar = (SeekBar) findViewById(R.id.sticker_size_seekBar);
        seekBar.setProgress(size);
    }

    public void resetState(){
        mRecyclerViewAdapter.resetState();
    }

    public void setEditStickerListener(OnEditStickerListener listener){
        mListener = listener;
    }

    private void InitRecyclerView(){
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.edit_sticker_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        mRecyclerViewAdapter = new RecyclerViewAdapter(getStickerStyles());
        recyclerView.setAdapter(mRecyclerViewAdapter);
        recyclerView.addItemDecoration(new SpaceItemDecoration(2));
        mRecyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(View view , int position){
                // 设置贴纸样式
                mListener.setStickerStyle(position);
            }
        });

    }

    private void InitBtnAndSeekBar(){
        Button horBtn = (Button) findViewById(R.id.horizontal_flip_btn);
        horBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // 水平翻转
                mListener.horizontalFlip();
            }
        });

        Button verBtn = (Button) findViewById(R.id.vertical_flip_btn);
        verBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // 垂直翻转
                mListener.verticalFlip();
            }
        });

        final TextView textView = (TextView)findViewById(R.id.sticker_size_cur_value_text);
        SeekBar seekBar = (SeekBar) findViewById(R.id.sticker_size_seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textView.setText(i + "%");
                // 设置贴纸大小
                mListener.setStickerSize(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> implements View.OnClickListener{
        private Map<Integer, StickerStyle> mStickerStyles = null;
        private OnItemClickListener mOnItemClickListener = null;
        private int selected_position = 0;

        public void resetState(){
            selected_position = 0;
            notifyDataSetChanged();
        }

        public RecyclerViewAdapter(Map<Integer, StickerStyle> data) {
            mStickerStyles = data;
        }

        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_sticker_recycler_view_item, parent, false);
            v.setOnClickListener(this);
            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            holder.mImageView.setImageResource(mStickerStyles.get(position).mResourceId);
            holder.itemView.setTag(position);

            if(selected_position == position){
                holder.itemView.setBackgroundColor(Color.parseColor("#5db5ff"));
            }else{
                holder.itemView.setBackgroundColor(Color.parseColor("#d6d6d6"));
            }

        }

        @Override
        public int getItemCount() {
            return  mStickerStyles.size();
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

        public void setOnItemClickListener(RecyclerViewAdapter.OnItemClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        public  class ViewHolder extends RecyclerView.ViewHolder{
            ImageView mImageView;

            public ViewHolder(View itemView){
                super(itemView);
                mImageView = (ImageView) itemView.findViewById(R.id.sticker_recycler_view_image_view);
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

    private Map<Integer, StickerStyle> getStickerStyles(){
        Map<Integer, StickerStyle> map = new HashMap<>();
        StickerStyle s = new StickerStyle();
        s.mResourceId = R.mipmap.sticker1;
        map.put(0, s);
        StickerStyle s1 = new StickerStyle();
        s1.mResourceId = R.mipmap.sticker2;
        map.put(1, s1);
        StickerStyle s2 = new StickerStyle();
        s2.mResourceId = R.mipmap.sticker3;
        map.put(2, s2);
        return map;
    }

    class StickerStyle{
        public int mResourceId;
    }

    public interface OnEditStickerListener{
        void setStickerStyle(int pos);
        void setStickerSize(int size);
        void horizontalFlip();
        void verticalFlip();
    }


}
