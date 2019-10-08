package com.aliyunos.filemanager.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import yunos.support.v4.view.ViewPager;

public class ScrollableViewPager extends ViewPager {
    private boolean mScrollable = true;
    public ScrollableViewPager(Context context) {
        super(context);
    }
    public ScrollableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollable(boolean scrollable) {
        mScrollable = scrollable;
    }
    public boolean getScrollable() { return mScrollable; }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mScrollable) {
            return super.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mScrollable) {
            super.scrollTo(x, y);
        }
    }
}
