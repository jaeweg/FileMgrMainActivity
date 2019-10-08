package com.aliyunos.filemanager.ui.view;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.aliyunos.filemanager.FileMgrMainActivity;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.util.FileUtil;
import com.aliyunos.filemanager.util.FooterBarUtil;
import com.aliyunos.filemanager.util.GraphicUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.aliyunos.filemanager.util.ShareUtil;

import java.io.File;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionSheet;
import hwdroid.widget.FooterBar.FooterBarMenu;
import hwdroid.widget.FooterBar.FooterBarType;
import yunos.ui.util.ReflectHelper;

public abstract class FileListView implements FooterBarType.OnFooterItemClick {
    private static final String TAG = "FileListView";

    public enum Mode {
        Normal,
        Selecting,
        Searching,
        GetContext
    }

    protected Activity mActivity;
    protected ViewGroup mContainer;
    protected ScrollOverListView mListView;
    protected FileListViewAdapter mListAdapter;
    protected TextView mNoItemTextView;
    protected ImageView mNoItemImageView;
    protected TextView mNoSearchItemTextView;
    protected LinearLayout mFooterBarContainer;
    protected FooterBarMenu mFooterBarMenu;
    protected FooterBarUtil.FooterItemInfo[] mMainFooterItemInfos;
    protected FooterBarUtil.FooterItemInfo[] mFooterItemInfos;
    protected int[] mItemLongClickIds;
    protected int[] mFolderLongClickIds;
    protected LinearLayout mDirContainer;
    protected Mode mCurrentMode;
    protected int mSelectableItemId = -1;
    protected CheckBox mRightSelectCheckBox;
    private   Handler mHandler = new Handler();
    private   int mOffsetForHeaderAnimation = 0;
    private   static final int ANIMATION_DURATION = 300;
    private   ActionBarView  mNormalActionBarView;
    private   ActionBarView  mEditAcionBarView;
    private   boolean        mExitEditMode = false;

    private HorizontalScrollView mTagScroll;

    public static class PathInfo {
        String showName;
        String path;
        int lastIndex = -1;
        public int getLastIndex() {return lastIndex;};
        public void setLastIndex(int lastIndex) {this.lastIndex = lastIndex;};
        public String getShowName() { return showName; }
        public String getPath() { return path; }
        public PathInfo(String name, String p) {
            this.showName = name;
            this.path = p;
        }
    }
    protected List<PathInfo> mCurrentPath = new ArrayList<PathInfo>();

    public FileListView(Activity activity) {
        mActivity = activity;
        mCurrentMode = Mode.Normal;
    }

    protected void init(ViewGroup container, FileListViewAdapter adapter) {
        mContainer = container;
        mListAdapter = adapter;

        mListView = (ScrollOverListView)mContainer.findViewById(R.id.scroll_listview);
        mNoItemImageView = (ImageView)mContainer.findViewById(R.id.empty_pic);
        mNoItemTextView = (TextView)mContainer.findViewById(R.id.no_item_text);
        mNoSearchItemTextView = (TextView)mContainer.findViewById(R.id.nosearch_item_text);
        mFooterBarContainer = (LinearLayout)mContainer.findViewById(R.id.footbar_menu);
        mDirContainer = (LinearLayout)mContainer.findViewById(R.id.dir_container);
        mTagScroll = (HorizontalScrollView) mContainer.findViewById(R.id.horizontalScrollView1);
        mOffsetForHeaderAnimation = mActivity.getResources().getDimensionPixelSize(R.dimen.offset_header_animation);
        mListView.setOnScrollOverListener(new ScrollOverListView.OnScrollOverListener() {
            @Override
            public boolean onListViewTopAndPullDown(MotionEvent event, int delta) {
                onListViewPullDownFromTop(event, delta);
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
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                int mFirstVisibleItem = 0;
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        if (mCurrentPath != null && mCurrentPath.size() >= 1) {
                            mCurrentPath.get(mCurrentPath.size() - 1).setLastIndex(mFirstVisibleItem);
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                    mFirstVisibleItem = firstVisibleItem;
                }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onListItemClick(view, view.getId());
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return onListItemLongClick(view, view.getId());
            }
        });

        mListView.setAdapter(mListAdapter);
        mListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mCurrentMode == Mode.Normal || mCurrentMode == Mode.GetContext) {
                    resetFooterBarMenu();
                    refreshCurrentMode();
                } else if (mCurrentMode == Mode.Searching) {
                    mNoSearchItemTextView.setVisibility(
                            mListAdapter.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);
                    mListAdapter.reSearch();
                }
            }
        });

        BusProvider.getInstance().register(this);
        FileMgrCore.doFirstScan(false);

        setMode(mCurrentMode);
    }

    public void onResume() {
        BusProvider.getInstance().register(this);
    }

    public void onPause() {
        BusProvider.getInstance().unregister(this);
    }

    public void onDestroy() {
        releaseInputMethod(mActivity);
    }

    public boolean backToUpLevel() {
        if (mCurrentMode != Mode.Normal && mCurrentMode != Mode.GetContext) {
            Log.d(TAG, "backToUpLevel");
            setMode(Mode.Normal);
            return true;
        }

        return false;
    }

    protected String getShowNameFromInfo(List<PathInfo> pathInfos) {
        String showName = "";

        int length = pathInfos.size();
        for (int i = 0; i < length; i++) {
            showName += pathInfos.get(i).getShowName();

            if (i < length - 1) {
                showName += "/";
            }
        }
        return showName;
    }

    protected String getFullPathFromInfo(List<PathInfo> pathInfos) {
        int length = pathInfos.size();
        if (length == 0) {
            return "/";
        }
        String fullPath = pathInfos.get(pathInfos.size() - 1).getPath();
        if (fullPath == null || fullPath.length() == 0) {
            fullPath = "/";
        }
        return fullPath;
    }

    protected boolean isRootPath() {
        return mCurrentPath.size() == 1;
    }

    protected void onListViewPullDownFromTop(MotionEvent event, int delta) {
        if (delta > 20) {
            mListAdapter.showSearchButton(true);
        }
    }

    protected boolean onListItemClick(View itemView, int id) {
        if (id == FileListViewAdapter.SEARCH_BUTTON_ID) {
            setMode(Mode.Searching);
            return true;
        } else if (mCurrentMode == Mode.Selecting || mCurrentMode == Mode.GetContext) {
            if (mCurrentMode == Mode.GetContext) {
                FileInfo[] fileInfoList = mListAdapter.getFileInfoList();
                FileInfo fileInfo = fileInfoList[id];
                if (fileInfo.isFolder())
                    return false;
            }

            boolean selected = mListAdapter.getItemSelected(id);
            mListAdapter.setItemSelected(itemView, id, !selected);

            if (mRightSelectCheckBox != null) {
                mRightSelectCheckBox.setChecked(mListAdapter.getSelectedItemCount() == mListAdapter.getCount());
            }

            refreshFooterBarMenu();
            View custemView = mActivity.getActionBar().getCustomView();
            if(custemView instanceof ActionBarView) {
                ActionBarView actionBarView = (ActionBarView)custemView;
                actionBarView.setTitleColor(mActivity.getResources().getColor(R.color.select_text_color));
                String format = mActivity.getResources().getString(R.string.numberOfSelected);
                String title = String.format(format, mListAdapter.getSelectedItemCount());
                actionBarView.setTitle(title);
            }
            return true;
        }

        return false;
    }

    protected boolean onListItemLongClick(View itemView, int id) {
        if (mCurrentMode == Mode.Selecting || mCurrentMode == Mode.GetContext) {
            return true;
        }
        FileInfo[] fileInfoList = mListAdapter.getFileInfoList();
        if (id < 0 || id > fileInfoList.length)
            return true;

        final FileInfo fileInfo = fileInfoList[id];
        final int[] menuIds = fileInfo.isFolder() ? mFolderLongClickIds : mItemLongClickIds;

        FileActionSheet actionSheet = new FileActionSheet(mActivity);
        actionSheet.showMenu(menuIds, new ActionSheet.CommonButtonListener() {
            @Override
            public void onClick(int position) {
                onActionMenuClick(fileInfo, menuIds[position]);
            }

            @Override
            public void onDismiss(ActionSheet actionSheet) { }
        });
        return true;
    }

    protected void onActionMenuClick(FileInfo fileInfo, int action) {
        switch (action) {
            case FileUtil.FILE_OPERATOR_SHARE:
                ArrayList<FileInfo> list = new ArrayList<FileInfo>();;
                list.add(fileInfo);
                ShareUtil.shareDialog(mActivity, list);
                break;
        }
    }

    public View getListView() {
        return mListView;
    }

    // -----FooterBar
    protected void setMainFooterBarItems(FooterBarUtil.FooterItemInfo[] footerItemInfos) {
        mMainFooterItemInfos = footerItemInfos;
        setFooterBarItems(footerItemInfos);
    }

    protected void setFooterBarItems(FooterBarUtil.FooterItemInfo[] footerItemInfos) {
        setFooterBarItems(footerItemInfos, -1);
    }

    protected void setFooterBarItems(FooterBarUtil.FooterItemInfo[] footerItemInfos, int selectableItemId) {
        mFooterItemInfos = footerItemInfos;
        mSelectableItemId = selectableItemId;
        refreshFooterBarMenu();
    }

    protected void refreshFooterBarMenu() {
        if (mFooterBarMenu != null) {
            mFooterBarMenu.clear();
        }
        resetFooterBarMenu();
    }

    protected void resetFooterBarMenu() {
        switch (mCurrentMode) {
            case Normal:
                resetFooterBarMenuInNormal();
                break;
            case Selecting:
                resetFooterBarMenuInSelectMode();
                break;
            case Searching:
                mFooterBarContainer.setVisibility(View.GONE);
                break;
            case GetContext:
                resetFooterBarMenuInGetContextMode();
                break;
        }
    }

    @Override
    public void onFooterItemClick(View view, int id) {
        switch (id) {
            case FileUtil.FILE_OPERATOR_SORT: {
                ActionSheet actionSheet = new ActionSheet(mActivity);
                actionSheet.setTitle(mActivity.getString(R.string.actions_menu_Sort));
                actionSheet.setSingleChoiceItems(new String[] {
                                mActivity.getString(R.string.ali_order_name),
                                mActivity.getString(R.string.ali_order_size),
                                mActivity.getString(R.string.ali_order_date),
                                mActivity.getString(R.string.ali_order_type)},
                        PreferenceUtil.getInteger(PreferenceUtil.Setting.SETTINGS_KEY_ORDER),
                        new ActionSheet.SingleChoiceListener() {
                            @Override
                            public void onClick(int position) {
                                PreferenceUtil.setInteger(PreferenceUtil.Setting.SETTINGS_KEY_ORDER, position);
                                mListAdapter.setSortMode(
                                        FileListViewAdapter.SortMode.values()[position]
                                );
                            }
                            @Override
                            public void onDismiss(ActionSheet actionSheet) {}
                        });
                actionSheet.show(view);
                break;
            }
            case FileUtil.FILE_OPERATOR_COPY:
                if (mCurrentMode != Mode.Selecting) {
                    setFooterBarItems(FooterBarUtil.getItemsByIds(new int[] {
                            FileUtil.FILE_OPERATOR_COPY
                    }), FileUtil.FILE_OPERATOR_COPY);
                    setMode(Mode.Selecting);
                }
                break;
            case FileUtil.FILE_OPERATOR_DELETE:
                setFooterBarItems(FooterBarUtil.getItemsByIds(new int[] {
                        FileUtil.FILE_OPERATOR_DELETE
                }), FileUtil.FILE_OPERATOR_DELETE);
                setMode(Mode.Selecting);
                break;
            case FileUtil.FILE_OPERATOR_CUT:
                if (mCurrentMode != Mode.Selecting) {
                    setFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                            FileUtil.FILE_OPERATOR_CUT
                    }), FileUtil.FILE_OPERATOR_CUT);
                    setMode(Mode.Selecting);
                }
                break;
            case FileUtil.FILE_OPERATOR_SHARE:
                if(mCurrentMode != Mode.Selecting) {
                    setFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                            FileUtil.FILE_OPERATOR_SHARE
                    }), FileUtil.FILE_OPERATOR_SHARE);
                    setMode(Mode.Selecting);
                }
                break;
            case FileUtil.FILE_OPERATOR_ZIP:
                if(mCurrentMode != Mode.Selecting) {
                    setFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                            FileUtil.FILE_OPERATOR_ZIP
                    }), FileUtil.FILE_OPERATOR_ZIP);
                    setMode(Mode.Selecting);
                }
                break;
        }
    }

    protected void resetFooterBarMenuInNormal() {
        TextView indexingTextView = (TextView)mContainer.findViewById(R.id.indexing_text);

        if (mListAdapter.getCount() != 0 || FileMgrCore.isScanned()) {
            if (mFooterBarMenu == null) {
                mFooterBarMenu = new FooterBarMenu(mActivity);
                mFooterBarMenu.setOnFooterItemClick(this);
                mFooterBarContainer.addView(mFooterBarMenu);
            }
            indexingTextView.setVisibility(View.INVISIBLE);
            mFooterBarContainer.setVisibility(View.VISIBLE);

            for (FooterBarUtil.FooterItemInfo info:mFooterItemInfos) {
                if (mFooterBarMenu.getItem(info.itemId) == null) {
                    mFooterBarMenu.addItem(
                            info.itemId,
                            mActivity.getResources().getText(info.textId),
                            mActivity.getResources().getDrawable(info.iconId));
                }
            }

            mFooterBarMenu.setPrimaryItemCount(3);
            mFooterBarMenu.updateItems();
        } else {
            indexingTextView.setVisibility(View.VISIBLE);
            mFooterBarContainer.setVisibility(View.INVISIBLE);
        }
    }

    private void resetFooterBarMenuInSelectMode() {
        for (FooterBarUtil.FooterItemInfo info:mFooterItemInfos) {
            if (mFooterBarMenu.getItem(info.itemId) == null) {
                mFooterBarMenu.addItem(
                        info.itemId,
                        mActivity.getResources().getText(info.textId),
                        mActivity.getResources().getDrawable(info.iconId));
            }
        }

        if (mSelectableItemId != -1) {
            if (mListAdapter.getSelectedItemCount() > 0) {
                mFooterBarMenu.setItemTextColor(
                        mSelectableItemId, mActivity.getResources().getColor(R.color.button_text_color_ok));
                mFooterBarMenu.setItemEnable(mSelectableItemId, true);
            } else {
                mFooterBarMenu.setItemTextColor(
                        mSelectableItemId, mActivity.getResources().getColor(R.color.button_text_color_ok_disable));
                mFooterBarMenu.setItemEnable(mSelectableItemId, false);
            }
            mFooterBarMenu.updateItems();
        }
    }

    protected void resetFooterBarMenuInGetContextMode() {
        TextView indexingTextView = (TextView)mContainer.findViewById(R.id.indexing_text);

        if (mListAdapter.getCount() != 0 || FileMgrCore.isScanned()) {
            indexingTextView.setVisibility(View.INVISIBLE);
        } else {
            indexingTextView.setVisibility(View.VISIBLE);
        }
    }

    // -----Mode Switch
    public void setMode(Mode mode) {
        if (mCurrentMode != mode) {
            if((mCurrentMode == Mode.Selecting && mode == Mode.Normal) || (mCurrentMode == Mode.Normal && mode == Mode.Selecting)){
                mListAdapter.setAnimationRegion(mListView.getFirstVisiblePosition(),mListView.getLastVisiblePosition());
                mListAdapter.setNeedAnimation(true);
            } else {
                mListAdapter.setNeedAnimation(false);
            }

            if(mCurrentMode == Mode.Selecting && mode == Mode.Normal) {
                mExitEditMode = true;
            } else {
                mExitEditMode = false;
            }

            mCurrentMode = mode;
            FileMgrMainActivity.setViewPagerScrollable(true);
            refreshCurrentMode();
            refreshFooterBarMenu();
        }
    }

    protected void refreshCurrentMode() {
        switch (mCurrentMode) {
            case Normal:
                switchToNormalMode();
                break;
            case Selecting:
                switchToSelectMode();
                break;
            case Searching:
                switchToSearchMode();
                break;
            case GetContext:
                switchToGetContextMode();
                break;
        }
    }

    protected void switchToNormalMode() {
        if(!mExitEditMode) {
            mListAdapter.setMode(FileListViewAdapter.Mode.Normal);
            FileMgrMainActivity.refreshActionBarView();
        } else {
            mExitEditMode = false;
            Drawable backgroundColor = ReflectHelper.Context.getDrawable(mActivity, R.color.setting_header_color);
            ActionBar actionBar = mActivity.getActionBar();
            actionBar.setBackgroundDrawable(backgroundColor);
            mEditAcionBarView = (ActionBarView)mActivity.getActionBar().getCustomView();
            mEditAcionBarView.setBackgroundResource(R.color.setting_header_color);
            mEditAcionBarView.animate().alpha(0).translationYBy(-mOffsetForHeaderAnimation)
            .setInterpolator(new AccelerateInterpolator())
            .setDuration(ANIMATION_DURATION/2)
            .setListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mListAdapter.setMode(FileListViewAdapter.Mode.Normal);
                    mEditAcionBarView.setVisibility(View.GONE);
                    mEditAcionBarView.setAlpha(1);
                    mEditAcionBarView.setY(mEditAcionBarView.getY() + mOffsetForHeaderAnimation);
                    mEditAcionBarView.setBackgroundResource(R.drawable.setting_head);
                    FileMgrMainActivity.AnimationActionBarView();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            }).start();
        }

        mFooterItemInfos = mMainFooterItemInfos;
        if (mListAdapter.getCount() == 0 && FileMgrCore.isScanned()) {
            mNoItemTextView.setVisibility(View.VISIBLE);
            mNoItemImageView.setVisibility(View.VISIBLE);
        } else {
            mNoItemTextView.setVisibility(View.INVISIBLE);
            mNoItemImageView.setVisibility(View.INVISIBLE);
        }
        mNoSearchItemTextView.setVisibility(View.INVISIBLE);

        // hide keyboard in normal mode
        View currentFocusView = mActivity.getCurrentFocus();
        if (currentFocusView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    protected void switchToSelectMode() {
        FileMgrMainActivity.setViewPagerScrollable(false);
        mListAdapter.setMode(FileListViewAdapter.Mode.Selecting);
        ActionBar actionBar = mActivity.getActionBar();
        ActionBarView actionBarView= new ActionBarView(mActivity);

        actionBarView.setTitleColor(mActivity.getResources().getColor(R.color.select_text_color));
        String format = mActivity.getResources().getString(R.string.numberOfSelected);
        String title = String.format(format, 0);
        actionBarView.setTitle(title);

        // back button
        Drawable backIcon= ReflectHelper.Context.getDrawable(mActivity, R.drawable.actionbar_back_icon);
        actionBarView.showBackKey(true, backIcon, new ActionBarView.OnBackKeyItemClick() {
            @Override
            public void onBackKeyItemClick() {
                backToUpLevel();
            }
        });

        // check button
        mRightSelectCheckBox = new CheckBox(mActivity);
        Drawable checkboxIcon = ReflectHelper.Context.getDrawable(mActivity, R.drawable.check_selector);
        mRightSelectCheckBox.setButtonDrawable(checkboxIcon);
        actionBarView.addRightItem(mRightSelectCheckBox);

        // fix right item offset diff
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                View rightItem = (View) mRightSelectCheckBox.getParent();
                if (rightItem != null) {
                    rightItem.setPadding(
                            rightItem.getPaddingLeft() + (int) GraphicUtil.convertDpToPixel(mActivity, 4),
                            rightItem.getPaddingTop(),
                            rightItem.getPaddingRight(), rightItem.getPaddingBottom());
                }
            }
        });

        actionBarView.setOnRightWidgetItemClickListener2(new ActionBarView.OnRightWidgetItemClick2() {
            @Override
            public void onRightWidgetItemClick(View v) {
                mRightSelectCheckBox.setChecked(!mRightSelectCheckBox.isChecked());
                if (mListAdapter != null) {
                    mListAdapter.setAllItemSelected(mRightSelectCheckBox.isChecked());
                }

                refreshFooterBarMenu();

                ActionBarView actionBarView = (ActionBarView) mActivity.getActionBar().getCustomView();
                actionBarView.setTitleColor(mActivity.getResources().getColor(R.color.select_text_color));
                String format = mActivity.getResources().getString(R.string.numberOfSelected);
                String title = String.format(format, mListAdapter.getSelectedItemCount());
                actionBarView.setTitle(title);
            }
        });

        //actionBarView.setBackgroundResource(R.drawable.setting_head);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        //actionBar.setCustomView(actionBarView);

        Drawable backgroundColor = ReflectHelper.Context.getDrawable(mActivity, R.color.setting_header_color);
        actionBar.setBackgroundDrawable(backgroundColor);
        mNormalActionBarView = (ActionBarView)mActivity.getActionBar().getCustomView();
        mNormalActionBarView.setBackgroundResource(R.color.setting_header_color);
        mEditAcionBarView = actionBarView;
        mNormalActionBarView.animate().alpha(0).translationYBy(-mOffsetForHeaderAnimation)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(ANIMATION_DURATION/2).setListener(new Animator.AnimatorListener() {

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
                         mNormalActionBarView.setVisibility(View.GONE);
                         mNormalActionBarView.setAlpha(1);
                         mNormalActionBarView.setY(mNormalActionBarView.getY() + mOffsetForHeaderAnimation);
                         mNormalActionBarView.setBackgroundResource(R.drawable.setting_head);

                         ActionBar actionBar = mActivity.getActionBar();
                         actionBar.setCustomView(mEditAcionBarView);
                         mEditAcionBarView.setVisibility(View.VISIBLE);
                         mEditAcionBarView.setAlpha(0);
                         mEditAcionBarView.setBackgroundResource(R.color.setting_header_color);
                         mEditAcionBarView.setY(mEditAcionBarView.getY() - mOffsetForHeaderAnimation);
                         mEditAcionBarView.animate().alpha(1).translationYBy(mOffsetForHeaderAnimation)
                                 .setInterpolator(new DecelerateInterpolator())
                                 .setDuration(ANIMATION_DURATION/2)
                                 .setListener(new Animator.AnimatorListener() {
                                     @Override
                                     public void onAnimationStart(Animator animation) {

                                     }

                                     @Override
                                     public void onAnimationEnd(Animator animation) {
                                         ActionBar actionBar = mActivity.getActionBar();
                                         actionBar.setBackgroundDrawable(null);
                                         mEditAcionBarView.setBackgroundResource(R.drawable.setting_head);
                                     }

                                     @Override
                                     public void onAnimationCancel(Animator animation) {

                                     }

                                     @Override
                                     public void onAnimationRepeat(Animator animation) {

                                     }
                                 }).start();

                     }

                     @Override
                     public void onAnimationCancel(Animator animation) {
                         // TODO Auto-generated method stub

                     }
                 }).start();
    }

    protected void switchToSearchMode() {
        mListAdapter.setMode(FileListViewAdapter.Mode.Searching);
        FileMgrMainActivity.setViewPagerScrollable(false);

        final SearchView searchView= new SearchView(mActivity);
        searchView.setBackgroundResource(R.drawable.setting_full_search_head);

        searchView.setOnCloseListener(new android.widget.SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.clearFocus();
                FileMgrMainActivity.refreshActionBarView();
                setMode(Mode.Normal);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                mListAdapter.searchWithKey(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mListAdapter.searchWithKey(s);
                return false;
            }
        });

        ActionBar actionBar = mActivity.getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(searchView,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.WRAP_CONTENT));

        // change system bar color to white
        SystemBarColorManager systemBarColorManager = new SystemBarColorManager(mActivity);
        systemBarColorManager.setViewFitsSystemWindows(mActivity, true);
        systemBarColorManager.setStatusBarColor(Color.WHITE);
        systemBarColorManager.setStatusBarDarkMode(mActivity, true);

        searchView.initRealEditText();
        searchView.animateToSearch();
    }

    protected void switchToGetContextMode() {
        mListAdapter.setMode(FileListViewAdapter.Mode.GetContext);
        mFooterItemInfos = mMainFooterItemInfos;
        if (mListAdapter.getCount() == 0 && FileMgrCore.isScanned()) {
            mNoItemTextView.setVisibility(View.VISIBLE);
            mNoItemImageView.setVisibility(View.VISIBLE);
        } else {
            mNoItemTextView.setVisibility(View.INVISIBLE);
            mNoItemImageView.setVisibility(View.INVISIBLE);
        }

        ActionBarView actionBarView = (ActionBarView)mActivity.getActionBar().getCustomView();
        actionBarView.setTitleColor(mActivity.getResources().getColor(R.color.select_text_color));
        String format = mActivity.getResources().getString(R.string.numberOfSelected);
        String title = String.format(format, 0);
        actionBarView.setTitle(title);
    }

    protected void cancelSearchView() {
        if (mCurrentMode == Mode.Searching) {
            SearchView searchView = (SearchView) mActivity.getActionBar().getCustomView();
            searchView.animateToNormal();
        }
    }

    // -----Crumbs
    protected void onTagViewClick(String tag, String path) {
        if(mCurrentPath.size() > 0) {
            List<PathInfo> curPathInfos = mCurrentPath.subList(0,mCurrentPath.size());
            String curpath = getShowNameFromInfo(curPathInfos);
            if(curpath.equals(path)) {
                if (mCurrentMode == Mode.Selecting || mCurrentMode == Mode.Searching) {
                    setMode(Mode.Normal);
                }
                return;
            }
        }

        for (int i = 0; i < mCurrentPath.size(); i++) {
            if (mCurrentPath.get(i).getShowName().equals(tag)) {
                List<PathInfo> pathInfos = mCurrentPath.subList(0, i + 1);
                String tagPath = getShowNameFromInfo(pathInfos);
                if (tagPath.equals(path)) {
                    if (mCurrentMode == Mode.Selecting || mCurrentMode == Mode.Searching) {
                        setMode(Mode.Normal);
                        mListAdapter.setNeedAnimation(false);
                    }

                    mCurrentPath = new ArrayList<PathInfo>(pathInfos);
                    gotoPath(mCurrentPath);
                    break;
                }
            }
        }
    }

    private void releaseInputMethod(Context context) {
        if (context == null) {
            return;
        }
        try {
            // 对 mCurRootView mServedView mNextServedView 进行置空...
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) {
                return;
            }// author:sodino mail:sodino@qq.com

            Object obj_get = null;
            Field f_mCurRootView = imm.getClass().getDeclaredField("mCurRootView");
            Field f_mServedView = imm.getClass().getDeclaredField("mServedView");
            Field f_mNextServedView = imm.getClass().getDeclaredField("mNextServedView");

            if (f_mCurRootView.isAccessible() == false) {
                f_mCurRootView.setAccessible(true);
            }
            obj_get = f_mCurRootView.get(imm);
            if (obj_get != null) { // 不为null则置为空
                f_mCurRootView.set(imm, null);
            }

            if (f_mServedView.isAccessible() == false) {
                f_mServedView.setAccessible(true);
            }
            obj_get = f_mServedView.get(imm);
            if (obj_get != null) { // 不为null则置为空
                f_mServedView.set(imm, null);
            }

            if (f_mNextServedView.isAccessible() == false) {
                f_mNextServedView.setAccessible(true);
            }
            obj_get = f_mNextServedView.get(imm);
            if (obj_get != null) { // 不为null则置为空
                f_mNextServedView.set(imm, null);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void gotoPath(List<PathInfo> pathInfos) {}

    protected void setCrumbsPath(String path) {
        String[] subPaths = path.split(File.separator);
        String tagPath = "";

        LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        margin.setMargins(((int)GraphicUtil.convertDpToPixel(mActivity, -10)), 0, 0, 0);

        mDirContainer.removeAllViews();
        for (int i = 0; i < subPaths.length; i++) {
            String tag = subPaths[i];
            tagPath += tag;
            View tagView = getTagView(tag, tagPath);

            if (i == 0) {
                mDirContainer.addView(tagView);
            } else {
                mDirContainer.addView(tagView, margin);
            }

            tagPath += File.separator;
        }

        mHandler.removeCallbacks(mTagScrollRun);
        mHandler.post(mTagScrollRun);
    }

    private Runnable mTagScrollRun = new Runnable(){
        @Override public void run() {
            if(mTagScroll != null) {
                mTagScroll.fullScroll(View.FOCUS_RIGHT);
            }
        }
    };

    private View getTagView(final String tag, final String tagPath) {
        ViewGroup tagView = (LinearLayout) LayoutInflater.from(mActivity).inflate(R.layout.crumbs_item, null);
        tagView.setBackgroundResource(R.drawable.aui_crumb_bg_sub);
        tagView.setClickable(true);
        ((TextView)tagView.findViewById(R.id.dir_name)).setText(tag);

        tagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTagViewClick(tag, tagPath);
            }
        });
        return tagView;
    }
}
