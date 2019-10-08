package com.aliyunos.filemanager.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.aliyunos.filemanager.R;
import hwdroid.widget.item.TextItem;
import hwdroid.widget.itemview.ItemView;

public class FileListItem extends TextItem {
    public Drawable leftDrawable;
    public Drawable rightDrawable;
    public ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_INSIDE;
    public String appName;

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.filelist_item_view, parent);
    }
}
