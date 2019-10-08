package com.aliyunos.filemanager.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrThumbnailLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.item.Item;
// add by huangjiawei at 20170710 sortByName include chinese begin
import com.aliyunos.filemanager.util.PinYin;
// add by huangjiawei at 20170710 sortByName include chinese end
public class FileListViewAdapter extends BaseAdapter implements FileMgrThumbnailLoader.LoadThumbnailCallback {
    private static final String TAG = "FileListViewAdapter";
    public static final int SEARCH_BUTTON_ID = -1;
    public static final int APP_NAME_TEXT_VIEW_ID = -2;

    public enum Mode {
        Normal,
        Selecting,
        Searching,
        GetContext
    }

    public enum SortMode {
        SortByName,
        SortBySize,
        SortByDate,
        SortByType
    }

    protected WeakReference<Context> mContext;
//    protected Context mContext;
    protected FileInfo[] mFileInfoList;
    protected boolean mIsShowSearchButton = false;
    protected SearchButton mSearchButton;
    protected String mSearchKey;
    protected Mode mCurrentMode;
    protected SortMode mCurrentSortMode;
    protected FileInfo[] mSearchInfoList;
    protected boolean mIsChanging = false;
    protected boolean mAlphaShowHidden = true;
    protected boolean mMultiSelectable = true;

    private  int mFirstAnimationPos = 0;
    private  int mLastAnimationPos = 0;
    private  boolean mNeedAnimation = false;
    private  Set<Integer> mAnimationSet = new HashSet<Integer>();

    public FileListViewAdapter(Context ctx) {
        mContext = new WeakReference<Context>(ctx);
        mCurrentMode = Mode.Normal;
    }

    @Override
    public void notifyDataSetChanged() {
        if (mIsChanging)
            return;

        mIsChanging = true;
        super.notifyDataSetChanged();
        mIsChanging = false;
    }

    @Override
    public int getCount() {
        FileInfo[] fileInfoList = getFileInfoList();
        if (fileInfoList != null)
            return fileInfoList.length + (mIsShowSearchButton ? 1 : 0);
        return 0;
    }

    @Override
    public Object getItem(int position) {

        return position;
    }
    @Override
    public long getItemId(int id) { return id; }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        int itemPosition = position;

        if (mContext == null) {
            return null;
        }
        Context context = mContext.get();
        if (context == null) {
            return null;
        }

        // 1. Show Search Button Check
        if (mIsShowSearchButton) {
            if (position == 0) {
                if (mSearchButton == null) {
                    mSearchButton = new SearchButton(context);
                    mSearchButton.setBackground(context.getResources().getDrawable(R.color.default_background));
                    mSearchButton.setBackgroundResource(R.drawable.setting_search_head);
                }
                convertView = mSearchButton;
                convertView.setId(SEARCH_BUTTON_ID);
                return convertView;
            } else {
                itemPosition -= 1;
            }
        }

        if (convertView != null && convertView instanceof SearchButton) {
            convertView = null;
        }

        // 2. Get fileInfoList from Search or Normal list
        FileInfo[] fileInfoList = getFileInfoList();
        FileInfo fileInfo = null;

        if (itemPosition < fileInfoList.length) {
            fileInfo = fileInfoList[itemPosition];
        }

        // 3. Compose the itemView with fileInfo
        if (fileInfo != null) {
            FileListItem item = new FileListItem();

            Locale locale = Locale.getDefault();
            if (locale.getLanguage().equals("zh") && locale.getCountry().equals("CN")) {
                //简体下才显示
                item.appName = fileInfo.getAppName();
            }

            item.mText = fileInfo.getName();
            item.mSubText = fileInfo.getMetaInfo(mContext.get());

            if (mCurrentMode == Mode.Selecting) {
                item.setTypeMode(Item.Type.CHECK_MODE);
                item.setChecked(fileInfo.isSelected());
            } else if (mCurrentMode == Mode.GetContext && !fileInfo.isFolder()) {
                item.setTypeMode(mMultiSelectable ? Item.Type.CHECK_MODE : Item.Type.RADIO_MODE);
                item.setChecked(fileInfo.isSelected());
            } else {
                item.setTypeMode(Item.Type.NORMAL_MODE);

                if (fileInfo.isFolder()) {
                    item.rightDrawable = context.getResources().getDrawable(R.drawable.ic_list_enter);
                } else {
                    item.rightDrawable = null;
                }
            }

            if (convertView == null) {
                FileListItemView itemView = (FileListItemView)item.newView(context, null);
                itemView.prepareItemView();
                convertView = itemView;
            }

            if (!fileInfo.isFolder()) {
                item.leftDrawable = FileMgrThumbnailLoader.getInstance().getDefaultThumbnailByType(
                        context, fileInfo.getType(), fileInfo.getName());

                loadThumbnail(convertView, item, fileInfo);
            } else {
                FileMgrThumbnailLoader.getInstance().removeLoadingKey(convertView);
            }

            //根据位置记忆是否需要动画
            boolean bNeedAnimate = false;
            if (mNeedAnimation && position >= mFirstAnimationPos && position <= mLastAnimationPos) {
                if (!mAnimationSet.contains(position)) {
                    bNeedAnimate = true;
                    mAnimationSet.add(position);
                }
            }

            ((FileListItemView)convertView).setAnimation(bNeedAnimate);
            ((FileListItemView)convertView).setObject(item);

            if (mAlphaShowHidden) {
                if (fileInfo.getName().startsWith(".")) {
                    convertView.setAlpha((float) 0.5);
                } else {
                    convertView.setAlpha((float) 1.0);
                }
            }
        }

        convertView.setId(itemPosition);
        return convertView;
    }

    protected void loadThumbnail(View convertView, FileListItem item, FileInfo fileInfo) {}

    @Override
    public void onLoadThumbnailComplete(View itemView, Object obj, Bitmap bitmap, int w, int h) {
        if (bitmap == null) return;

        if (mContext == null) {
            return;
        }
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        FileListItem item = (FileListItem)obj;
        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        drawable.setBounds(0, 0, w, h);
        item.leftDrawable = drawable;

        ((FileListItemView)itemView).setObject(item);
    }

    public FileInfo[] getFileInfoList() {
        return (mCurrentMode == Mode.Searching) ? mSearchInfoList : mFileInfoList;
    }

    public boolean setFileInfoListWithKeepSelectMode(FileInfo[] fileInfoList) {
        boolean isAllSelected = false;
        if (mCurrentMode == Mode.Selecting) {
            for (FileInfo nowFile : fileInfoList) {
                boolean fileIsExist = false;
                boolean isSelected = false;
                for (FileInfo originFile : mFileInfoList) {
                    if (originFile.getPath().equals(nowFile.getPath())) {
                        fileIsExist = true;
                        isSelected = originFile.isSelected();
                        nowFile.setSelected(isSelected);
                        break;
                    }
                }
            }

            mFileInfoList = fileInfoList;
            if(mContext.get() != null) {
                Activity activity = (Activity) mContext.get();
                ActionBarView actionBarView = (ActionBarView) activity.getActionBar().getCustomView();
                actionBarView.setTitleColor(activity.getResources().getColor(R.color.select_text_color));
                String format = activity.getResources().getString(R.string.numberOfSelected);
                String title = String.format(format, getSelectedItemCount());
                actionBarView.setTitle(title);
            }

            if (getSelectedItemCount() == getCount() && getCount() > 0) {
                isAllSelected = true;
            }
        } else {
            mFileInfoList = fileInfoList;
        }
        Log.i("huangjiawei", "currentSortMode == " + mCurrentSortMode);
        sortFileList();
        notifyDataSetChanged();
        return isAllSelected;
    }

    public void setFileInfoList(FileInfo[] fileInfoList) {
        mFileInfoList = fileInfoList;
        sortFileList();
        notifyDataSetChanged();
    }

    public void setMode(Mode mode) {
        if (mCurrentMode != mode) {
            mCurrentMode = mode;
            Log.d(TAG, "mode:" + mode);
            switch (mode) {
                case Normal:
                    this.notifyDataSetChanged();
                    break;
                case Selecting:
                    // reset selected
                    for (FileInfo fileInfo:mFileInfoList) {
                        fileInfo.setSelected(false);
                    }

                    if (mIsShowSearchButton) {
                        showSearchButton(false);
                    } else {
                        this.notifyDataSetChanged();
                    }
                    break;
                case Searching:
                    mSearchInfoList = mFileInfoList;
                    mSearchKey = "";
                    showSearchButton(false);
                    break;
                case GetContext:
                    this.notifyDataSetChanged();
                    break;
            }
        }
    }

    public void setSortMode(final SortMode sortMode) {
        if (mCurrentSortMode != sortMode) {
        	Log.i("huangjiawei", "sortMode == " + sortMode);
            mCurrentSortMode = sortMode;
            setFileInfoList(mFileInfoList);
        }
    }

    // Select Mode
    public ArrayList<FileInfo> getSelectedItems() {
        ArrayList<FileInfo> selectedFileInfos = new ArrayList<FileInfo>();
        for (FileInfo fileInfo:mFileInfoList) {
            if (fileInfo.isSelected())
                selectedFileInfos.add(fileInfo);
        }
        return selectedFileInfos;
    }

    public int getSelectedItemCount() {
        int count = 0;
        for (FileInfo fileInfo:mFileInfoList) {
            if (fileInfo.isSelected())
                count++;
        }
        return count;
    }

    public boolean getItemSelected(int index) {
        if (index > mFileInfoList.length)
            throw new IllegalArgumentException();
        return mFileInfoList[index].isSelected();
    }

    public void setMultiSelectable(boolean multiSelectable) {
        mMultiSelectable = multiSelectable;
    }

    public void setItemSelected(View itemView, int index, boolean selected) {
        if (index > mFileInfoList.length)
            throw new IllegalArgumentException();

        if(itemView != null) {
            ((FileListItemView)itemView).setCheckBoxChecked(selected);
        }

        if (mCurrentMode == Mode.GetContext) {
            if (!mMultiSelectable) {
                for (FileInfo fileInfo : mFileInfoList) {
                    fileInfo.setSelected(false);
                }
            }
        }

        mFileInfoList[index].setSelected(selected);

        if(((FileListItemView)itemView).isRadioMode()) {
            this.notifyDataSetChanged();
        }
    }

    public void setAllItemSelected(boolean selected) {
        if (mCurrentMode != Mode.Selecting)
            return;

        for (FileInfo fileInfo:mFileInfoList) {
            fileInfo.setSelected(selected);
        }
        this.notifyDataSetChanged();
    }

    // Search Mode
    public boolean isShowSearchButton() {
        return mIsShowSearchButton;
    }

    public void showSearchButton(boolean isShowSearchButton) {
        if (mCurrentMode != Mode.Normal) {
            isShowSearchButton = false;
        }

        if (mIsShowSearchButton != isShowSearchButton) {
            Log.d(TAG, "isShow:" + isShowSearchButton);
            mIsShowSearchButton = isShowSearchButton;
            this.notifyDataSetChanged();
        }
    }

    public void reSearch() {
        if (mCurrentMode != Mode.Searching || TextUtils.isEmpty(mSearchKey)) {
            return;
        }
        ArrayList<FileInfo> searchInfoList = new ArrayList<FileInfo>();
        for (FileInfo fileInfo:mFileInfoList) {
            if (fileInfo.getName().toUpperCase(Locale.getDefault()).contains(mSearchKey)) {
                searchInfoList.add(fileInfo);
            }
        }

        mSearchInfoList = searchInfoList.toArray(new FileInfo[0]);
        this.notifyDataSetChanged();
    }

    public void searchWithKey(String key) {
        if (mCurrentMode != Mode.Searching || mSearchKey.equals(key)) {
            return;
        }
        mSearchKey = key.toUpperCase(Locale.getDefault());
        ArrayList<FileInfo> searchInfoList = new ArrayList<FileInfo>();
        for (FileInfo fileInfo:mFileInfoList) {
            if (fileInfo.getName().toUpperCase(Locale.getDefault()).contains(mSearchKey)) {
                searchInfoList.add(fileInfo);
            }
        }

        mSearchInfoList = searchInfoList.toArray(new FileInfo[0]);
        this.notifyDataSetChanged();
    }

    public void setAnimationRegion(int beginPos, int endPos) {
        mFirstAnimationPos = beginPos;
        mLastAnimationPos = endPos;
        mAnimationSet.clear();
    }

    public void setNeedAnimation(boolean isNeeedAnimation) {
        mNeedAnimation = isNeeedAnimation;
        if(!mNeedAnimation) {
            mAnimationSet.clear();
        }
    }

    private void sortFileList() {
        if (mFileInfoList == null) {
            return;
        }

        Arrays.sort(mFileInfoList, new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo lObj, FileInfo rObj) {
                if (lObj.equals(rObj)) {
                    return 0;
                }

                if (lObj.isFolder() ^ rObj.isFolder()) {
                    return lObj.isFolder() ? -1 : 1;
                }

                long compareRet = 0;
                switch (mCurrentSortMode) {
                    case SortByName:
                     // add by huangjiawei at 20170710 sortByName include chinese begin
                    	String lObjString = PinYin.getPinYin(lObj.getName());
                    	String rObjString = PinYin.getPinYin(rObj.getName());
                        compareRet = lObjString.compareToIgnoreCase(rObjString);
                     // add by huangjiawei at 20170710 sortByName include chinese end
                        
                       // compareRet = lObj.getName().compareToIgnoreCase(rObj.getName());
                        if (compareRet == 0) {
                            compareRet = rObj.getLastModifyTime() - lObj.getLastModifyTime();
                        }
                        break;
                    case SortByDate:
                        compareRet = rObj.getLastModifyTime() - lObj.getLastModifyTime();
                        if (compareRet == 0) {
                            compareRet = lObj.getName().compareToIgnoreCase(rObj.getName());
                        }
                        break;
                    case SortBySize:
                        compareRet = lObj.getSize() - rObj.getSize();
                        if (compareRet == 0) {
                            compareRet = lObj.getName().compareToIgnoreCase(rObj.getName());
                        }
                        break;
                    case SortByType:
                        if (lObj.isFolder()) {
                            return lObj.getName().compareToIgnoreCase(rObj.getName());
                        }

                        int lExtIndex = lObj.getName().lastIndexOf(".");
                        int rExtIndex = rObj.getName().lastIndexOf(".");

                        boolean fso1NoType = lExtIndex < 0;
                        boolean fso2NoType = rExtIndex < 0;
                        if (fso1NoType || fso2NoType) {
                            return fso1NoType ? -1 : 1;
                        }

                        String lExt = lObj.getName().substring(lExtIndex);
                        String rExt = rObj.getName().substring(rExtIndex);

                        compareRet = lExt.compareToIgnoreCase(rExt);
                }

                if (compareRet > 0) {
                    return 1;
                } else if (compareRet < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}
