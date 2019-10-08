package com.aliyunos.filemanager.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class ScrollOverListView extends ListView {
    private static final String TAG = "ScrollOverListView";
    private int mLastY;
    private int mTopPosition;
    private boolean mLockDraw = false;

    public interface OnScrollOverListener {
        boolean onListViewTopAndPullDown(MotionEvent event, int delta);
        boolean onMotionDown(MotionEvent event);
        boolean onMotionMove(MotionEvent event, int delta);
        boolean onMotionUp(MotionEvent event);
    }

    private OnScrollOverListener mOnScrollOverListener = new OnScrollOverListener() {
        @Override
        public boolean onListViewTopAndPullDown(MotionEvent event, int delta) {
            return false;
        }

        @Override
        public boolean onMotionDown(MotionEvent event) {
            return false;
        }

        @Override
        public boolean onMotionMove(MotionEvent event, int delta) {
            return false;
        }

        @Override
        public boolean onMotionUp(MotionEvent event) {
            return false;
        }
    };

    public ScrollOverListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ScrollOverListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScrollOverListView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mTopPosition = 0;
    }

    public void setOnScrollOverListener(OnScrollOverListener onScrollOverListener) {
        mOnScrollOverListener = onScrollOverListener;
    }

    public void setTopPosition(int index) {
        if (index < 0)
            throw new IllegalArgumentException("Top position must > 0");
        mTopPosition = index;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastY = (int)event.getRawY();
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int)event.getRawY();

        boolean isHandled = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLastY = y;
                isHandled = mOnScrollOverListener.onMotionDown(event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int childCount = getChildCount();
                if (childCount == 0) {
                    break;
                }

                final int deltaY = y - mLastY;
                isHandled = mOnScrollOverListener.onMotionMove(event, deltaY);
                if (isHandled) {
                    break;
                }

                final int firstTop = getChildAt(0).getTop();
                final int listPadding = getListPaddingTop();

                final int firstVisiblePosition = getFirstVisiblePosition();

                if (firstVisiblePosition <= mTopPosition && firstTop >= listPadding && deltaY > 0) {
                    isHandled = mOnScrollOverListener.onListViewTopAndPullDown(event, deltaY);
                    if (isHandled) {
                        break;
                    }
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                isHandled = mOnScrollOverListener.onMotionUp(event);
                break;
            }
        }

        mLastY = y;
        if (isHandled) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mLockDraw) {
            super.onDraw(canvas);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mLockDraw) {
            super.dispatchDraw(canvas);
        }
    }

    public void setLockDraw(boolean lockDraw) {
        mLockDraw = lockDraw;
    }
}
