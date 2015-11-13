package net.deadwi.library;

import android.graphics.Bitmap;

public class FreeImageWrapper
{
    static {
        System.loadLibrary("plasma");
    }
    static private boolean isInit=false;

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
    static public native boolean loadImageFromPath(Bitmap  bitmap, String path);
    static public native boolean loadImageFromZip(Bitmap  bitmap, String zipPath, String path);
    static public native boolean loadImageFromMemory(Bitmap  bitmap, Byte[] data, int dataSize);
}
