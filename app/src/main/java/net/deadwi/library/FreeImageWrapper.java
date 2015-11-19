package net.deadwi.library;

import android.graphics.Bitmap;

import net.deadwi.viewer.Option;

public class FreeImageWrapper
{
    static {
        System.loadLibrary("plasma");
    }
    static private boolean isInit=false;
    final static public int RETURN_PAGE_UNIT = 100000;

    static public void init()
    {
        if(isInit==false)
        {
            initFreeImage();
            isInit=true;
        }
    }
    static public void deInit()
    {
        if(isInit==true)
        {
            deInitFreeImage();
            isInit=false;
        }
    }

    static public int getOptionViewMode(boolean isReadLeftToRight, int splitOption)
    {
        switch (splitOption)
        {
            case Option.SPLIT_SINGLE:
                return 0;
            case Option.SPLIT_AUTO:
                return isReadLeftToRight ? 1 : 2;
            case Option.SPLIT_DOUBLE:
                return isReadLeftToRight ? 3 : 4;
        }
        return 0;
    }

    static public int getOptionResizeMode(int displayOption)
    {
        switch (displayOption)
        {
            case Option.DISPLAY_FIT:
                return 0;
            case Option.DISPLAY_WIDTH:
                return 1;
            case Option.DISPLAY_HEIGHT:
                return 2;
        }
        return 0;
    }

    static public int getOptionResizeMethod(int resizeMethod)
    {
        switch (resizeMethod)
        {
            case Option.RESIZE_METHOD_BOX:
                return 0;
            case Option.RESIZE_METHOD_BILINEAR:
                return 2;
            case Option.RESIZE_METHOD_BSPLINE:
                return 3;
            case Option.RESIZE_METHOD_CATMULLROM:
                return 4;
            case Option.RESIZE_METHOD_LANCZOS3:
                return 5;
        }
        return 0;
    }

    static private native void initFreeImage();
    static private native void deInitFreeImage();
    static public native boolean loadImageFromMemory(Bitmap  bitmap, Byte[] data, int dataSize);
    static public native int loadImageFromPath(Bitmap  bitmap, String path, boolean isLastPage, int viewIndex, int optionViewMode, int optionResizeMode, int optionResizeMethod);
    static public native int loadImageFromZip(Bitmap  bitmap, String zipPath, String path, boolean isLastPage, int viewIndex, int optionViewMode, int optionResizeMode, int optionResizeMethod);
}
