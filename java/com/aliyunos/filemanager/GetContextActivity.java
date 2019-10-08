package com.aliyunos.filemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.aliyunos.filemanager.core.FileMgrFileEvent;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.category.CategoryFileListAdapter;
import com.aliyunos.filemanager.ui.category.CategoryFileListView;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.aliyunos.filemanager.ui.local.LocalFileListView;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListView;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import hwdroid.app.HWActivity;
import hwdroid.widget.ActionBar.ActionBarView;
import yunos.ui.util.ReflectHelper;

public class GetContextActivity extends HWActivity {
    private static final String TAG = "GetContextActivity";
    private static final String MAIL_MULTI_PICK = "multi-pick";
    private static final String ALIYUN_ACTION_BROWSE = "aliyun.intent.action.browse";
    private static final String ANDROID_PICK_ACION = "android.intent.action.PICK";

    private LocalFileListView mFileListView;
    private LocalFileListAdapter mFileListAdapter;
    private CategoryFileListView mCategoryFileListView;
    private CategoryFileListAdapter mCategoryFileListAdapter;

    private ActionBarView mActionBarView;
    private static Handler mHander = new Handler();

    private boolean mIsBrowseMode = false;
    private boolean mIsPickByType = false;

    class GetContextFileListView extends LocalFileListView {
        public GetContextFileListView(Activity activity) { super(activity); }
        @Override
        protected void onListViewPullDownFromTop(MotionEvent event, int delta) {
            return;
        }

        @Override
        protected void resetFooterBarMenu() {
            super.resetFooterBarMenu();
            mFooterBarContainer.setVisibility(View.GONE);
        }

        @Override
        protected boolean onListItemLongClick(View itemView, int id) {
            if(mIsBrowseMode) {
                return true;
            }

            return super.onListItemLongClick(itemView,id);
        }
    }

    class GetContextFileListViewByType extends CategoryFileListView {
        public GetContextFileListViewByType(Activity activity) {
            super(activity,new CategoryFileListView.CategoryFileListCallback() {
                @Override
                public void onRootClicked() {
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LayoutInflater mInflater = LayoutInflater.from(this);
        View mainView = mInflater.inflate(R.layout.getcontext_main, null);
        setActivityContentView(mainView);

        ViewGroup container = (ViewGroup)mainView.findViewById(R.id.filelist_view_container);

        mIsPickByType = isPickByMIMEType();
        if(mIsPickByType) {
            mCategoryFileListView = new GetContextFileListViewByType(this);
            mCategoryFileListAdapter = new CategoryFileListAdapter((this));
            mCategoryFileListAdapter.setMultiSelectable(getMultiSelectable());

            mCategoryFileListView.init(container, mCategoryFileListAdapter);
            mCategoryFileListView.setMode(FileListView.Mode.GetContext);
            mCategoryFileListView.showFileList(KbxLocalFileManager.CategoryType.Audio);

        } else {
            mFileListView = new GetContextFileListView(this);
            mFileListAdapter = new LocalFileListAdapter(this);
            mFileListAdapter.setMultiSelectable(getMultiSelectable());

            String browsePath = checkBrowseModeAndGetPath();
            FileListView.Mode mode = mIsBrowseMode ? FileListView.Mode.Normal : FileListView.Mode.GetContext;
            mFileListView.init(container, mFileListAdapter, mode);

            // check browse mode first.
            if (mIsBrowseMode) {
                if (browsePath.isEmpty()) {
                    mFileListView.gotoRootPath();
                } else {
                    mFileListView.gotoPath(browsePath);
                }
            } else {
                mFileListView.gotoRootPath();
            }
        }

        refreshActionBarView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void onFileChange(FileMgrFileEvent event) {
        if (mIsPickByType || mFileListView == null) {
           return;
        }

        mFileListView.onFileChange(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (backToUpLevel()) {
                return true;
            }

            this.finish();
            return false;
        }

        return false;
    }

    private boolean backToUpLevel() {
        if(!mIsPickByType && mFileListView.backToUpLevel() ) {
            return true;
        }

        return false;
    }

    private void refreshActionBarView() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            mActionBarView = new ActionBarView(this);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarMgr = new SystemBarColorManager(this);
        systemBarMgr.setViewFitsSystemWindows(this, true);
        systemBarMgr.setStatusBarColor(getResources().getColor(R.color.setting_header_color));
        systemBarMgr.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.status_dark_mode));

        getActionBar().setCustomView(mActionBarView,
                new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mActionBarView.setBackgroundResource(R.drawable.setting_head);
        if(!mIsBrowseMode) {

            //title
            mActionBarView.setTitleColor(getResources().getColor(R.color.select_text_color));
            String format = getResources().getString(R.string.numberOfSelected);
            String title = String.format(format, 0);
            mActionBarView.setTitle(title);

            // cancel button
            Drawable cancelIcon = ReflectHelper.Context.getDrawable(this, R.drawable.ic_cancel);
            mActionBarView.showBackKey(true, cancelIcon, new ActionBarView.OnBackKeyItemClick() {
                @Override
                public void onBackKeyItemClick() {
                    finish();
                }
            });

            // done button
            ImageButton doneButton = new ImageButton(this);
            Drawable doneIcon = ReflectHelper.Context.getDrawable(this, R.drawable.ic_done);
            doneButton.setBackground(doneIcon);
            mActionBarView.addRightItem(doneButton);
            mActionBarView.setOnRightWidgetItemClickListener2(new ActionBarView.OnRightWidgetItemClick2() {
                @Override
                public void onRightWidgetItemClick(View view) {
                    LinkedList<File> files = new LinkedList<File>();
                    FileInfo[] fileInfos;
                    if (mIsPickByType) {
                        fileInfos = mCategoryFileListAdapter.getFileInfoList();
                    } else {
                        fileInfos = mFileListAdapter.getFileInfoList();
                    }
                    for (FileInfo fileInfo : fileInfos) {
                        if (fileInfo.isSelected()) {
                            files.add(new File(fileInfo.getPath()));
                        }
                    }

                    Intent intent = new Intent();
                    if (files.size() == 0) {
                        mHander.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(FileMgrApplication.getInstance().getApplicationContext(),
                                        R.string.not_select_anyfile, Toast.LENGTH_LONG).show();
                            }
                        });
                        return;

                    } else if (files.size() == 1) {
                        // single select mode
                        intent.setData(Uri.fromFile(files.get(0)));
                    } else {
                        // multi select mode
                        ArrayList<Uri> uris = new ArrayList<Uri>();
                        for (File file : files) {
                            uris.add(Uri.fromFile(file));
                        }

                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    }
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });
        } else {
            //title
            mActionBarView.setTitleColor(getResources().getColor(R.color.select_text_color));
            mActionBarView.setTitle(getResources().getString(R.string.AllFiles));
        }
    }

    private String checkBrowseModeAndGetPath() {
        String browsePath = "";
        Intent intent = getIntent();
        if(intent.getAction() != null && intent.getAction().equalsIgnoreCase(ALIYUN_ACTION_BROWSE)) {
            mIsBrowseMode = true;
            if(intent.hasExtra("rootpath")) {
                String filePath = intent.getStringExtra("rootpath");
                Log.v(TAG, "fileUri:" + filePath);
                if (filePath != null) {
                    File tmpFile = new File(filePath);
                    if (tmpFile != null && tmpFile.isDirectory()) {
                        browsePath = tmpFile.getAbsolutePath();
                    }
                }
            }
        }
        return browsePath;
    }

    //目前只支持audio
    private  boolean  isPickByMIMEType() {
        Intent intent = getIntent();
        if(intent.getAction() !=null && intent.getAction().equalsIgnoreCase(ANDROID_PICK_ACION)) {
            String mimeType =  intent.resolveType(this);
            if(mimeType == null)
                return false;

            if(mimeType.equalsIgnoreCase("vnd.android.cursor.dir/audio") ||
                    mimeType.equalsIgnoreCase("audio/*")) {
                return true;
            }
        }

        return false;
    }

    private boolean getMultiSelectable() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(MAIL_MULTI_PICK, false)) {
            return true;
        }
        return false;
    }
}
