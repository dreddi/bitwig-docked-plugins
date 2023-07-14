package dev.late.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DisplayUtil {

  static long displayWidth = 3840;
  // TODO: make this dynamic, for now we assume a 4k display

  static {
    try {
      loadLibraryFromJar("/res/libdisplayutil.so");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private static void loadLibraryFromJar(String path) throws IOException {

    // Prepare temporary file
    File temp = File.createTempFile("libdisplayutil_", ".so");
    temp.deleteOnExit();
    if (!temp.exists()) {
      throw new FileNotFoundException("[bitwig-docked-plugins] Temp file " + temp.getAbsolutePath() + " could not be created.");
    }

    // Prepare buffer for data copying
    byte[] buffer = new byte[1024];
    int readBytes;

    // Open and check input stream
    try (InputStream is = DisplayUtil.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new FileNotFoundException("[bitwig-docked-plugins] File " + path + " was not found inside the JAR.");
      }

      try (OutputStream os = new FileOutputStream(temp)) {
        // Copy data to temporary file
        while ((readBytes = is.read(buffer)) != -1) {
          os.write(buffer, 0, readBytes);
        }
      }
    }

    // Finally, load the library
    System.load(temp.getAbsolutePath());
    System.err.println("[bitwig-docked-plugins] Loaded library " + path + " from classpath as " + temp.getAbsolutePath());
  }






  private static native int moveAndResizeWindowNative(long windowId, int x, int y, int width,
      int height, boolean allowXMapWindow);

  private static native int hideWindowNative(long windowId);

  private static native long findWindowIdNative(String windowName);

  public static native int[] getWindowDimensionsNative(String windowName);

  // Window ID cache
  private static final Map<String, Long> windowIdCache = new HashMap<>();

  public static void cacheWindowId(String windowName, long windowId) {
    windowIdCache.put(windowName, windowId);
  }

  public static Long getWindowIdFromCache(String windowName) {
    return windowIdCache.get(windowName);
  }


  public static void moveAndResizeWindow(String windowName, int x, int y, int width, int height,
      boolean allowXMapWindow) {
    // First check the cache
    // If the cache is empty, search the X11 tree for the window
    // If that's empty, then give up.
    // Else, cache the found window ID, and continue to move and resize the window

    // on cache hit, or cache update
    // do the move
    // sometimes the cached window ID is invalid, so the operation will fail
    // in that case, we retry by calling the method recursively.

    Long windowId = getWindowIdFromCache(windowName);
    if (windowId == null) {
      windowId = findWindowIdNative(windowName);
      if (windowId == 0) {
        return;
      }
      cacheWindowId(windowName, windowId);
    }

    int status = moveAndResizeWindowNative(windowId, x, y, width, height, allowXMapWindow);

    // if status == JNI_ERR
    if (status == -1) {
      // Remove the invalid window ID from the cache
      windowIdCache.remove(windowName);
    }
  }

  public static void hideWindow(String windowName) {
    Long windowId = getWindowIdFromCache(windowName);
    if (windowId == null) {
      windowId = findWindowIdNative(windowName);
      if (windowId == 0) {
        return;
      }
      cacheWindowId(windowName, windowId);
    }

    int status = hideWindowNative(windowId);

    // -1: JNI_ERR, 0: false, 1: true
    if (status == -1 || status == 0) {
      // Remove the invalid window ID from the cache
      windowIdCache.remove(windowName);
    }
  }

  public static void drawWindow(String windowName, long[] geom, boolean allowXMapWindow) {
    if (geom[0] >= displayWidth - 2) {
      hideWindow(windowName);
    } else if (geom[2] < 2) { // X will error with BadValue if we try to resize to 0 width

      hideWindow(windowName);
    } else {
      moveAndResizeWindow(windowName, (int) geom[0], (int) geom[1], (int) geom[2], (int) geom[3],
          allowXMapWindow);
    }

  }

  static RateLimiter rateLimiter = new RateLimiter(
      1000); // Guards against spamming the X server with resize requests


}
