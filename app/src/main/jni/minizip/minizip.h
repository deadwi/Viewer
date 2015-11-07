#include <jni.h>

#ifndef _Included_MinizipWrapper_
#define _Included_MinizipWrapper_
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_net_deadwi_library_MinizipWrapper_extractZip(JNIEnv *, jobject, jstring, jstring, jstring);

JNIEXPORT jstring JNICALL Java_net_deadwi_library_MinizipWrapper_getFilenameInZip(JNIEnv *, jobject, jstring);

JNIEXPORT jobject JNICALL Java_net_deadwi_library_MinizipWrapper_getFilenamesInZip(JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif
