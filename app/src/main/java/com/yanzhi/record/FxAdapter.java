package com.yanzhi.record;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by liuluwei on 2017/9/19.
 */

class FxAdapter extends RecyclerView.Adapter {
    private Context m_context;
    private List<FxItem> m_fxItemList;

    private OnItemClickListener onItemClickListener;

    public FxAdapter(Context context, List<FxItem> fxItemList) {
        m_context = context;
        m_fxItemList = fxItemList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(m_context).inflate(R.layout.activity_item_fx, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        FxItem fxItem = m_fxItemList.get(position);
        viewHolder.fxThumb.setImageResource(fxItem.getFxResId());
        viewHolder.fxName.setText(fxItem.getFxName());
        viewHolder.itemView.setTag(position);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ps = Integer.valueOf(v.getTag().toString());
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(v, ps);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return m_fxItemList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView fxThumb;
        private TextView fxName;

        public ViewHolder(View itemView) {
            super(itemView);
            fxThumb = itemView.findViewById(R.id.fxThumb);
            fxName = itemView.findViewById(R.id.fxName);
        }
    }


    public interface OnItemClickListener {
        void onItemClick(View v, int pos);
    }
}
