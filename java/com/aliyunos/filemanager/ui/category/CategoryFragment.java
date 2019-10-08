package com.aliyunos.filemanager.ui.category;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCategoryEvent;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrFileEvent;
import com.aliyunos.filemanager.core.FileMgrScanEvent;
import com.aliyunos.filemanager.core.FileMgrTransfer;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.TabFragment;
import com.aliyunos.filemanager.util.FormatUtil;
import com.aliyunos.filemanager.core.StorageInfo;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.squareup.otto.Subscribe;

public class CategoryFragment extends TabFragment {
    private static final String TAG = "CategoryFragment";
    private CategoryListAdapter mCategoryAdapter;
    private View mMainView;
    private Context mContext;
    private ViewGroup mFileListContainer;
    private CategoryFileListView mFileListView;
    private CategoryFileListAdapter mFileListAdapter;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFileListView != null) {
            mFileListView.onResume();
        }

        BusProvider.getInstance().register(this);

        if (FileMgrCore.isScanned()) {
            mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    onCategoryFileChange(null);
                    mFileListView.onCategoryFileChange(null);
                    mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        } else {
            FileMgrCore.doFirstScan(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mFileListView != null) {
            mFileListView.onPause();
        }

        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFileListView != null) {
            mFileListView.onDestroy();
        }
    }

    @Override
    public void onPageScrolled() {
        super.onPageScrolled();

        long curTime = System.currentTimeMillis();
        if (mRootView != null) {
            onCategoryFileChange(null);
            Log.d("CategoryFragment", "onCategoryFileChange Fragment consumeTime = " + (System.currentTimeMillis() - curTime));
            mFileListView.onCategoryFileChange(null);
            Log.d("CategoryFragment", "onCategoryFileChange FileListView consumeTime = " + (System.currentTimeMillis() - curTime));
        }

        Log.d("CategoryFragment", "onPageScrolled consumeTime = " + (System.currentTimeMillis() - curTime));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            return rootView;
        }

        mContext = this.getActivity();
        mRootView = inflater.inflate(R.layout.category_main, container, false);
        mMainView = mRootView.findViewById(R.id.category_main_id);
        GridView gridView = (GridView)mRootView.findViewById(R.id.Category_grid);
        mCategoryAdapter = new CategoryListAdapter(this.getActivity());
        gridView.setAdapter(mCategoryAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Log.v(TAG, "onItemClick " + view.getId());
                mMainView.setVisibility(View.INVISIBLE);
                mFileListContainer = getFileListContainer();
                mFileListContainer.setVisibility(View.VISIBLE);
                mFileListView.showFileList(KbxLocalFileManager.CategoryType.values()[pos]);
            }
        });

        mFileListAdapter = new CategoryFileListAdapter(this.getActivity());
        mFileListView = new CategoryFileListView(
                this.getActivity(), new CategoryFileListView.CategoryFileListCallback() {
            @Override
            public void onRootClicked() {
                backToRootLevel();
            }
        });

        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                refreshCapacity();
                mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        // add by huangjiawei at 20170221
        mHandler = new Handler();
        return mRootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backToUpLevel()) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onScanCompleted(FileMgrScanEvent event) {
        mCategoryAdapter.notifyDataSetChanged();
        refreshCapacity();
    }

    @Subscribe
    public void onCategoryFileChange(FileMgrCategoryEvent event) {
        if (FileMgrTransfer.getsInstance().isTransfering()
                || FileMgrCore.isIgnoreFileWatch()) {
            return;
        }
        Log.d(TAG, "onCategoryFileChange");
        if (mCategoryAdapter != null) {
            mCategoryAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onFileChange(FileMgrFileEvent event) {
        if (event.getEventType() == FileMgrFileEvent.EVENT_TYPE_FILECHANGE_EVENT_LIST) {
            if (FileMgrTransfer.getsInstance().isTransfering()
                    || FileMgrCore.isIgnoreFileWatch()) {
                return;
            }
            refreshCapacity();
        }
    }

    private boolean backToRootLevel() {
        mFileListView.backToUpLevel();

        if (mMainView.getVisibility() == View.INVISIBLE) {
            mMainView.setVisibility(View.VISIBLE);
            getFileListContainer().setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    private boolean backToUpLevel() {
        if (mFileListView.backToUpLevel()) {
            return true;
        }

        if (mMainView.getVisibility() == View.INVISIBLE) {
            mMainView.setVisibility(View.VISIBLE);
            getFileListContainer().setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    private void refreshCapacity() {
//        final long aliTotalSize = FileMgrCore.getTotalCapacity();
    	/*
         *add by huangjiawei at 2016/9/27 for change to compatie OTG and so on.
         *alter by huangjiawei at 20170221 增加线程实现，handler.post start
         */
    	Runnable mRunnable = new Runnable() {
			
			@Override
			public void run() {
				long timeOne = System.currentTimeMillis();
				Log.i("CategoryFragment", "timeOne == " + timeOne);
				long[] doovTotalCapacity = FileMgrCore.getDoovTotalCapacity(mContext);
		    	long RomInfo = doovTotalCapacity[0];
		    	long totalCatacity = doovTotalCapacity[1];
		    	long isOnlyRom = doovTotalCapacity[2];
		        final long aliAvalSize = FileMgrCore.getAvailableCapacity();
		        if (RomInfo > 0) {
		            TextView totalMemoryView = (TextView)mRootView.findViewById(R.id.storage_unuse_memory_id);
		           // add by huangjiawei at 2016/9/9 begin
		            String totalString = "";
		            String avalString = "";
		            totalString = FormatUtil.totalStorage(RomInfo,totalCatacity,isOnlyRom);
		            avalString = FormatUtil.formatCapacitySize(aliAvalSize);
		            /*
		            add by huangjiawei at 2016/9/27 for change to compatie OTG and so on.
		            */
		            String strMemInfo = mContext.getString(R.string.capacity_all)
		                                + totalString + "     "
		                                + mContext.getString(R.string.capacity_avail)
		                                + avalString;
		            long timeTwo = System.currentTimeMillis();
					Log.i("CategoryFragment", "timeTwo == " + timeTwo);
		            totalMemoryView.setText(strMemInfo);
			}
		}
    	};
    	mHandler.post(mRunnable);
    	/*
    	 * alter by huangjiawei at 20170221 增加线程实现，handler.post end
    	 */
    	long timeThree = System.currentTimeMillis();
		Log.i("CategoryFragment", "timeThree == " + timeThree);
            View progressOther = mRootView.findViewById(R.id.capacity_progress_other);
            View progressImage = mRootView.findViewById(R.id.capacity_progress_image);
            View progressVideo = mRootView.findViewById(R.id.capacity_progress_video);
            View progressAudio = mRootView.findViewById(R.id.capacity_progress_audio);
            View progressDocument = mRootView.findViewById(R.id.capacity_progress_document);
            View progressAPK = mRootView.findViewById(R.id.capacity_progress_apk);
            View progressZIP = mRootView.findViewById(R.id.capacity_progress_archive);

            TextView memoryOther = (TextView)mRootView.findViewById(R.id.capacity_text_other);
            TextView memoryImage = (TextView)mRootView.findViewById(R.id.capacity_text_picture);
            TextView memoryVideo = (TextView)mRootView.findViewById(R.id.capacity_text_video);
            TextView memoryAudio = (TextView)mRootView.findViewById(R.id.capacity_text_music);
            TextView memoryDocument = (TextView)mRootView.findViewById(R.id.capacity_text_text);
            TextView memoryAPK = (TextView)mRootView.findViewById(R.id.capacity_text_apk);
            TextView memoryZIP = (TextView)mRootView.findViewById(R.id.capacity_text_zip);

            setCategoryProgressAndSize((TextView)progressImage, memoryImage,
                    KbxLocalFileManager.CategoryType.Image, R.string.CategoryItemPic);
            setCategoryProgressAndSize((TextView)progressVideo, memoryVideo,
                    KbxLocalFileManager.CategoryType.Video, R.string.CategoryItemVideo);
            setCategoryProgressAndSize((TextView)progressAudio, memoryAudio,
                    KbxLocalFileManager.CategoryType.Audio, R.string.CategoryItemMusic);
            setCategoryProgressAndSize((TextView)progressDocument, memoryDocument,
                    KbxLocalFileManager.CategoryType.Doc, R.string.CategoryItemText);
            setCategoryProgressAndSize((TextView)progressAPK, memoryAPK,
                    KbxLocalFileManager.CategoryType.Apk, R.string.CategoryItemApk);
            setCategoryProgressAndSize((TextView)progressZIP, memoryZIP,
                    KbxLocalFileManager.CategoryType.Zip, R.string.CategoryItemZip);
            setCategoryProgressAndSize((TextView)progressOther, memoryOther);
        
    }

    private void setCategoryProgressAndSize(TextView progressView, TextView capacityView,
            KbxLocalFileManager.CategoryType categoryType, int categoryFormatString) {
        long totalSize = FileMgrCore.getTotalCapacity();
        long totalWidth = mRootView.findViewById(R.id.capacity_bar).getWidth();
        long categoryTypeSize = FileMgrCore.getCategoryTypeSize(categoryType);
        long width = totalWidth * categoryTypeSize / totalSize;

        progressView.setWidth((int)width);
        capacityView.setText(mContext.getResources().getText(categoryFormatString)
                + " " + FormatUtil.formatCapacitySize(categoryTypeSize));
    }

    private void setCategoryProgressAndSize(TextView progressView, TextView capacityView) {
        long totalSize = FileMgrCore.getTotalCapacity();
        long totalWidth = mRootView.findViewById(R.id.capacity_bar).getWidth();
        long otherTypeSize = FileMgrCore.getOtherTypeCapacity();
        long width = totalWidth * otherTypeSize / totalSize;

        progressView.setWidth((int)width);
        capacityView.setText(mContext.getResources().getText(R.string.CategoryItemNone)
                + " " + FormatUtil.formatCapacitySize(otherTypeSize));
    }

    private ViewGroup getFileListContainer() {
        if (mFileListContainer == null) {
            ViewStub viewStub = (ViewStub)getView().findViewById(R.id.category_subtypes_id);
            mFileListContainer = (ViewGroup)viewStub.inflate();
            mFileListView.init(mFileListContainer, mFileListAdapter);
        }
        return mFileListContainer;
    }
}
