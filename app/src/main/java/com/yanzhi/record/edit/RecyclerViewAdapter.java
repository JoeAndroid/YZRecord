package com.yanzhi.record.edit;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.meicam.sdk.NvsThumbnailView;
import com.yanzhi.record.R;

import java.util.List;

/**
 * Created by admin on 2017/11/6.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>  {
    private List<TrimActivity.VideoInfoDescription> m_videoInfolist;
    private Context m_mContext;
    private OnItemLongPressListener m_onItemLongPressListener = null;
    private OnEditClickListener m_onEditClickListener = null;
    private OnSortClickListener m_onSortClickListener = null;
    private OnDeleteClickListener m_onDeleteClickListener = null;

    public RecyclerViewAdapter(Context context, List<TrimActivity.VideoInfoDescription> videoInfoList) {
        m_mContext = context;
        m_videoInfolist = videoInfoList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(m_mContext).inflate(R.layout.item_videoedit, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder( ViewHolder holder, int position) {
        holder.mVideoCoverView.setMediaFilePath(m_videoInfolist.get(position).videoFilePath);
        holder.mTextAssetNum.setText(m_videoInfolist.get(position).mVideoIndex);

        final ViewHolder curHolder = holder;
        final int curPos = position;
        if(m_onEditClickListener != null){
            holder.mVideoEditBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    m_onEditClickListener.onEditClick(v,curPos);
                }
            });
        }
        if(m_onDeleteClickListener != null){
            holder.mVideoDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m_onDeleteClickListener.onDeleteClick(v,curPos);
                }
            });
        }
        if(m_onItemLongPressListener != null){
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    m_onItemLongPressListener.onItemLongPress(curHolder,curPos);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return m_videoInfolist.size();
    }

    public void setOnItemLongPressListener( OnItemLongPressListener itemLongPressListener ) {
        this.m_onItemLongPressListener = itemLongPressListener;
    }
    public void setOnEditClickListener(OnEditClickListener editClickListener) {
        this.m_onEditClickListener = editClickListener;
    }

    public void setOnSortClickListener(OnSortClickListener sortClickListener) {
        this.m_onSortClickListener = sortClickListener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener deleteClickListener) {
        this.m_onDeleteClickListener = deleteClickListener;
    }

    public interface OnItemLongPressListener {
        void onItemLongPress(RecyclerViewAdapter.ViewHolder holder, int pos);
    }
    public interface OnEditClickListener {
        void onEditClick(View view, int pos);
    }

    public interface OnSortClickListener {
        void onSortClick(View view, int pos);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(View view, int pos);
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        NvsThumbnailView mVideoCoverView;
        TextView mTextAssetNum;
        ImageView mVideoEditBtn;
        ImageView mVideoSortBtn;
        ImageView mVideoDeleteBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            mVideoCoverView = (NvsThumbnailView) itemView.findViewById(R.id.videoCover);
            mTextAssetNum = (TextView) itemView.findViewById(R.id.assetNum);
            mVideoEditBtn = (ImageView) itemView.findViewById(R.id.videoEditBtn);
            mVideoSortBtn = (ImageView) itemView.findViewById(R.id.videoSorttn);
            mVideoDeleteBtn = (ImageView) itemView.findViewById(R.id.videoDeleteBtn);
        }
    }
}
