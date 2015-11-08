#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <iostream>
#include <sstream>
#include <fstream>

#include "FreeImage.h"

#define  LOG_TAG    "libplasma"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


/** Generic image loader
	@param lpszPathName Pointer to the full file name
	@param flag Optional load flag constant
	@return Returns the loaded dib if successful, returns NULL otherwise
*/
FIBITMAP* GenericLoader(const char* lpszPathName, int flag) {
    FREE_IMAGE_FORMAT fif = FIF_UNKNOWN;

    // check the file signature and deduce its format
    // (the second argument is currently not used by FreeImage)
    fif = FreeImage_GetFileType(lpszPathName, 0);
    if(fif == FIF_UNKNOWN) {
        // no signature ?
        // try to guess the file format from the file extension
        fif = FreeImage_GetFIFFromFilename(lpszPathName);
    }
    LOGI("FIF : %d", fif);

    // check that the plugin has reading capabilities ...
    if((fif != FIF_UNKNOWN) && FreeImage_FIFSupportsReading(fif)) {
        // ok, let's load the file
        FIBITMAP *dib = FreeImage_Load(fif, lpszPathName, flag);
        // unless a bad file format, we are done !
        return dib;
    }
    LOGI("Not support format : %d", fif);
    return NULL;
}

/**
	FreeImage error handler
	@param fif Format / Plugin responsible for the error
	@param message Error message
*/
void FreeImageErrorHandler(FREE_IMAGE_FORMAT fif, const char *message)
{
    std::string out;
    if(fif != FIF_UNKNOWN)
    {
        out += FreeImage_GetFormatFromFIF(fif);
        out += " Format ";
    }
    out += message;
    LOGE(out.c_str());
}

static uint16_t  make565(int red, int green, int blue)
{
    return (uint16_t)( ((red   << 8) & 0xf800) |
                       ((green << 2) & 0x03e0) |
                       ((blue  >> 3) & 0x001f) );
}

static void fill_plasma( AndroidBitmapInfo*  info, void*  pixels)
{
    int  yy;
    for (yy = 0; yy < info->height; yy++)
    {
        uint16_t*  line = (uint16_t*)pixels;
        int xx;
        for (xx = 0; xx < info->width; xx++)
        {
            line[xx] = make565(255,255,255);
        }
        // go to next line
        pixels = (char*)pixels + info->stride;
    }
}

static void copy_pixels( AndroidBitmapInfo*  info, void*  from, void* to)
{
    memcpy(to,from,info->height*info->stride);
    /*
    int  yy;
    for (yy = 0; yy < info->height; yy++)
    {
        uint16_t* lineFrom = (uint16_t*)from;
        uint16_t* lineTo = (uint16_t*)to;

        int xx;
        for (xx = 0; xx < info->width; xx++)
        {
            lineTo[xx] = lineFrom[xx];
        }
        // go to next line
        from = (char*)from + info->stride;
        to = (char*)to + info->stride;
    }
     */
}


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_net_deadwi_viewer_FastImage_renderPlasma2(JNIEnv *env, jobject obj,jobject bitmap,jstring path)
{
    jboolean isCopy;
    const char * _imgPath = env->GetStringUTFChars(path, &isCopy);
    if(_imgPath==NULL)
        return;
    std::string imgPath = _imgPath;
    env->ReleaseStringUTFChars(path, _imgPath);

    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565)
    {
        LOGE("Bitmap format is not RGB_565 !");
        return;
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    // work
    FreeImage_Initialise();
    FreeImage_SetOutputMessage(FreeImageErrorHandler);
    LOGI("FreeImage %s, FIF count %d", FreeImage_GetVersion(), FreeImage_GetFIFCount());
    FIBITMAP *dib = GenericLoader(imgPath.c_str(), 0);
    if(dib)
    {
        LOGI("Load OK");
        FIBITMAP *rescaled = FreeImage_Rescale(dib, info.width, info.height);
        FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
        FreeImage_FlipVertical(dib565);

        copy_pixels(&info, FreeImage_GetBits(dib565), pixels);

        FreeImage_Unload(dib565);
        FreeImage_Unload(rescaled);
        FreeImage_Unload(dib);
    }

    FreeImage_DeInitialise();

    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL Java_net_deadwi_viewer_FastImage_renderPlasma3(JNIEnv *env, jobject obj,jobject bitmap,jbyteArray data,jint dataSize)
{
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565)
    {
        LOGE("Bitmap format is not RGB_565 !");
        return;
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    jboolean isCopy;
    BYTE* byteData = (BYTE*)env->GetByteArrayElements(data, &isCopy);
    jsize byteDataSize = env->GetArrayLength(data);

    // work
    FreeImage_Initialise();
    FreeImage_SetOutputMessage(FreeImageErrorHandler);
    LOGI("FreeImage %s, FIF count %d", FreeImage_GetVersion(), FreeImage_GetFIFCount());

    FIMEMORY *hmem = FreeImage_OpenMemory(byteData, dataSize);
    FREE_IMAGE_FORMAT fif = FreeImage_GetFileTypeFromMemory(hmem, 0);
    FIBITMAP *dib = FreeImage_LoadFromMemory(fif, hmem, 0);
    if(dib)
    {
        LOGI("Load OK");
        FIBITMAP *rescaled = FreeImage_Rescale(dib, info.width, info.height);
        FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
        FreeImage_FlipVertical(dib565);

        copy_pixels(&info, FreeImage_GetBits(dib565), pixels);

        FreeImage_Unload(dib565);
        FreeImage_Unload(rescaled);
        FreeImage_Unload(dib);
    }
    FreeImage_CloseMemory(hmem);

    FreeImage_DeInitialise();

    AndroidBitmap_unlockPixels(env, bitmap);
    // not update
    env->ReleaseByteArrayElements(data, (jbyte*)byteData, JNI_ABORT);
}


JNIEXPORT void JNICALL Java_net_deadwi_viewer_FastImage_loadImage(JNIEnv * env, jobject, jstring path)
{
    jboolean isCopy;
    const char * _imgPath = env->GetStringUTFChars(path, &isCopy);
    if(_imgPath==NULL)
        return;
    std::string imgPath = _imgPath;
    env->ReleaseStringUTFChars(path, _imgPath);

    FreeImage_Initialise();
    FreeImage_SetOutputMessage(FreeImageErrorHandler);
    LOGI("FreeImage %s, FIF count %d", FreeImage_GetVersion(), FreeImage_GetFIFCount());
    LOGI("Try to load : %s", imgPath.c_str());

    FIBITMAP *dib = GenericLoader(imgPath.c_str(), 0);
    if(dib)
    {
        LOGI("Load OK");
        FreeImage_Unload(dib);
    }

    FreeImage_DeInitialise();
}
#ifdef __cplusplus
}
#endif