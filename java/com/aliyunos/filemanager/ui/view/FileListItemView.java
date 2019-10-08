package com.aliyunos.filemanager.ui.view;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aliyunos.filemanager.R;

import hwdroid.widget.item.Item;
import hwdroid.widget.itemview.ItemView;

public class FileListItemView extends LinearLayout implements ItemView{
    private TextView mTextView;
    private TextView mDivideTextView;
    private TextView mAppTextView;
    private TextView mSubTextView;
    private ImageView mImageView;
    private ImageView mIsDownloadedView;
    private RelativeLayout mImageViewLayout;

    private LinearLayout mRightWidgetFrame;
    private CheckBox mCheckBox;
    private RadioButton mRadioButton;
    private ImageView mRightImageView;
    private int mOffsetForItemAnimation = 0;
    private static final int ANIMATION_DURATION = 300;
    private boolean mNeedAnimation = false;
    private Item.Type mMode;

    public FileListItemView(Context context) {
        this(context, null);
    }

    public FileListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.filelist_item_text);
        mDivideTextView = (TextView) findViewById(R.id.filelist_item_divide);
        mAppTextView = (TextView) findViewById(R.id.filelist_item_apptext);
        mSubTextView = (TextView) findViewById(R.id.filelist_item_subtext);
        mImageView = (ImageView) findViewById(R.id.filelist_item_left_icon);
        mIsDownloadedView = (ImageView) findViewById(R.id.download_flag);
        mImageViewLayout = (RelativeLayout) findViewById(R.id.filelist_item_left_drawable);
        mRightWidgetFrame = (LinearLayout) findViewById(R.id.filelist_item_right_widget);
        mOffsetForItemAnimation = this.getContext().getResources().getDimensionPixelSize(
                R.dimen.offset_item_animation);

        mDivideTextView.setText("|");
    }

    public void setAnimation(boolean bAnimation) {
        mNeedAnimation = bAnimation;
    }

    public void setObject(Item object) {
        final FileListItem item = (FileListItem) object;

        if (item.leftDrawable == null) {
            mImageViewLayout.setVisibility(View.GONE);
        } else {
            mImageViewLayout.setVisibility(View.VISIBLE);
            mImageView.setScaleType(item.scaleType);
            mImageView.setImageDrawable(item.leftDrawable);
        }

        mTextView.setEnabled(object.isEnabled());
        mAppTextView.setEnabled(object.isEnabled());
        mSubTextView.setEnabled(object.isEnabled());

        setTextView(item.mText);
        setAppTextView(item.appName);
        setSubtextView(item.mSubText);
        mMode = item.getTypeMode();
        switch(item.getTypeMode()) {
            case CHECK_MODE:
                mRightWidgetFrame.removeAllViews();
                if(mCheckBox == null) {
                    mCheckBox = new CheckBox(this.getContext());
                    mCheckBox.setClickable(false);
                    mCheckBox.setFocusable(false);
                } else {
                    setCheckBoxChecked(false);
                    mCheckBox.refreshDrawableState();
                }

                if (mNeedAnimation) {
                    mRightWidgetFrame.setTranslationX(mOffsetForItemAnimation);
                }
                mRightWidgetFrame.setVisibility(View.VISIBLE);
                mCheckBox.setEnabled(object.isEnabled());
                mRightWidgetFrame.addView(mCheckBox);
                setCheckBoxChecked(item.isChecked());

                if (mNeedAnimation) {
                    mRightWidgetFrame.animate().translationXBy(-mOffsetForItemAnimation)
                            .setInterpolator(new DecelerateInterpolator())
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new Animator.AnimatorListener() {

                                @Override
                                public void onAnimationStart(Animator animation) {
                                    // TODO Auto-generated method stub
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                    // TODO Auto-generated method stub

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    // TODO Auto-generated method stub
                                    mRightWidgetFrame.setTranslationX(0);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    // TODO Auto-generated method stub

                                }
                            }).start();
                }
                mNeedAnimation = false;
                break;
            case RADIO_MODE:
                mRightWidgetFrame.removeAllViews();
                if(mRadioButton == null) {
                    mRadioButton = new RadioButton(this.getContext());
                    mRadioButton.setClickable(false);
                    mRadioButton.setFocusable(false);
                }
                mRightWidgetFrame.setVisibility(View.VISIBLE);
                mRadioButton.setEnabled(object.isEnabled());
                mRightWidgetFrame.addView(mRadioButton);
                setRadioButtonChecked(item.isChecked());
                break;
            case IMAGE_MODE:
            case NORMAL_MODE:
                if(item.rightDrawable != null) {
                    if (mRightImageView == null) {
                        mRightImageView = new ImageView(this.getContext());
                    }
                }

                if (mNeedAnimation) {
                    mNeedAnimation = false;
                    mRightWidgetFrame.setTranslationX(0);
                    mRightWidgetFrame.animate().translationXBy(mOffsetForItemAnimation)
                            .setInterpolator(new DecelerateInterpolator())
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new Animator.AnimatorListener() {

                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mRightWidgetFrame.setTranslationX(0);
                                    mRightWidgetFrame.removeAllViews();
                                    if(item.rightDrawable != null) {
                                        if(mRightImageView != null) {
                                            mRightWidgetFrame.addView(mRightImageView);
                                            mRightImageView.setImageDrawable(item.rightDrawable);
                                            mRightWidgetFrame.setVisibility(View.VISIBLE);
                                        }
                                    }else {
                                        mRightWidgetFrame.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }
                            }).start();
                } else {
                    mRightWidgetFrame.removeAllViews();
                    if(item.rightDrawable != null) {
                        if(mRightImageView != null) {
                            mRightWidgetFrame.addView(mRightImageView);
                            mRightImageView.setImageDrawable(item.rightDrawable);
                            mRightWidgetFrame.setVisibility(View.VISIBLE);
                        }
                    }else {
                        mRightWidgetFrame.setVisibility(View.GONE);
                    }
                }
                break;
            default:
                mRightWidgetFrame.removeAllViews();
                mRightWidgetFrame.setVisibility(View.GONE);
                break;
        }
    }

    private void setTextView(String text) {
        mTextView.setText(text);
    }

    private void setAppTextView(String text) {
        if (text == null || text.isEmpty()) {
            mAppTextView.setVisibility(View.GONE);
            mDivideTextView.setVisibility(View.GONE);
        } else {
            mAppTextView.setText(text);
            //mTextView.setMaxWidth(550);
            mAppTextView.setVisibility(View.VISIBLE);
            mDivideTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setSubtextView(String text) {
        if(text == null || text.isEmpty()) {
            mSubTextView.setText("");
            mSubTextView.setVisibility(View.GONE);
            mTextView.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            mTextView.setGravity(Gravity.BOTTOM);
            mSubTextView.setVisibility(View.VISIBLE);
            mSubTextView.setText(text);
        }
    }

    public void setCheckBoxChecked(boolean checked) {
        if(mCheckBox != null)
            mCheckBox.setChecked(checked);
    }

    private void setRadioButtonChecked(boolean checked) {
        if(mRadioButton != null)
            mRadioButton.setChecked(checked);
    }

    public void setIsDownloaded(boolean downloaded) {
        if (mIsDownloadedView != null) {
            if (downloaded) {
                mIsDownloadedView.setVisibility(VISIBLE);
            } else {
                mIsDownloadedView.setVisibility(GONE);
            }
        }
    }

    public boolean isRadioMode() {
        if(mMode == Item.Type.RADIO_MODE) {
            return true;
        }

        return false;
     }

    @Override
    public void setSubTextSingleLine(boolean enabled) {
        if(mSubTextView != null) {
            mSubTextView.setSingleLine(enabled);
        }
    }
}
