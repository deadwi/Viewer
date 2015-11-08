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

/*
void getFileTime(const char *filename, tm_zip *tmzip, uLong *dostime)
{
    struct stat s = { 0 };
    time_t tm_t = 0;

    if (strcmp(filename, "-") != 0) {
        char name[MAX_FILENAME_LEN + 1];

        int len = strlen(filename);
        if (len > MAX_FILENAME_LEN) {
            len = MAX_FILENAME_LEN;
        }

        strncpy(name, filename, MAX_FILENAME_LEN - 1);
        name[MAX_FILENAME_LEN] = 0;

        if (name[len - 1] == '/') {
            name[len - 1] = 0;
        }

        if (stat(name, &s) == 0) {
            tm_t = s.st_mtime;
        }
    }

    struct tm* filedate = localtime(&tm_t);
    tmzip->tm_sec  = filedate->tm_sec;
    tmzip->tm_min  = filedate->tm_min;
    tmzip->tm_hour = filedate->tm_hour;
    tmzip->tm_mday = filedate->tm_mday;
    tmzip->tm_mon  = filedate->tm_mon;
    tmzip->tm_year = filedate->tm_year;
}
*/

void setFileTime(const char *filename, uLong dosdate, tm_unz tmu_date)
{
    struct tm newdate;
    newdate.tm_sec  = tmu_date.tm_sec;
    newdate.tm_min  = tmu_date.tm_min;
    newdate.tm_hour = tmu_date.tm_hour;
    newdate.tm_mday = tmu_date.tm_mday;
    newdate.tm_mon  = tmu_date.tm_mon;

    if (tmu_date.tm_year > 1900) {
        newdate.tm_year = tmu_date.tm_year - 1900;
    } else {
        newdate.tm_year = tmu_date.tm_year;
    }
    newdate.tm_isdst = -1;

    struct utimbuf ut;
    ut.actime = ut.modtime = mktime(&newdate);
    utime(filename, &ut);
}

/*
int isLargeFile(const char* filename)
{
    FILE* pFile = fopen64(filename, "rb");
    if (pFile == NULL) return 0;

    fseeko64(pFile, 0, SEEK_END);
    ZPOS64_T pos = ftello64(pFile);
    fclose(pFile);

    return (pos >= 0xffffffff);
}
*/

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

int extractCurrentFile(unzFile uf, const char *password)
{
    unz_file_info64 file_info = { 0 };
    char filename_inzip[MAX_FILENAME_LEN] = { 0 };

    int status = unzGetCurrentFileInfo64(uf, &file_info, filename_inzip, sizeof(filename_inzip), NULL, 0, NULL, 0);
    if (status != UNZ_OK) {
    	return status;
    }

    uInt size_buf = WRITE_BUFFER_SIZE;
    void* buf = (void*) malloc(size_buf);
    if (buf == NULL) return UNZ_INTERNALERROR;

    status = unzOpenCurrentFilePassword(uf, password);
    const char* write_filename = filename_inzip;

    // Create the file on disk so we can unzip to it
    FILE* fout = NULL;
    if (status == UNZ_OK) {
        fout = fopen64(write_filename, "wb");
    }

    // Read from the zip, unzip to buffer, and write to disk
    if (fout != NULL) {
        do {
            status = unzReadCurrentFile(uf, buf, size_buf);
            if (status <= 0) break;
            if (fwrite(buf, status, 1, fout) != 1) {
                status = UNZ_ERRNO;
                break;
            }
        } while (status > 0);

        if (fout) fclose(fout);

        // Set the time of the file that has been unzipped
        if (status == 0) {
        	setFileTime(write_filename, file_info.dosDate, file_info.tmu_date);
        }
    }

    unzCloseCurrentFile(uf);

    free(buf);
    return status;
}

JNIEXPORT jint JNICALL Java_net_deadwi_library_MinizipWrapper_extractZip(JNIEnv * env, jobject, jstring zipfileStr, jstring dirnameStr, jstring passwordStr)
{
    jboolean isCopy;
    const char * zipfilename = env->GetStringUTFChars(zipfileStr, &isCopy);
    const char * dirname = env->GetStringUTFChars(dirnameStr, &isCopy);
    const char * password = env->GetStringUTFChars(passwordStr, &isCopy);

    int status = 0;

    unzFile uf = NULL;

    // Open zip file
    if (zipfilename != NULL) {
        uf = unzOpen64(zipfilename);
    }
    if (uf == NULL) {
    	return ERROR_ZIP_FILE_NOT_FOUND;
    }

    // Extract all
    status = unzGoToFirstFile(uf);
    if (status != UNZ_OK) {
    	return ERROR_ZIP_FILE;
    }

    chdir(dirname);
    status = extractCurrentFile(uf, password);

    // Release memory
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
    env->ReleaseStringUTFChars(dirnameStr, dirname);
    env->ReleaseStringUTFChars(passwordStr, password);

    return status;
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

        jstring jfilename = env->NewStringUTF((const char*) &filename_in_zip);
        jobject jitem = env->NewObject(classFileItem, env->GetMethodID(classFileItem, "<init>", "(Ljava/lang/String;Ljava/lang/String;J)V"),
                                       zipfileStr,jfilename,file_info.uncompressed_size);
        env->CallBooleanMethod(listObj, env->GetMethodID(classArrayList, "add", "(Ljava/lang/Object;)Z"), jitem);
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
    const char * innerFilename = env->GetStringUTFChars(innerFileStr, &isCopy);

    LOGI("Open zip : %s target : %s",zipfilename,innerFilename);

    unzFile uf = unzOpen64(zipfilename);
    if (uf == NULL)
        return ERROR_FILE_NOT_FOUND;

    int ret;
    ret = unzGoToFirstFile(uf);
    if(ret != UNZ_OK)
        return ERROR_ZIP_FILE_NOT_FOUND;

    jbyte* byteData = NULL;
    jsize outSize = 0;
    if(jout)
    {
        byteData = env->GetByteArrayElements(jout, &isCopy);
        outSize = env->GetArrayLength(jout);
    }

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
            if(file_info.uncompressed_size>outSize)
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
                readSize = unzReadCurrentFile(uf, (void*) byteData, outSize);
                LOGI("Read %d byte",readSize);
                if(readSize<=0)
                    break;
            }
        }
        else
            status = ERROR_ZIP_FILE_SIZE_0;
        break;
    }

    // Release memory
    env->ReleaseStringUTFChars(zipfileStr, zipfilename);
    env->ReleaseStringUTFChars(innerFileStr, innerFilename);
    // update
    if(byteData)
        env->ReleaseByteArrayElements(jout, byteData, 0);

    return status;
}
