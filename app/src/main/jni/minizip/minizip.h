#include <jni.h>

#ifndef _Included_MinizipWrapper_
#define _Included_MinizipWrapper_
#ifdef __cplusplus
extern "C" {
#endif

int getFileData(const char* zipFilename, const char* innerFilename, jbyte* byteData, jsize byteDataSize);

JNIEXPORT jobject JNICALL Java_net_deadwi_library_MinizipWrapper_getFilenamesInZip(JNIEnv *, jobject, jstring);

JNIEXPORT jint JNICALL Java_net_deadwi_library_MinizipWrapper_getFileData(JNIEnv * env, jobject, jstring zipfileStr, jstring innerFileStr, jbyteArray jout);

#ifdef __cplusplus
}
#endif
#endif
