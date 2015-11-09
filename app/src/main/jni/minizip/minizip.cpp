#include <jni.h>
#include <android/log.h>

#include "minizip.h"
#include "unzip.h"
#include "zip.h"

#include <cstring>
#include <time.h>
#include <unistd.h>
#include <utime.h>
#include <sys/stat.h>

#include <string>

#include "../StringConverter_For_JNI.h"

#define MKDIR(d) mkdir(d, 0775)

#define  LOG_TAG    "libminizip"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

const int WRITE_BUFFER_SIZE = 16384;
const int READ_BUFFER_SIZE = 1024*4;
const int MAX_FILENAME_LEN = 1024;

// Errors id
const int ERROR_CREATE_ZIP = -100;
const int ERROR_GET_CRC32 = -101;
const int ERROR_WHILE_READ = -102;
const int ERROR_FILE_NOT_FOUND = -103;
const int ERROR_ZIP_FILE_NOT_FOUND = -104;
const int ERROR_ZIP_FILE = -105;
const int ERROR_ZIP_FILE_SIZE_0 = -106;

// Calculate the CRC32 of a file
int getCRC32(const char* filenameinzip, Bytef *buf, unsigned long size_buf, unsigned long* result_crc)
{
    unsigned long calculate_crc = 0;

    int status = ZIP_OK;

    FILE *fin = fopen64(filenameinzip, "rb");
    if (fin == NULL) status = ERROR_GET_CRC32;
    else {
        unsigned long size_read = 0;
        do {
            size_read = (int) fread(buf, 1, size_buf, fin);

            if ((size_read < size_buf) && (feof(fin) == 0)) {
                status = ERROR_WHILE_READ;
            }

            if (size_read > 0) {
                calculate_crc = crc32(calculate_crc, buf, size_read);
            }
        } while ((status == ZIP_OK) && (size_read > 0));
    }

    if (fin) {
        fclose(fin);
    }

    *result_crc = calculate_crc;
    return status;
}

int getFileData(const char* zipFilename, const char* innerFilename, jbyte* byteData, jsize byteDataSize)
{
    LOGI("Open zip : %s target : %s",zipFilename,innerFilename);

    unzFile uf = unzOpen64(zipFilename);
    if (uf == NULL)
        return ERROR_FILE_NOT_FOUND;

    int ret;
    ret = unzGoToFirstFile(uf);
    if(ret != UNZ_OK)
        return ERROR_ZIP_FILE_NOT_FOUND;

    int status=0;
    unz_file_info64 file_info = { 0 };
    char filename_in_zip[MAX_FILENAME_LEN] = { 0 };
    while(true)
    {
        ret = unzGetCurrentFileInfo64(uf, &file_info, filename_in_zip, sizeof(filename_in_zip), NULL, 0, NULL, 0);
        if (ret != UNZ_OK)
        {
            status = ERROR_ZIP_FILE;
            break;
        }
        if(strcmp(innerFilename,filename_in_zip)!=0)
        {
            ret = unzGoToNextFile(uf);
            if (ret != UNZ_OK)
                break;
            continue;
        }

        if(file_info.uncompressed_size>0)
        {
            if(file_info.uncompressed_size>byteDataSize)
            {
                status = file_info.uncompressed_size;
                break;
            }
            ret = unzOpenCurrentFile(uf);
            if (ret != UNZ_OK)
            {
                status = ERROR_ZIP_FILE;
                break;
            }

            LOGI("Get data : %s",filename_in_zip);
            int readSize = 0;
            while(true)
            {
                readSize = unzReadCurrentFile(uf, (void*) byteData, byteDataSize);
                LOGI("Read %d byte",readSize);
                if(readSize<=0)
                    break;
            }
        }
        else
            status = ERROR_ZIP_FILE_SIZE_0;
        break;
    }
    return  status;
}

JNIEXPORT jobject JNICALL Java_net_deadwi_library_MinizipWrapper_getFilenamesInZip(JNIEnv * env, jobject, jstring zipfileStr)
{
    jboolean isCopy;
    const char * _zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    if(_zipfilename==NULL)
        return NULL;
    std::string zipfilename = _zipfilename;
    env->ReleaseStringUTFChars(zipfileStr, _zipfilename);

    unzFile uf = unzOpen64(zipfilename.c_str());
    if (uf == NULL)
        return NULL;

    int ret;
    ret = unzGoToFirstFile(uf);
    if(ret != UNZ_OK)
        return NULL;

    jclass classArrayList = env->FindClass("java/util/ArrayList");
    jclass classFileItem = env->FindClass("net/deadwi/viewer/FileItem");
    jobject listObj = env->NewObject(classArrayList, env->GetMethodID(classArrayList, "<init>", "()V"));

    unz_file_info64 file_info = { 0 };
    char filename_in_zip[MAX_FILENAME_LEN] = { 0 };
    while(true)
    {
        ret = unzGetCurrentFileInfo64(uf, &file_info, filename_in_zip, sizeof(filename_in_zip), NULL, 0, NULL, 0);
        if (ret != UNZ_OK)
            return NULL;

        jstring jfilename = javaNewStringCharEucKR(env, filename_in_zip);
        jobject jitem = env->NewObject(classFileItem, env->GetMethodID(classFileItem, "<init>", "(Ljava/lang/String;Ljava/lang/String;J)V"),
                                       zipfileStr,jfilename,file_info.uncompressed_size);
        env->CallBooleanMethod(listObj, env->GetMethodID(classArrayList, "add", "(Ljava/lang/Object;)Z"), jitem);
        env->DeleteLocalRef(jfilename);
        env->DeleteLocalRef(jitem);

        LOGI("file : %s, size : %d", filename_in_zip, file_info.uncompressed_size);

        ret = unzGoToNextFile(uf);
        if (ret != UNZ_OK)
            break;
    }

    return listObj;
}

JNIEXPORT jint JNICALL Java_net_deadwi_library_MinizipWrapper_getFileData(JNIEnv * env, jobject, jstring zipfileStr, jstring innerFileStr, jbyteArray jout)
{
    jboolean isCopy;
    const char * zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    char * innerFilename = cstrFromJavaStringEucKR(env,innerFileStr);
    jbyte* byteData = NULL;
    jsize outSize = 0;
    if(jout)
    {
        byteData = env->GetByteArrayElements(jout, &isCopy);
        outSize = env->GetArrayLength(jout);
    }

    int status = getFileData(zipfilename, innerFilename, byteData, outSize);

    // Release memory
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
    if(innerFilename)
        free(innerFilename);
    // update
    if(byteData)
        env->ReleaseByteArrayElements(jout, byteData, 0);

    return status;
}
