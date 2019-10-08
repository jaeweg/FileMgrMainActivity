package com.aliyunos.filemanager.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
/**
 * Created by icelake on 15/8/31.
 */
public class ShareUtil {

    private static final String TAG = "ShareUtil";

    /**
     * Enumeration of mime/type' categories
     */
    public enum MimeTypeCategory {
        /**
         * No category
         */
        NONE,
        /**
         * System file
         */
        SYSTEM,
        /**
         * Application, Installer, ...
         */
        APP,
        /**
         * Binary file
         */
        BINARY,
        /**
         * Text file
         */
        TEXT,
        /**
         * Document file (text, spreedsheet, presentation, pdf, ...)
         */
        DOCUMENT,
        /**
         * e-Book file
         */
        EBOOK,
        /**
         * Mail file (email, message, contact, calendar, ...)
         */
        MAIL,
        /**
         * Compressed file
         */
        COMPRESS,
        /**
         * Executable file
         */
        EXEC,
        /**
         * Database file
         */
        DATABASE,
        /**
         * Font file
         */
        FONT,
        /**
         * Image file
         */
        IMAGE,
        /**
         * Audio file
         */
        AUDIO,
        /**
         * Video file
         */
        VIDEO,
        /**
         * Security file (certificate, keys, ...)
         */
        SECURITY
    }

    /**
     * An internal class for holding the mime/type database structure
     */
    private static class MimeTypeInfo {
        MimeTypeInfo() {
            /** NON BLOCK **/
        }

        public MimeTypeCategory mCategory;
        public String mMimeType;
        public String mDrawable;

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime
                    * result
                    + ((this.mCategory == null) ? 0 : this.mCategory.hashCode());
            result = prime
                    * result
                    + ((this.mDrawable == null) ? 0 : this.mDrawable.hashCode());
            result = prime
                    * result
                    + ((this.mMimeType == null) ? 0 : this.mMimeType.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MimeTypeInfo other = (MimeTypeInfo) obj;
            if (this.mCategory != other.mCategory)
                return false;
            if (this.mDrawable == null) {
                if (other.mDrawable != null)
                    return false;
            } else if (!this.mDrawable.equals(other.mDrawable))
                return false;
            if (this.mMimeType == null) {
                if (other.mMimeType != null)
                    return false;
            } else if (!this.mMimeType.equals(other.mMimeType))
                return false;
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "MimeTypeInfo [mCategory=" + this.mCategory + //$NON-NLS-1$
                    ", mMimeType=" + this.mMimeType + //$NON-NLS-1$
                    ", mDrawable=" + this.mDrawable + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static Map<String, MimeTypeInfo> sMimeTypes;

    /**
     * Constructor of <code>MimeTypeHelper</code>.
     */
    private ShareUtil() {
        super();
    }


    public static final String getMimeType(Context context, String  Filename) {
        // Ensure that mime types are loaded
        if (sMimeTypes == null) {
            loadMimeTypes(context);
        }

        // Get the extension and delivery
        return getMimeTypeFromExtension(Filename);
    }
    /* YUNOS BEGIN PB */
    //##AliFileBrowser:jessie.yj@alibaba-inc.com
    //##:(8258469) ##date:2016/05/12
    //##description:get correct mimeType
    public static final MimeTypeInfo getMimeTypeInfo(Context context, String  Filename) {
        if (sMimeTypes == null) {
            loadMimeTypes(context);
        }
        String ext = FileUtil.getExtension(Filename);
        if (ext == null) {
                return null;
        }

        // Load from the database of mime types
        MimeTypeInfo mimeTypeInfo = sMimeTypes
            .get(ext.toLowerCase(Locale.ROOT));
        if (mimeTypeInfo == null) {
            return null;
        }
        return mimeTypeInfo;
    }
    /* YUNOS BEGIN END */
    private static final String getMimeTypeFromExtension(
            final String  Filename) {
        String ext = FileUtil.getExtension(Filename);
        if (ext == null) {
            return null;
        }

        // Load from the database of mime types
        MimeTypeInfo mimeTypeInfo = sMimeTypes
                .get(ext.toLowerCase(Locale.ROOT));
        if (mimeTypeInfo == null) {
            return null;
        }

        return mimeTypeInfo.mMimeType;
    }
    /**
     * Method that loads the mime type information.
     *
     * @param context
     *            The current context
     */
    // IMP! This must be invoked from the main activity creation
    public static synchronized void loadMimeTypes(Context context) {
        if (sMimeTypes == null) {
            try {
                // Load the mime/type database
                Properties mimeTypes = new Properties();
                mimeTypes.load(context.getResources().openRawResource(
                        R.raw.mime_types));

                // Parse the properties to an in-memory structure
                // Format: <extension> = <category> | <mime type> | <drawable>
                sMimeTypes = new HashMap<String, MimeTypeInfo>(mimeTypes.size());
                Enumeration<Object> e = mimeTypes.keys();
                while (e.hasMoreElements()) {
                    try {
                        String extension = (String) e.nextElement();
                        String data = mimeTypes.getProperty(extension);
                        String[] mimeData = data.split("\\|"); //$NON-NLS-1$

                        // Create a reference of MimeType
                        MimeTypeInfo mimeTypeInfo = new MimeTypeInfo();
                        mimeTypeInfo.mCategory = MimeTypeCategory
                                .valueOf(mimeData[0].trim());
                        mimeTypeInfo.mMimeType = mimeData[1].trim();
                        mimeTypeInfo.mDrawable = mimeData[2].trim();
                        sMimeTypes.put(extension, mimeTypeInfo);

                    } catch (Exception e2) {
                        /** NON BLOCK **/
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Fail to load mime types raw file.", e); //$NON-NLS-1$
            }
        }
    }

    @SuppressLint("ShowToast") 
    public static void shareDialog(Context mActivity, ArrayList<FileInfo> list) {

        ArrayList<Uri> uris = new ArrayList<Uri>();
        String mimeType = null;
        boolean isSameType = true;
        MimeTypeCategory isSameMimeType = null;
        MimeTypeInfo mimeTypeInfo = null;
        for (int i = 0; i < list.size(); i++) {
            File file = new File(list.get(i).getPath());
            /* YUNOS BEGIN PB */
            //##AliFileBrowser:jessie.yj@alibaba-inc.com
            //##:(8258469) ##date:2016/05/12
            //##description:get correct mimeType
            mimeTypeInfo = ShareUtil.getMimeTypeInfo(mActivity, file.getName());
            /* YUNOS BEGIN PB */
            //##AliFileBrowser:xianqiu.zbb@alibaba-inc.com
            //##:(8267675) ##date:2016/05/14
            //##description:filemanager will crash when sharing the file with .imy
            if (null != mimeTypeInfo){
                mimeType = mimeTypeInfo.mMimeType;
                Log.e(TAG, "shareDialog " + mimeTypeInfo.toString());
                if (null == isSameMimeType) {
                    isSameMimeType = mimeTypeInfo.mCategory;
                } else if (isSameMimeType != mimeTypeInfo.mCategory) {
                    isSameType = false;
                }
            }
            /* YUNOS BEGIN END */
            /* YUNOS BEGIN END */
            Uri u = Uri.fromFile(file);


            uris.add(u);
        }
        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                : android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(mimeType == null)
            mimeType = "application/octet-stream";
        if (multiple) {
            /* YUNOS BEGIN PB */
            //##AliFileBrowser:jessie.yj@alibaba-inc.com
            //##:(8258469) ##date:2016/05/12
            //##description:get correct mimeType
            if(isSameType){
                intent.setType(getMultiType(mimeType));
            }else{
                intent.setType("*/*");
            }
            /* YUNOS BEGIN END */
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        //bug101912
		// add by huangjiawei at 20170703 the Max of share number begin
        if (list.size() <= 200) {
        	mActivity.startActivity(Intent.createChooser(intent, mActivity.getText(R.string.actions_title_Share)));
		}else {
			Toast.makeText(mActivity, mActivity.getString(R.string.doov_share_limit), Toast.LENGTH_LONG).show();
		}
        // add by huangjiawei at 20170703 the Max of share number end
    }
    /* YUNOS BEGIN PB */
    //##AliFileBrowser:jessie.yj@alibaba-inc.com
    //##:(8258469) ##date:2016/05/12
    //##description:get correct mimeType
    private static  String getMultiType(String mimeType){
        String multiType = mimeType;
        String sub = multiType.substring(multiType.indexOf("/") + 1);
        multiType = multiType.replace(sub,"*");
        return multiType;
    }
    /* YUNOS BEGIN END */
}
