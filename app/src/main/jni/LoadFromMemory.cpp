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
#include "split.h"
#include "interpret_cast.h"

#define  LOG_TAG    "libimage"
#ifdef NDEBUG
#define  LOGI(...)
#define  LOGE(...)
#else
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif

const int RETURN_PAGE_UNIT = 100000;

enum RETURN_CODE {
    RETURN_CODE_OK = 0,
    RETURN_CODE_FAIL_UNKOWN = -1,
    RETURN_CODE_UNKOWN_TYPE = -2,
    RETURN_CODE_UNSUPPORT_TYPE = -3,
    RETURN_CODE_FAIL_TO_LOADIMAGE = -4,
};

enum VIEW_MODE {
    VIEW_MODE_SINGLE=0,
    VIEW_MODE_AUTO_L,
    VIEW_MODE_AUTO_R,
    VIEW_MODE_DOUBLE_L,
    VIEW_MODE_DOUBLE_R
};

enum RESIZE_MODE {
    RESIZE_MODE_FULL_RATE=0,
    RESIZE_MODE_WIDTH_RATE,
    RESIZE_MODE_HEIGHT_RATE
};

static const char* FILTER_GRAY_2BIT = "GRAY2";
static const char* FILTER_ADJUST_COLOR = "AC";

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

inline static uint16_t make565(uint8_t red, uint8_t green, uint8_t blue)
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

inline static uint8_t get_red_code_from_565(uint16_t c)
{
    return ((((c & FI16_565_RED_MASK) >> FI16_565_RED_SHIFT) * 0xFF) / 0x1F);
}

inline static uint8_t get_green_code_from_565(uint16_t c)
{
    return ((((c & FI16_565_GREEN_MASK) >> FI16_565_GREEN_SHIFT) * 0xFF) / 0x3F);
}

inline static uint8_t get_blue_code_from_565(uint16_t c)
{
    return ((((c & FI16_565_BLUE_MASK) >> FI16_565_BLUE_SHIFT) * 0xFF) / 0x1F);
}

inline static uint8_t get_gray_code_from_565(uint16_t c)
{
    // http://www.w3.org/Graphics/Color/sRGB
    uint8_t g =  0.2126f * get_red_code_from_565(c) +
                 0.7152f * get_green_code_from_565(c) +
                 0.0722f * get_blue_code_from_565(c);
    return g;
}

inline static uint16_t get_gray_color_from_565(uint16_t c)
{
    uint8_t g =  get_gray_code_from_565(c);
    return make565(g,g,g);
}

inline static float get_saturation(uint8_t r,uint8_t g,uint8_t b)
{
    float min = std::min( std::min(r, g), b);
    float max = std::max( std::max(r, g), b);
    if(max == 0)
        return 0;
    return (max-min) / max;
}

inline static float get_saturation(uint16_t c)
{
    uint8_t r = get_red_code_from_565(c);
    uint8_t g = get_green_code_from_565(c);
    uint8_t b = get_blue_code_from_565(c);
    float min = std::min( std::min(r, g), b);
    float max = std::max( std::max(r, g), b);
    if(max == 0)
        return 0;
    return (max-min) / max;
}

inline static void get_HSV_from_565(uint16_t c, float& h, float& s, float& v)
{
    uint8_t r = get_red_code_from_565(c);
    uint8_t g = get_green_code_from_565(c);
    uint8_t b = get_blue_code_from_565(c);
    float min = std::min( std::min(r, g), b);
    float max = std::max( std::max(r, g), b);
    float delta = max - min;
    if( max != 0 )
        s = delta / max;
    else
    {
        s = 0;
        h = -1;
        return;
    }
    if( r == max )
        h = ( g - b ) / delta;		// between yellow & magenta
    else if( g == max )
        h = 2 + ( b - r ) / delta;	// between cyan & yellow
    else
        h = 4 + ( r - g ) / delta;	// between magenta & cyan
    h *= 60;				// degrees
    if( h < 0 )
        h += 360;
}

static void convert_grayscale(FIBITMAP *dib565)
{
    const unsigned width = FreeImage_GetWidth(dib565);
    const unsigned height = FreeImage_GetHeight(dib565);
    for (unsigned rows = 0; rows < height; rows++)
    {
        uint16_t* line = (uint16_t*)FreeImage_GetScanLine(dib565, rows);
        for (int xx = 0; xx < width; xx++)
        {
            line[xx] = get_gray_color_from_565(line[xx]);
        }
    }
}

static uint8_t get_auto_grayscale_1bit_threshold(FIBITMAP *dib565)
{
    const int GRAY_SIZE = 256;
    unsigned v[GRAY_SIZE] = {0};
    const unsigned width = FreeImage_GetWidth(dib565);
    const unsigned height = FreeImage_GetHeight(dib565);
    for (unsigned rows = 0; rows < height; rows++)
    {
        uint16_t* line = (uint16_t*)FreeImage_GetScanLine(dib565, rows);
        for (int xx = 0; xx < width; xx++)
        {
            uint8_t g = get_gray_code_from_565(line[xx]);
            v[g]++;
        }
    }

    int ma = 40;
    unsigned mv[GRAY_SIZE] = {0};
    for(int i=ma;i<GRAY_SIZE;i++)
    {
        mv[i]=0;
        for(int j=0;j<ma;j++)
            mv[i-j] += v[i];
    }

    double total = width*height;
    double grade;
    double maxGrade=0;
    uint8_t maxGray=0;
    for(int i=ma;i<GRAY_SIZE;i++)
    {
        grade = static_cast<double>(mv[i]==0 ? 1 : mv[i])/static_cast<double>(mv[i-1]==0 ? 1 : mv[i-1]);
        if(grade > maxGrade && mv[i]/total>0.001)
        {
            //LOGE("%d %d %g",i,mv[i],grade);
            maxGrade = grade;
            maxGray = static_cast<uint8_t>(i);
        }
    }

    LOGI("auto grayscale threshold : %d (v=%g)",maxGray,maxGrade);
    return maxGray;
}

static void convert_grayscale_1bit(FIBITMAP *dib565, uint8_t threshold)
{
    const unsigned width = FreeImage_GetWidth(dib565);
    const unsigned height = FreeImage_GetHeight(dib565);

    if(threshold>=255)
        threshold = get_auto_grayscale_1bit_threshold(dib565);

    for (unsigned rows = 0; rows < height; rows++)
    {
        uint16_t* line = (uint16_t*)FreeImage_GetScanLine(dib565, rows);
        for (int xx = 0; xx < width; xx++)
        {
            uint8_t g = get_gray_code_from_565(line[xx]);
            if(g>=threshold)
                line[xx] = make565(255,255,255);
            else
                line[xx] = make565(0,0,0);
        }
    }
}

static void convert_adjust_colors(FIBITMAP *dib565, int brightness, int contrast, int invert)
{
    if(brightness==0 && contrast==0 && invert!=0)
        FreeImage_Invert(dib565);
    else
    {
        FIBITMAP *dib24 = FreeImage_ConvertTo24Bits(dib565);
        FreeImage_AdjustColors(dib24, brightness, contrast, 1.0, invert == 0 ? FALSE : TRUE);

        FIBITMAP *tdib565 = FreeImage_ConvertTo16Bits565(dib24);
        FreeImage_Unload(dib24);

        FreeImage_Paste(dib565, tdib565, 0, 0, 256);
        FreeImage_Unload(tdib565);
    }
}

static void convert_grayscale_for_black_text(FIBITMAP *dib)
{
    RGBQUAD rgb;
    RGBQUAD white;
    white.rgbRed=255;
    white.rgbGreen=255;
    white.rgbBlue=255;

    const unsigned width = FreeImage_GetWidth(dib);
    const unsigned height = FreeImage_GetHeight(dib);
    for (unsigned yy = 0; yy < height; yy++)
    {
        for (int xx = 0; xx < width; xx++)
        {
            FreeImage_GetPixelColor(dib, xx, yy, &rgb);

            if(get_saturation(rgb.rgbRed,rgb.rgbGreen,rgb.rgbBlue)<0.05)
                FreeImage_SetPixelColor(dib, xx, yy, &rgb);
            else
                FreeImage_SetPixelColor(dib, xx, yy, &white);
        }
    }
}

static void image_filter(FIBITMAP *dib565,const char * filterOption)
{
    // bpp : 16
    // FREE_IMAGE_TYPE : FIT_BITMAP
    LOGI("option : %s",filterOption);

    std::vector<std::string> fset;
    std::vector<std::string> argset;
    split(filterOption,' ',fset);
    for(size_t i=0;i<fset.size();i++)
    {
        argset.clear();
        split(fset[i],'|',argset);
        if(argset.empty())
            continue;
        if(argset[0]==FILTER_GRAY_2BIT && argset.size()==2)
        {
            uint8_t v = interpret_cast<int>(argset[1]);
            convert_grayscale_1bit(dib565,v);
        }
        else if(argset[0]==FILTER_ADJUST_COLOR && argset.size()==4)
        {
            int brightness = interpret_cast<int>(argset[1]);
            int contrast = interpret_cast<int>(argset[2]);
            int invert = interpret_cast<int>(argset[3]);
            convert_adjust_colors(dib565,brightness,contrast,invert);
        }
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

    fill_pixels(&info, pixels, make565(255,255,255));
    copy_pixels_flip_vertical(pixels, &info, FreeImage_GetBits(dib565), FreeImage_GetPitch(dib565), displayX, displayY, resizeWidth, resizeHeight);

    FreeImage_Unload(dib565);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("image_out ms : %g",now_ms()-starttime);
}

static int image_out2(JNIEnv *env, jobject bitmap, FIBITMAP *dib, int viewMode, int resizeMode, int resizeMethod, bool isLastPage, int viewIndex, double nextGapRate=0.1,const char * filterOption=NULL)
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

    LOGI("Out resize : %d %d in (%d %d)", resizeWidth, resizeHeight, info.width, info.height);

    int viewCount=1;
    originX2 = originWidth;
    originY2 = originHeight;
    if(resizeHeight>info.height) // RESIZE_MODE_WIDTH_RATE
    {
        double originViewHeight = static_cast<double>(info.height) * (static_cast<double>(originWidth)/static_cast<double>(info.width));
        viewCount = ceil( static_cast<double>(resizeHeight) / ((1.0-nextGapRate)*info.height) );
        resizeHeight = info.height;

        displayX = 0;
        displayY = 0;
        originY = ((1.0-nextGapRate)*originViewHeight)*(viewIndex % viewCount);
        originY2 = originY + originViewHeight;
        if(originY2>originHeight)
        {
            originY = originHeight-originViewHeight;
            originY2 = originHeight;
        }
    }
    else if(resizeWidth>info.width) // RESIZE_MODE_HEIGHT_RATE
    {
        double originViewWidth = static_cast<double>(info.width) * (static_cast<double>(originHeight)/static_cast<double>(info.height));
        viewCount = ceil(static_cast<double>(resizeWidth) / ((1.0-nextGapRate)*info.width) );
        resizeWidth = info.width;

        displayX = 0;
        displayY = 0;
        originX = ((1.0-nextGapRate)*originViewWidth)*(viewIndex % viewCount);
        originX2 = originX + originViewWidth;
        if(originX2>originWidth)
        {
            originX = originWidth-originViewWidth;
            originX2 = originWidth;
        }
    }

    // select left or right page
    if(isDoublePage)
    {
        bool isFirstPage = isLeftPageFirst;
        // next split page
        if(viewIndex>=viewCount || isLastPage==true)
            isFirstPage = !isFirstPage;
        if(isFirstPage==false)
        {
            originX += originWidth;
            originX2 += originWidth;
        }
    }

    LOGI("origin xy-xy : %d %d %d %d (last : %d)", originX, originY, originX2, originY2, (isLastPage ? 1 : 0));

    FIBITMAP *rescaled = FreeImage_RescaleRect(dib, resizeWidth, resizeHeight, originX, originY, originX2, originY2, (FREE_IMAGE_FILTER)resizeMethod);
    FreeImage_Unload(dib);

    FIBITMAP *dib565 = FreeImage_ConvertTo16Bits565(rescaled);
    FreeImage_Unload(rescaled);

    if(filterOption)
        image_filter(dib565, filterOption);

    fill_pixels(&info, pixels, make565(255,255,255));
    copy_pixels_flip_vertical(pixels, &info, FreeImage_GetBits(dib565), FreeImage_GetPitch(dib565), displayX, displayY, resizeWidth, resizeHeight);

    FreeImage_Unload(dib565);
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("image_out ms : %g",now_ms()-starttime);

    return viewCount+RETURN_PAGE_UNIT*(isDoublePage ? 1 : 0);
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

JNIEXPORT jint JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromPath(JNIEnv *env, jobject obj,jobject bitmap,jstring path, jboolean isLastPage, jint viewIndex,
                                                                                  int optionViewMode, int optionResizeMode, int optionResizeMethod,
                                                                                  jstring filterStr)
{
    jint status = RETURN_CODE_FAIL_UNKOWN;
    jboolean isCopy;
    const char * imgPath = env->GetStringUTFChars(path, &isCopy);
    if(imgPath==NULL)
        return status;
    LOGI("Load : %s",imgPath);
    const char * filterOption = env->GetStringUTFChars(filterStr, &isCopy);

    FREE_IMAGE_FORMAT fif = FreeImage_GetFIFFromFilename(imgPath);
    if(fif == FIF_UNKNOWN)
        status = RETURN_CODE_UNKOWN_TYPE;
    else if(FreeImage_FIFSupportsReading(fif)==false)
        status = RETURN_CODE_UNSUPPORT_TYPE;
    else
    {
        FIBITMAP *dib = FreeImage_Load(fif, imgPath, 0);
        if(dib)
        {
            LOGI("Load OK");
            status = image_out2(env,bitmap,dib,optionViewMode,optionResizeMode,optionResizeMethod,isLastPage==JNI_TRUE,viewIndex,0.1,filterOption);
        }
        else
            status = RETURN_CODE_FAIL_TO_LOADIMAGE;
    }

    env->ReleaseStringUTFChars(path, imgPath);
    env->ReleaseStringUTFChars(filterStr, filterOption);
    return status;
}

JNIEXPORT jint JNICALL Java_net_deadwi_library_FreeImageWrapper_loadImageFromZip(JNIEnv *env, jobject obj,jobject bitmap, jstring zipfileStr, jstring innerFileStr, jboolean isLastPage, jint viewIndex,
                                                                                 int optionViewMode, int optionResizeMode, int optionResizeMethod,
                                                                                 jstring filterStr)
{
    jint status = RETURN_CODE_FAIL_UNKOWN;
    jboolean isCopy;
    const char * zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    const char * filterOption = env->GetStringUTFChars(filterStr, &isCopy);
    char * innerFilename = cstrFromJavaStringEucKR(env,innerFileStr);
    LOGI("Load : %s",innerFilename);

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
                    status = image_out2(env,bitmap,dib,optionViewMode,optionResizeMode,optionResizeMethod,isLastPage==JNI_TRUE,viewIndex,0.1,filterOption);
                }
            }
        }
        else
        {
            LOGI("Get data from zip fail : code=%d",ret);
        }
    }
    else
    {
        LOGI("Get data size from zip fail : code=%d",dataSize);
    }

    // Release memory
    if(byteData)
        free(byteData);
    if(innerFilename)
        free(innerFilename);
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
    env->ReleaseStringUTFChars(filterStr, filterOption);
    return status;
}

#ifdef __cplusplus
}
#endif