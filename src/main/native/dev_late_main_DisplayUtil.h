/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class dev_late_main_DisplayUtil */

#ifndef _Included_dev_late_main_DisplayUtil
#define _Included_dev_late_main_DisplayUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     dev_late_main_DisplayUtil
 * Method:    moveAndResizeWindowNative
 * Signature: (JIIIIZ)I
 */
JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_moveAndResizeWindowNative
  (JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean);

/*
 * Class:     dev_late_main_DisplayUtil
 * Method:    hideWindowNative
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_dev_late_main_DisplayUtil_hideWindowNative
  (JNIEnv *, jclass, jlong);

/*
 * Class:     dev_late_main_DisplayUtil
 * Method:    findWindowIdNative
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_dev_late_main_DisplayUtil_findWindowIdNative
  (JNIEnv *, jclass, jstring);

/*
 * Class:     dev_late_main_DisplayUtil
 * Method:    getWindowDimensionsNative
 * Signature: (Ljava/lang/String;)[I
 */
JNIEXPORT jintArray JNICALL Java_dev_late_main_DisplayUtil_getWindowDimensionsNative
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
