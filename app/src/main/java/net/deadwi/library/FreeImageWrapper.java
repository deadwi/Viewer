package net.deadwi.library;

import android.graphics.Bitmap;

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

    static private native void initFreeImage();
    static private native void deInitFreeImage();
    static public native boolean loadImageFromMemory(Bitmap  bitmap, Byte[] data, int dataSize);
    static public native int loadImageFromPath(Bitmap  bitmap, String path, boolean isLastPage, int viewIndex);
    static public native int loadImageFromZip(Bitmap  bitmap, String zipPath, String path, boolean isLastPage, int viewIndex);
}
