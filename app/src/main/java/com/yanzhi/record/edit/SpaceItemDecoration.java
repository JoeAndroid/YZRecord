package com.yanzhi.record.edit;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by admin on 2017-10-26.
 */

public class SpaceItemDecoration extends RecyclerView.ItemDecoration{
    private int topSpace;
    private int bottomSpace;

    public SpaceItemDecoration(int top, int bottom) {
        this.topSpace = top;
        this.bottomSpace = bottom;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top = topSpace;
        outRect.bottom = bottomSpace;
    }
}