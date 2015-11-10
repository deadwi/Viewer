#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <iostream>
#include <sstream>
#include <fstream>

#include "FreeImage.h"
#include "StringConverter_For_JNI.h"
#include "minizip/minizip.h"


#define  LOG_TAG    "libimage"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


/** Generic image loader
	@param lpszPathName Pointer to the full file name
	@param flag Optional load flag constant
	@return Returns the loaded dib if successful, returns NULL otherwise
*/
FIBITMAP* GenericLoader(const char* lpszPathName, int flag)
{
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

// Return current time in milliseconds
static double now_ms()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec*1000. + tv.tv_usec/1000.;
}

static uint16_t make565(uint8_t red, uint8_t green, uint8_t blue)
{
    return ((red>>3)<<11) | ((green>>2)<<5) | (blue>>3);
}

static void copy_pixels( AndroidBitmapInfo*  info, void*  from, void* to)
{
    memcpy(to,from,info->height*info->stride);
}

static void fill_pixels( AndroidBitmapInfo*  info, void* to, uint16_t color, int x=0, int y=0, int width=0, int height=0)
{
    if(width==0)
        width = info->width;
    if(height==0)
        height = info->height;

    if(x<0 || y<0 || x+width>info->width || y+height>info->height)
    {
        LOGE("fill_pixels : Over the range.");
        return;
    }

    char* toRow = (char*)to + (info->stride*(height+y-1));
    for (int yy = 0; yy < height; yy++)
    {
        uint16_t* line = (uint16_t*)toRow+x;
        for (int xx = 0; xx < width; xx++)
            line[xx]=color;
        toRow = toRow - info->stride;
    }
}

static void copy_pixels_flip_vertical( AndroidBitmapInfo*  info, void*  from, void* to, int x=0, int y=0, int fromWidth=0, int fromHeight=0)
{
    if(fromWidth==0)
        fromWidth = info->width;
    if(fromHeight==0)
        fromHeight = info->height;

    if(x<0 || y<0 || x+fromWidth>info->width || y+fromHeight>info->height)
    {
        LOGE("copy_pixels_flip_vertical : Over the range.");
        return;
    }

    int fromStride = sizeof(uint16_t)*fromWidth;
    char* fromRow = (char*)from;
    char* toRow = (char*)to + (info->stride*(fromHeight+y-1));

    for (int yy = 0; yy < fromHeight; yy++)
    {
        memcpy((uint16_t*)toRow+x,fromRow,fromStride);
        fromRow = fromRow + fromStride;
        toRow = toRow - info->stride;
    }
}

static void image_out(JNIEnv *env, jobject bitmap, FIBITMAP *dib)
{
    AndroidBitmapInfo info;
    void* pixels;
    int ret;
    double starttime = now_ms();

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

    unsigned int originWidth = FreeImage_GetWidth(dib);
    unsigned int originHeight = FreeImage_GetHeight(dib);
    double originRate = static_cast<double>(originWidth)/static_cast<double>(originHeight);
    double displayRate = static_cast<double>(info.width)/static_cast<double>(info.height);

    unsigned int resizeWidth = 0;
    unsigned int resizeHeight = 0;
    unsigned int displayX = 0;
    unsigned int displayY = 0;
    if(displayRate > originRate)
    {
        // originWidth : originHeight = width : info.height
        // width = originWidth * info.height / originHeight
        resizeWidth = info.height * originRate;
        resizeHeight = info.height;
        displayX = (info.width-resizeWidth)/2;
        displayY = 0;
    }
    else
    {
        // originWidth : originHeight = info.width : height
        // height = originHeight * info.width / originWidth
        resizeWidth = info.width;
        resizeHeight =  info.width / originRate;
        displayX = 0;
        displayY = (info.height-resizeHeight)/2;
    }

    FIBITMAP *rescaled = FreeImage_Rescale(dib, resizeWidth, resizeHeight, FILTER_BILINEAR);
    FreeImage_Unload(dib);

    FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
    FreeImage_Unload(rescaled);

    fill_pixels(&info, pixels, make565(255,255,255));
    copy_pixels_flip_vertical(&info, FreeImage_GetBits(dib565), pixels, displayX, displayY, resizeWidth, resizeHeight);

    FreeImage_Unload(dib565);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("image_out ms : %g",now_ms()-starttime);
}


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_net_deadwi_library_FreeImageWrapper_initFreeImage(JNIEnv *env, jobject obj)
{
    FreeImage_Initialise();
    FreeImage_SetOutputMessage(FreeImageErrorHandler);
    LOGI("FreeImage %s, FIF count %d", FreeImage_GetVersion(), FreeImage_GetFIFCount());
}

JNIEXPORT void JNICALL Java_net_deadwi_library_FreeImageWrapper_deInitFreeImage(JNIEnv *env, jobject obj)
{
    FreeImage_DeInitialise();
}

JNIEXPORT void JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromPath(JNIEnv *env, jobject obj,jobject bitmap,jstring path)
{
    jboolean isCopy;
    const char * imgPath = env->GetStringUTFChars(path, &isCopy);
    if(imgPath==NULL)
        return;

    FIBITMAP *dib = GenericLoader(imgPath, 0);
    if(dib)
    {
        LOGI("Load OK");
        image_out(env,bitmap,dib);
    }

    env->ReleaseStringUTFChars(path, imgPath);
}

JNIEXPORT void JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromMemory(JNIEnv *env, jobject obj,jobject bitmap,jbyteArray data,jint dataSize)
{
    jboolean isCopy;
    BYTE* byteData = (BYTE*)env->GetByteArrayElements(data, &isCopy);
    jsize byteDataSize = env->GetArrayLength(data);

    FIMEMORY *hmem = FreeImage_OpenMemory(byteData, dataSize);
    FREE_IMAGE_FORMAT fif = FreeImage_GetFileTypeFromMemory(hmem, 0);
    FIBITMAP *dib = FreeImage_LoadFromMemory(fif, hmem, 0);
    FreeImage_CloseMemory(hmem);

    if(dib)
    {
        LOGI("Load OK");
        image_out(env,bitmap,dib);
    }

    // not update
    env->ReleaseByteArrayElements(data, (jbyte*)byteData, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromZip(JNIEnv *env, jobject obj,jobject bitmap, jstring zipfileStr, jstring innerFileStr)
{
    jboolean isCopy;
    const char * zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    char * innerFilename = cstrFromJavaStringEucKR(env,innerFileStr);

    jbyte* byteData = NULL;
    jsize dataSize = getFileData(zipfilename, innerFilename, NULL, 0);
    if(dataSize>0)
    {
        byteData = (jbyte*)malloc(dataSize);
        jsize status = getFileData(zipfilename, innerFilename, byteData, dataSize);
        if(status==0)
        {
            FIMEMORY *hmem = FreeImage_OpenMemory((BYTE*) byteData, dataSize);
            FREE_IMAGE_FORMAT fif = FreeImage_GetFileTypeFromMemory(hmem, 0);
            FIBITMAP *dib = FreeImage_LoadFromMemory(fif, hmem, 0);
            FreeImage_CloseMemory(hmem);
            free(byteData);
            byteData=0;

            if(dib)
            {
                LOGI("Load OK");
                image_out(env,bitmap,dib);
            }
        }
    }

    // Release memory
    if(byteData)
        free(byteData);
    if(innerFilename)
        free(innerFilename);
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
}

#ifdef __cplusplus
}
#endif