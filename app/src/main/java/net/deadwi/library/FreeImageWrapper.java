package net.deadwi.library;

import android.graphics.Bitmap;

public class FreeImageWrapper
{
    static {
        System.loadLibrary("plasma");
    }
    static private boolean isInit=false;
    final static public int RETURN_PAGE_UNIT = 100000;

    final static public int VIEW_MODE_SINGLE = 0;
    final static public int VIEW_MODE_AUTO_L = 1;
    final static public int VIEW_MODE_AUTO_R = 2;
    final static public int VIEW_MODE_DOUBLE_L = 3;
    final static public int VIEW_MODE_DOUBLE_R = 4;
    final static public int DISPLAY_FIT = 0;
    final static public int DISPLAY_WIDTH = 1;
    final static public int DISPLAY_HEIGHT = 2;
    final static public int RESIZE_METHOD_BOX = 0;
    final static public int RESIZE_METHOD_BILINEAR = 2;
    final static public int RESIZE_METHOD_BSPLINE = 3;
    final static public int RESIZE_METHOD_CATMULLROM = 4;
    final static public int RESIZE_METHOD_LANCZOS3 = 5;
    final static public String FILTER_GRAY_2BIT = "GRAY2";
    final static public String FILTER_ADJUST_COLOR = "AC";

    // int array index for getOutputImageArea
    final static public int AREA_INDEX_RESIZE_WIDTH = 0;
    final static public int AREA_INDEX_RESIZE_HEIGHT = 1;
    final static public int AREA_INDEX_ORIGIN_X = 2;
    final static public int AREA_INDEX_ORIGIN_Y = 3;
    final static public int AREA_INDEX_ORIGIN_X2 = 4;
    final static public int AREA_INDEX_ORIGIN_Y2 = 5;
    final static public int AREA_INDEX_DISPLAY_X = 6;
    final static public int AREA_INDEX_DISPLAY_Y = 7;
    final static public int AREA_INDEX_VIEW_COUNT = 8;
    final static public int AREA_INDEX_DOUBLE_PAGE = 9;

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

    static private native void initFreeImage();
    static private native void deInitFreeImage();
    static public native boolean loadImageFromMemory(Bitmap bitmap, Byte[] data, int dataSize);
    static public native int loadImageFromPath(Bitmap bitmap, String path, boolean isLastPage, int viewIndex, int optionViewMode, int optionResizeMode, int optionResizeMethod, String filterOption);
    static public native int loadImageFromZip(Bitmap bitmap, String zipPath, String path, boolean isLastPage, int viewIndex, int optionViewMode, int optionResizeMode, int optionResizeMethod, String filterOption);
    static public native int[] getOutputImageArea(boolean isLastPage, int viewIndex, int optionViewMode, int optionResizeMode, int srcWidth, int srcHeight, int viewWidth, int viewHeight);
    static public native int applyFilter(Bitmap bitmap, String filterOption);
}
