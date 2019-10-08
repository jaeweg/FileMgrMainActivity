package com.aliyunos.filemanager.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;

public class GraphicUtil {

    /**
     * This method converts dp unit to equivalent device specific value in
     * pixels.
     *
     * @param ctx
     *            The current context
     * @param dp
     *            A value in dp (Device independent pixels) unit
     * @return float A float value to represent Pixels equivalent to dp
     *         according to device
     */
    public static float convertDpToPixel(Context ctx, float dp) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * This method converts device specific pixels to device independent pixels.
     *
     * @param ctx
     *            The current context
     * @param px
     *            A value in px (pixels) unit
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(Context ctx, float px) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);

    }

    /**
     * Specifies a tint for this drawable.
     */
    public static void setTint(Drawable drawable, int tint) {
        try {
            Method setTintMethod = drawable.getClass().getMethod("setTint");
            setTintMethod.invoke(drawable, tint);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
