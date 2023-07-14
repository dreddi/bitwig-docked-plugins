package dev.late.bitwigabstractionlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import dev.late.main.Util;

public class BrowserState {
  
  public Set<Object> browserModalObjectCache = new HashSet<>();
  boolean lastKnownOpenState = false;
  private Object guiRoot;
  public Object lastFileDialog = null;

  private Instant blockWindowMappingGuard = null;

  public BrowserState(Object root) {
    if (!root.getClass().getName().equals("Pn")) {
      throw new IllegalArgumentException("Invalid class type, expected Pn, got " + root.getClass().getName());
    }

    guiRoot = root;
  }



  private boolean checkHasGuardExpired() {
    if (blockWindowMappingGuard == null) {
      return true;
    }
    Instant currentTimestamp = Instant.now();
    Duration elapsedTime = Duration.between(blockWindowMappingGuard, currentTimestamp);
    Duration halfSecond = Duration.ofMillis(500); // 0.5 seconds

    boolean isGuardExpired = elapsedTime.compareTo(halfSecond) >= 0;

    if (isGuardExpired) {
      blockWindowMappingGuard = null;
    }
    return isGuardExpired;
  }

  public boolean isAllowedToMapX11Windows() {

    // first, is the browser or menu open?
    boolean isOpen = isBrowserOrMenuOrFileDialogOpen();

    // if so, return false
    if (isOpen) {
      lastKnownOpenState = true;

      return false;
    }

    // if not, check if it has closed since we last were asked.
    if (lastKnownOpenState == true && isOpen == false) {
      blockWindowMappingGuard = Instant.now();
      lastKnownOpenState = false;
      return checkHasGuardExpired(); // wait the requisite amount of time
    }

    // if not, then it may have closed very recently, so check the timer
    return checkHasGuardExpired();
  }

  public boolean isBrowserOrMenuOrFileDialogOpen() {

    Object overlaySubTreeRoot = Util.getField(guiRoot, "mZc"); // Pn/cBk/dkq/NV

    boolean isBrowserOrModalVisible = true;
    boolean isHomeMenuVisible = true;
    boolean isFileDialogOpen = true;

    // Case: If the home menu is not visible
    isHomeMenuVisible = (boolean) Util.getField(overlaySubTreeRoot, "DSh");
    // // Pn/cBk/dkq/NV
    // // DSh = false
    // // oV.toString() == NOT_FOCUSED
    // // both correspond to popup off

    // Case: If a browser or modal object is constructed and linked to the GUI tree
    isBrowserOrModalVisible = doesBrowserOrModalExist();

    // Case: If a native file dialog is open
    isFileDialogOpen = isX11FileDialogCurrentlyInProgress();

    return isBrowserOrModalVisible || isHomeMenuVisible || isFileDialogOpen;
  }

  private boolean doesBrowserOrModalExist() {
    // We maintain a cache of all cFf and cFo objects that are created (by hooking
    // their construction), so that we can check if they are still referenced by the
    // GUI tree. If they are not, then they have been removed from view.

    // for each item in the browserModalObjectCache, check if it exists in the GUI
    // tree
    // if it doesn't, then delete the cached object, it's no longer needed.
    // if it does, then it's in view

    boolean windowExists = false;
    ArrayList<Object> cacheCopy = new ArrayList<>(browserModalObjectCache);
    for (Object windowObject : cacheCopy) {

      Object hWS = GuiElement.getParentGUIObject(windowObject);
      if (hWS == null) {
        browserModalObjectCache.remove(windowObject);
      } else {
        windowExists = true;
      }
    }

    return windowExists;
  }

  private boolean isX11FileDialogCurrentlyInProgress() {
    if (lastFileDialog == null) {
      return false;
    }
    boolean isRunning = (boolean) Util.getField(lastFileDialog, "SvE");

    return isRunning;
  }

}