#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <iostream>
#include <sstream>
#include <fstream>
#include <cmath>
#include <ctime>

#include "FreeImage.h"
#include "FreeImage_Rescale.h"
#include "StringConverter_For_JNI.h"
#include "minizip/minizip.h"


#define  LOG_TAG    "libimage"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

enum VIEW_MODE {
    VIEW_MODE_AUTO_L=0,
    VIEW_MODE_AUTO_R,
    VIEW_MODE_SINGLE,
    VIEW_MODE_DOUBLE_L,
    VIEW_MODE_DOUBLE_R
};

enum RESIZE_MODE {
    RESIZE_MODE_FULL_RATE=0,
    RESIZE_MODE_WIDTH_RATE,
    RESIZE_MODE_HEIGHT_RATE
};

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

static void copy_pixels_flip_vertical(void* to, AndroidBitmapInfo*  info, void*  from, int fromStride, int x=0, int y=0, int fromWidth=0, int fromHeight=0)
{
    if(fromWidth==0)
        fromWidth = info->width;
    if(fromHeight==0)
        fromHeight = info->height;

    if(x<0 || y<0 || x+fromWidth>info->width || y+fromHeight>info->height)
    {
        LOGE("copy_pixels_flip_vertical : Over the range.(x:%d fromWidth:%d infoWidth:%d y:%d fromHeight:%d infoHeight:%d",x,fromWidth,info->width,y,fromHeight,info->height);
        return;
    }

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

    //FIBITMAP *rescaled = FreeImage_RescaleRect(dib, resizeWidth, resizeHeight, 0, 0, originWidth, originHeight, FILTER_BILINEAR);
    FIBITMAP *rescaled = FreeImage_Rescale(dib, resizeWidth, resizeHeight, FILTER_BILINEAR);
    FreeImage_Unload(dib);

    FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
    FreeImage_Unload(rescaled);

    FreeImage_GetPitch(dib565);
    fill_pixels(&info, pixels, make565(255,255,255));
    copy_pixels_flip_vertical(pixels, &info, FreeImage_GetBits(dib565), FreeImage_GetPitch(dib565), displayX, displayY, resizeWidth, resizeHeight);

    FreeImage_Unload(dib565);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("image_out ms : %g",now_ms()-starttime);
}

static int image_out2(JNIEnv *env, jobject bitmap, FIBITMAP *dib, int viewMode, int resizeMode, int viewPage)
{
    int status = -1;
    AndroidBitmapInfo info;
    void* pixels;
    int ret;
    double starttime = now_ms();

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return status;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565)
    {
        LOGE("Bitmap format is not RGB_565 !");
        return status;
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return status;
    }

    unsigned int originWidth = FreeImage_GetWidth(dib);
    unsigned int originHeight = FreeImage_GetHeight(dib);
    double originRate = static_cast<double>(originWidth)/static_cast<double>(originHeight);
    double displayRate = static_cast<double>(info.width)/static_cast<double>(info.height);
    LOGI("origin size : %d %d", originWidth, originHeight);

    unsigned int resizeWidth = 0;
    unsigned int resizeHeight = 0;
    unsigned int originX = 0;
    unsigned int originY = 0;
    unsigned int originX2 = 0;
    unsigned int originY2 = 0;
    unsigned int displayX = 0;
    unsigned int displayY = 0;

    bool isLeftPageFirst = true;
    bool isDoublePage = false;

    if( ((viewMode==VIEW_MODE_AUTO_L || viewMode==VIEW_MODE_AUTO_R) && originRate>1.1) ||
            (viewMode==VIEW_MODE_DOUBLE_L || viewMode==VIEW_MODE_DOUBLE_R) )
    {
        originWidth /= 2;
        originRate = static_cast<double>(originWidth)/static_cast<double>(originHeight);
        isDoublePage = true;

        if(viewMode==VIEW_MODE_AUTO_R || viewMode==VIEW_MODE_DOUBLE_R)
            isLeftPageFirst = false;
    }

    if(resizeMode==RESIZE_MODE_FULL_RATE)
    {
        if (displayRate > originRate) {
            // originWidth : originHeight = width : info.height
            // width = originWidth * info.height / originHeight
            resizeWidth = info.height * originRate;
            resizeHeight = info.height;
            displayX = (info.width - resizeWidth) / 2;
            displayY = 0;
        }
        else {
            // originWidth : originHeight = info.width : height
            // height = originHeight * info.width / originWidth
            resizeWidth = info.width;
            resizeHeight = info.width / originRate;
            displayX = 0;
            displayY = (info.height - resizeHeight) / 2;
        }
    }
    else if(resizeMode==RESIZE_MODE_WIDTH_RATE)
    {
        resizeWidth = info.width;
        resizeHeight = info.width / originRate;
        displayX = 0;
        displayY = (info.height - resizeHeight) / 2;
    }
    else if(resizeMode==RESIZE_MODE_HEIGHT_RATE)
    {
        resizeWidth = info.height * originRate;
        resizeHeight = info.height;
        displayX = (info.width - resizeWidth) / 2;
        displayY = 0;
    }
    else
    {
        resizeWidth = info.width;
        resizeHeight = info.height;
        displayX = 0;
        displayY = 0;
    }

    int viewCount=1;
    originX2 = originWidth;
    originY2 = originHeight;
    if(resizeHeight>info.height)
    {
        viewCount = ceil(static_cast<double>(resizeHeight) / static_cast<double>(info.height));
        resizeHeight = info.height;
        double originViewHeight = static_cast<double>(originHeight)/viewCount;
        int viewIndex=viewPage % viewCount;

        displayX = 0;
        displayY = 0;
        originY = originViewHeight*viewIndex;
        originY2 = originY + originViewHeight;
        if(originY2>originHeight)
        {
            originY = originHeight-originViewHeight;
            originY2 = originHeight;
        }
    }
    else if(resizeWidth>info.width)
    {
        viewCount = ceil(static_cast<double>(resizeWidth) / static_cast<double>(info.width));
        resizeWidth=info.width;
        double originViewWidth = static_cast<double>(originWidth)/viewCount;
        int viewIndex=viewPage % viewCount;

        displayX = 0;
        displayY = 0;
        originX = originViewWidth*viewIndex;
        originX2 = originX + originViewWidth;
        if(originX2>originWidth)
        {
            originX = originWidth-originViewWidth;
            originX2 = originViewWidth;
        }
    }

    // select left or right page
    if(isDoublePage)
    {
        bool isFirstPage = isLeftPageFirst;
        // next split page
        if(viewPage>=viewCount)
            isFirstPage = !isFirstPage;
        if(isFirstPage==false)
        {
            originX += originWidth;
            originX2 += originWidth;
        }
    }

    LOGI("origin xy-xy : %d %d %d %d", originX, originY, originX2, originY2);

    //FIBITMAP *dibTmp = FreeImage_Copy(dib, originX, originY, originX2, originY2);
    //FIBITMAP *rescaled = FreeImage_Rescale(dibTmp, resizeWidth, resizeHeight);
    //FreeImage_Unload(dibTmp);

    FIBITMAP *rescaled = FreeImage_RescaleRect(dib, resizeWidth, resizeHeight, originX, originY, originX2, originY2, FILTER_BILINEAR);
    FreeImage_Unload(dib);

    FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
    FreeImage_Unload(rescaled);

    FreeImage_GetPitch(dib565);
    fill_pixels(&info, pixels, make565(255,255,255));
    copy_pixels_flip_vertical(pixels, &info, FreeImage_GetBits(dib565), FreeImage_GetPitch(dib565), displayX, displayY, resizeWidth, resizeHeight);

    FreeImage_Unload(dib565);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("image_out ms : %g",now_ms()-starttime);

    return viewCount*(isDoublePage ? 2 : 1);
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

JNIEXPORT jboolean JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromMemory(JNIEnv *env, jobject obj,jobject bitmap,jbyteArray data,jint dataSize)
{
    jboolean isSuccees = JNI_FALSE;
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
        isSuccees = JNI_TRUE;
    }

    // not update
    env->ReleaseByteArrayElements(data, (jbyte*)byteData, JNI_ABORT);
    return isSuccees;
}

JNIEXPORT jint JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromPath(JNIEnv *env, jobject obj,jobject bitmap,jstring path, jint viewPage)
{
    jint status = -1;
    jboolean isCopy;
    const char * imgPath = env->GetStringUTFChars(path, &isCopy);
    if(imgPath==NULL)
        return status;

    FIBITMAP *dib = GenericLoader(imgPath, 0);
    if(dib)
    {
        LOGI("Load OK");
        status = image_out2(env,bitmap,dib,VIEW_MODE_AUTO_L,RESIZE_MODE_WIDTH_RATE,viewPage);
    }

    env->ReleaseStringUTFChars(path, imgPath);
    return status;
}

JNIEXPORT jint JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromZip(JNIEnv *env, jobject obj,jobject bitmap, jstring zipfileStr, jstring innerFileStr, jint viewPage)
{
    jboolean status = -1;
    jboolean isCopy;
    const char * zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    char * innerFilename = cstrFromJavaStringEucKR(env,innerFileStr);

    jbyte* byteData = NULL;
    jsize dataSize = getFileData(zipfilename, innerFilename, NULL, 0);
    if(dataSize>0)
    {
        byteData = (jbyte*)malloc(dataSize);
        jsize ret = getFileData(zipfilename, innerFilename, byteData, dataSize);
        if(ret==0)
        {
            FIMEMORY *hmem = FreeImage_OpenMemory((BYTE*) byteData, dataSize);
            if(hmem!=NULL)
            {
                FREE_IMAGE_FORMAT fif = FreeImage_GetFileTypeFromMemory(hmem, 0);
                FIBITMAP *dib = FreeImage_LoadFromMemory(fif, hmem, 0);
                FreeImage_CloseMemory(hmem);
                free(byteData);
                byteData = 0;

                if (dib) {
                    LOGI("Load OK");
                    status = image_out2(env,bitmap,dib,VIEW_MODE_AUTO_L,RESIZE_MODE_FULL_RATE,viewPage);
                }
            }
        }
    }

    // Release memory
    if(byteData)
        free(byteData);
    if(innerFilename)
        free(innerFilename);
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
    return status;
}

#ifdef __cplusplus
}
#endif