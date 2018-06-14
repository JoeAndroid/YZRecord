package com.yanzhi.record;

/**
 * Created by liuluwei on 2017/9/19.
 */

public class FxItem {
    private String m_fxName;
    private int m_fxResId;

    public FxItem(String fxName, int fxResId) {
        m_fxName = fxName;
        m_fxResId = fxResId;
    }

    public String getFxName() {
        return m_fxName;
    }

    public int getFxResId() {
        return m_fxResId;
    }
}
