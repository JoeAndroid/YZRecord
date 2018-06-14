/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.yanzhi.record;

import android.content.Context;
import android.view.OrientationEventListener;

public class OrientationDetector extends OrientationEventListener {
    int Orientation;
    private OrientationChangedListener listener;

    public OrientationDetector(Context context) {
        super(context);
    }

    public void setOrientationChangedListener(OrientationChangedListener l) {
        listener = l;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        this.Orientation = orientation;
        if (listener != null) {
            listener.onOrientationChanged(orientation);
        }
    }

    public int getOrientation() {
        return Orientation;
    }

    public interface OrientationChangedListener {
        void onOrientationChanged(int orientation);
    }
}
