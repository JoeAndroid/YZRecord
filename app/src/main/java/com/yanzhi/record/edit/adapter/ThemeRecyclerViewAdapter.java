package com.yanzhi.record.edit.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yanzhi.record.R;
import com.yanzhi.record.edit.FxManageActivity;

import java.util.ArrayList;

/**
 * Created by admin on 2017/11/6.
 */

public class ThemeRecyclerViewAdapter extends RecyclerView.Adapter<ThemeRecyclerViewAdapter.ViewHolder>  {
    private ArrayList<FxManageActivity.FxInfoDescription> m_fxInfolist;
    private Context m_mContext;
    private OnItemClickListener m_onItemClickListener = null;
    public int selectPos = -1;

    public ThemeRecyclerViewAdapter(Context context, ArrayList<FxManageActivity.FxInfoDescription> fxInfoList) {
        m_mContext = context;
        m_fxInfolist = fxInfoList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(m_mContext).inflate(R.layout.item_theme, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.mTextImage.setImageBitmap(m_fxInfolist.get(position).mFxImage);
        holder.mTextName.setText(m_fxInfolist.get(position).mFxName);
        if(selectPos == position){
            holder.mImageBorder.setVisibility(View.VISIBLE);
            holder.mTextName.setSelected(true);
        }else {
            holder.mImageBorder.setVisibility(View.INVISIBLE);
            holder.mTextName.setSelected(false);
        }

        if(m_onItemClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(selectPos == position)
                        return;
                    selectPos = position;
                    notifyDataSetChanged();
                    m_onItemClickListener.onItemClick(v,position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return m_fxInfolist.size();
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.m_onItemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mTextImage;
        TextView mTextName;
        LinearLayout mImageBorder;
        public ViewHolder(View itemView) {
            super(itemView);
            mImageBorder = (LinearLayout) itemView.findViewById(R.id.themeImage_Border);
            mTextImage = (ImageView) itemView.findViewById(R.id.theme_Image);
            mTextName = (TextView) itemView.findViewById(R.id.theme_Name);

        }
    }
}
