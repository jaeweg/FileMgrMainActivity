package com.aliyunos.filemanager.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class SearchButton extends SearchView {
    public SearchButton(Context context) {
        super(context, null);
    }

    public SearchButton(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SearchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }
}
