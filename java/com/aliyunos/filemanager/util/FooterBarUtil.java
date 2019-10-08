package com.aliyunos.filemanager.util;

import com.aliyunos.filemanager.R;

public class FooterBarUtil {
    public static class FooterItemInfo {
        public int itemId;
        public int textId;
        public int iconId;

        public FooterItemInfo(int id, int text, int icon) {
            this.itemId = id;
            this.textId = text;
            this.iconId = icon;
        }
    }

    public static FooterItemInfo[] getItemsByIds(int[] ids) {
        FooterItemInfo[] itemInfos = new FooterItemInfo[ids.length];
        for (int i = 0; i < ids.length; i++) {
            itemInfos[i] = getItemById(ids[i]);
        }
        return itemInfos;
    }

    public static FooterItemInfo getItemById(int id) {
        FooterItemInfo info = null;
        switch (id) {
            case FileUtil.FILE_OPERATOR_SORT:
                info = new FooterItemInfo(id, R.string.actions_menu_Sort, R.drawable.sort_menu);
                break;
            case FileUtil.FILE_OPERATOR_COPY:
                info = new FooterItemInfo(id, R.string.actions_menu_Copy, R.drawable.copy_menu);
                break;
            case FileUtil.FILE_OPERATOR_DELETE:
                info = new FooterItemInfo(id, R.string.actions_menu_Delete, R.drawable.delete_menu);
                break;
            case FileUtil.FILE_OPERATOR_CUT:
                info = new FooterItemInfo(id, R.string.actions_menu_Cut, R.drawable.cut_menu);
                break;
            case FileUtil.FILE_OPERATOR_SHARE:
                info = new FooterItemInfo(id, R.string.actions_menu_Share, R.drawable.share_menu);
                break;
            case FileUtil.FILE_OPERATOR_ZIP:
                info = new FooterItemInfo(id, R.string.actions_menu_Zip, R.drawable.zip_menu);
                break;
            case FileUtil.FILE_OPERATOR_NEWFOLDER:
                info = new FooterItemInfo(id, R.string.actions_menu_NewDir, R.drawable.more_menu);
                break;
            case FileUtil.FILE_OPERATOR_COMPRESS:
                info = new FooterItemInfo(id, R.string.ali_zip, R.drawable.zip_menu);
                break;
            case FileUtil.FILE_OPERATOR_PASTE:
                info = new FooterItemInfo(id, R.string.actions_menu_Paste, R.drawable.paste_menu);
                break;
            case FileUtil.FILE_OPERATOR_SHOW_HIDE_FILE:
                info = new FooterItemInfo(id, R.string.actions_menu_ShowHideFile, R.drawable.more_menu);
                break;
            case FileUtil.FILE_OPERATOR_HIDE_FILE:
                info = new FooterItemInfo(id, R.string.actions_menu_NotShowHideFile, R.drawable.more_menu);
                break;
            case FileUtil.FILE_OPERATOR_REFRESH:
                info = new FooterItemInfo(id, R.string.actions_menu_Refresh, R.drawable.ic_renovate_normal);
                break;
            case FileUtil.FILE_OPERATOR_UPLOAD:
                info = new FooterItemInfo(id, R.string.upload, R.drawable.upload_menu);
                break;
            case FileUtil.FILE_OPERATOR_DOWNLOAD:
                info = new FooterItemInfo(id, R.string.download, R.drawable.download_menu);
                break;
            case FileUtil.FILE_OPERATOR_UNZIP:
                info = new FooterItemInfo(id, R.string.ali_unzip, R.drawable.unzip_menu);
                break;
            case FileUtil.FILE_OPERATOR_CANCEL:
                info = new FooterItemInfo(id, R.string.cancel, R.drawable.cancel_menu);
                break;
            case FileUtil.FILE_OPERATOR_CLOSE_CLOUD:
                info = new FooterItemInfo(id, R.string.actions_menu_CloseCloud, R.drawable.cancel_menu);
                break;
        }
        return info;
    }

}
