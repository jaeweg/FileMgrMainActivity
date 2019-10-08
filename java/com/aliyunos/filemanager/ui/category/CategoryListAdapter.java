package com.aliyunos.filemanager.ui.category;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.kanbox.filemgr.KbxLocalFileManager;

public class CategoryListAdapter extends BaseAdapter {
    private Context mContext;

    int [][] ItemSet = {
            {KbxLocalFileManager.CategoryType.Image.ordinal(),
                R.drawable.ic_filemanager_photo_normal,
                R.string.CategoryItemPic,
                R.drawable.color_image},
            {KbxLocalFileManager.CategoryType.Video.ordinal(),
                R.drawable.ic_filemanager_video_normal,
                R.string.CategoryItemVideo,
                R.drawable.color_video},
            {KbxLocalFileManager.CategoryType.Audio.ordinal(),
                R.drawable.ic_filemanager_music_normal,
                R.string.CategoryItemMusic,
                R.drawable.color_audio},
            {KbxLocalFileManager.CategoryType.Apk.ordinal(),
                R.drawable.ic_filemanager_apk_normal,
                R.string.CategoryItemApk,
                R.drawable.color_apk},
            {KbxLocalFileManager.CategoryType.Zip.ordinal(),
                R.drawable.ic_filemanager_zip_normal,
                R.string.CategoryItemZip,
                R.drawable.color_zip},
            {KbxLocalFileManager.CategoryType.Doc.ordinal(),
                R.drawable.ic_filemanager_file_normal,
                R.string.CategoryItemText,
                R.drawable.color_document}
    };

    class ViewHolder {
        TextView text;
        ImageView icon;
        TextView num;
    }

    public CategoryListAdapter(Context ctx) {
        mContext = ctx;
    }

    @Override
    public int getCount() {
        return ItemSet.length;
    }

    @Override
    public Object getItem(int position) { return position; }

    @Override
    public long getItemId(int id) { return id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.category_mainview_item, null);

            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.category_item_text);
            holder.icon = (ImageView) convertView.findViewById(R.id.category_item_image);
            holder.num = (TextView) convertView.findViewById(R.id.category_item_num);
            convertView.setTag(holder);

            // GridView的Item高度需要手工设置
            LayoutParams param = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    mContext.getResources().getDimensionPixelSize(R.dimen.category_mainview_item_height));
            convertView.setLayoutParams(param);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int[] item = ItemSet[position];
        int item_id = item[0];
        int image_res = item[1];
        int text_res = item[2];
        KbxLocalFileManager.CategoryType categoryType =
                KbxLocalFileManager.CategoryType.values()[position];

        holder.icon.setImageResource(image_res);
        holder.text.setText(mContext.getResources().getString(text_res));
        holder.num.setText(String.valueOf(FileMgrCore.getCategoryFileCount(categoryType)));

        convertView.setId(item_id);
        return convertView;
    }
}
