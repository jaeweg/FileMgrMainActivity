package com.aliyunos.filemanager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
    public static final int FILE_OPERATOR_SORT = 1000;
    public static final int FILE_OPERATOR_COPY = 1001;
    public static final int FILE_OPERATOR_DELETE = 1002;
    public static final int FILE_OPERATOR_CUT = 1003;
    public static final int FILE_OPERATOR_SHARE = 1004;
    public static final int FILE_OPERATOR_ZIP = 1005;
    public static final int FILE_OPERATOR_NEWFOLDER = 1006;
    public static final int FILE_OPERATOR_SHOW_HIDE_FILE = 1007;
    public static final int FILE_OPERATOR_HIDE_FILE = 1008;
    public static final int FILE_OPERATOR_PASTE = 1009;
    public static final int FILE_OPERATOR_COMPRESS = 1010;
    public static final int FILE_OPERATOR_REFRESH = 1011;
    public static final int FILE_OPERATOR_UPLOAD = 1012;
    public static final int FILE_OPERATOR_DOWNLOAD = 1013;
    public static final int FILE_OPERATOR_SHOW_DETAIL = 1014;
    public static final int FILE_OPERATOR_RENAME = 1015;
    public static final int FILE_OPERATOR_CRUSH = 1016;
    public static final int FILE_OPERATOR_UNZIP = 1017;
    public static final int FILE_OPERATOR_CANCEL = 1018;
    public static final int FILE_OPERATOR_CLOSE_CLOUD = 1019;

    // copy a file from srcFile to destFile, return true if succeed, return
    // false if fail
    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = copyToFile(in, destFile);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Copy data from a source stream to destFile. Return true if succeed,
     * return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static final String[] COMPRESSED_TAR = {
            "tar.gz", "tar.bz2", "tar.lzma" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    };

    /**
     * Method that returns the extension of a file system object.
     *
     * @param name The name of file system object
     * @return The extension of the file system object, or <code>null</code> if
     * <code>fso</code> has no extension.
     */
    public static String getExtension(String name) {
        final char dot = '.';
        int pos = name.lastIndexOf(dot);
        if (pos == -1 || pos == 0) { // Hidden files doesn't have extensions
            return null;
        }

        // Exceptions to the general extraction method
        int cc = COMPRESSED_TAR.length;
        for (int i = 0; i < cc; i++) {
            if (name.endsWith("." + COMPRESSED_TAR[i])) { //$NON-NLS-1$
                return COMPRESSED_TAR[i];
            }
        }

        // General extraction method
        return name.substring(pos + 1);
    }


    public static long listDirFileNum(File from,long BaseNum)
    {
        if(from.isFile())
        {
            BaseNum++;
        }
        else if(from.isDirectory())
        {
            File[] files = from.listFiles();
            if(files != null && files.length > 0)
            {
                for(File f : files)
                {
                    if(f.isFile())
                    {
                        BaseNum++;
                    }
                    else if(f.isDirectory())
                    {
                        BaseNum = listDirFileNum(f, BaseNum);
                    }
                }
            }
        }
        return BaseNum;
    }
}
