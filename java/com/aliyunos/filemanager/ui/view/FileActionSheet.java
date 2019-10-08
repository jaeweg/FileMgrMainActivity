package com.aliyunos.filemanager.ui.view;

import android.content.Context;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.util.FileUtil;

import java.util.Arrays;

import hwdroid.widget.ActionSheet;

public class FileActionSheet extends ActionSheet {
    private Context mContext;

    public FileActionSheet(Context context) {
        super(context);
        mContext = context;
    }

    public void showMenu(final int[] actions, final CommonButtonListener listener) {
        String[] items = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            int action = actions[i];
            switch (action) {
                case FileUtil.FILE_OPERATOR_SHOW_DETAIL:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Detail);
                    break;
                case FileUtil.FILE_OPERATOR_RENAME:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Rename);
                    break;
                case FileUtil.FILE_OPERATOR_COPY:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Copy);
                    break;
                case FileUtil.FILE_OPERATOR_DELETE:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Delete);
                    break;
                case FileUtil.FILE_OPERATOR_CUT:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Cut);
                    break;
                case FileUtil.FILE_OPERATOR_ZIP:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Zip);
                    break;
                case FileUtil.FILE_OPERATOR_SHARE:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Share);
                    break;
                case FileUtil.FILE_OPERATOR_CRUSH:
                    items[i] = mContext.getResources().getString(R.string.actions_menu_Crush);
                    break;
            }
        }

        super.setCommonButtons(Arrays.asList(items), null, null, listener);
        super.showWithDialog();
    }
}
