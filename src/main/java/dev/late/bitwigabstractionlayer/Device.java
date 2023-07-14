package dev.late.bitwigabstractionlayer;

import java.util.ArrayList;
import java.util.UUID;

import dev.late.main.Util;

public class Device {

  Object device;

  public static boolean isDevice(Object obj) {
    return obj.getClass().getName().equals("adX");
  }

  public static Device getDeviceFromChildOfDevice(Object obj) {
    if(!obj.toString().contains("adX")){
      throw new IllegalArgumentException(
          "Expected child GUI element of device, got" + obj);
    }
    Object instance = obj;
    while (!isDevice(instance)) {
      instance = GuiElement.getParentGUIObject(instance);
    }
    return new Device(instance);
  }

  public Device(Object device) {
    if (!isDevice(device)) {
      throw new IllegalArgumentException(
          "Invalid class type, expected adX, got " + device.getClass().getName());
    }
    this.device = device;
  }

  public boolean isParamAreaOccluded(Object paramArea) {

    if (!paramArea.getClass().getName().equals("djf")) {
      throw new IllegalArgumentException(
          "Invalid class type, expected djf, got " + paramArea.getClass().getName());
    }

    // if its minimised, its occluded
    if (isDeviceMinimised()) {
      return true;
    }

    // get the device's parent GUI object, the adQ device chain
    Object deviceChain = GuiElement.getParentGUIObject(device);
    if (deviceChain == null) {
      return false; // We don't know if its occluded or not, just default to drawing the plugin.
    }

    // get the adQs parent object, the CAS device chain scroll container
    Object scrollContainer = GuiElement.getParentGUIObject(deviceChain);
    if (scrollContainer == null) {
      return false;
    }
    // get the geometry of the scroll container.
    // The scroll container x offset tells us the scroll position of the devices
    // view. Negative containerGeom[0] mean we are scrolled 'x' distance into the
    // devices view. It will also never be positive, so we need to negate it to get
    // the actual scroll position.
    long[] containerGeom = GuiElement.getDimensions(scrollContainer);
    long scrollContainerLeftEdge = (-1 * containerGeom[0]);

    // get the CASs parent object, the device view viewport
    // The dimensions of this geom map to the allowable drawable region for our
    // plugin window.
    Object deviceViewViewport = GuiElement.getParentGUIObject(scrollContainer);
    if (deviceViewViewport == null) {
      return false;
    }
    long[] viewportGeom = GuiElement.getDimensions(deviceViewViewport);
    long viewportWidth = viewportGeom[2];
    long viewportRightEdge = scrollContainerLeftEdge + viewportWidth;

    // calculate the offset of the param window to the position of the device in the
    // device chain
    long[] result = getOffsetOfParamAreaRelativeToDevice(paramArea);

    long pluginLeftEdge = result[0];
    long pluginRightEdge = result[0] + result[2];

    if (scrollContainerLeftEdge > pluginRightEdge) {
      // we are hidden on the left side
      return true;
    }
    if (pluginLeftEdge > viewportRightEdge) {
      // we are hidden on the right side
      return true;
    }

    return false;
  }

  public long[] getVisibleAreaRelativeToAbsolutePosition(Object paramArea) {

    // get the device's parent GUI object, the adQ device chain
    Object deviceChain = GuiElement.getParentGUIObject(device);
    if (deviceChain == null) {
      return null;
    }

    // get the adQ's parent object, the CAS device chain scroll container
    Object scrollContainer = GuiElement.getParentGUIObject(deviceChain);
    if (scrollContainer == null) {
      return null;
    }
    long[] containerGeom = GuiElement.getDimensions(scrollContainer);
    // The width of this geom is the full expanded width of the device chain.
    // containerGeom[0] == -100 means that we are scrolled 100 pixels(?) into the device chain.
    long scrollPosition = (-1 * containerGeom[0]);

    // get the CAS's parent object, the devices view viewport
    Object deviceViewViewport = GuiElement.getParentGUIObject(scrollContainer);
    if (deviceViewViewport == null) {
      return null;
    }
    long[] viewportGeom = GuiElement.getDimensions(deviceViewViewport);
    // The height/width of this geom marks the dimensions of the device chain's visible area.
    long viewportWidth = viewportGeom[2];
    long endOfViewport = scrollPosition + viewportWidth;


    // calculate the offset of the param window to the position of the device in the
    // device chain
    long[] result = getOffsetOfParamAreaRelativeToDevice(paramArea);
    long pluginLeftEdge = result[0];
    long pluginRightEdge = result[0] + result[2];

    long leftBoundAsOffset = 0;
    long rightBoundAsOffset = 0;

    if (scrollPosition > pluginLeftEdge) {

      // we are occluded on the left side
      // the x offset needs to be shifted right
      // the width needs to be reduced by the amount of the offset
      leftBoundAsOffset = scrollPosition - pluginLeftEdge;
      rightBoundAsOffset = pluginLeftEdge - scrollPosition;
    }

    if (pluginRightEdge > endOfViewport) {
      // we are occluded on the right side
      // the width needs to be reduced by the difference between the
      // right edge of the viewport and the right edge of the plugin window
      rightBoundAsOffset += endOfViewport - pluginRightEdge;

    }

    return new long[] {leftBoundAsOffset, rightBoundAsOffset};
  }

  private boolean isDeviceMinimised() {
    // boolean isMinimised = adX.sNw.Cy2 == false;
    Object deviceBodyContainer = Util.getField(device, "sNw");
    long deviceBodyWidth = (long) Util.getField(deviceBodyContainer, "Sp3");
    boolean isMinimised = deviceBodyWidth == 0;

    return isMinimised;
  }

  public boolean isShowPluginWindowEnabled() {

    ArrayList<Object> array = (ArrayList<Object>) Util.getField(device, "FKC.Xwz.oVA.mvS");

    if (array.size() != 3) {
      // Have never seen this situation, so we should fail gracefully, and not draw
      // the plugin.
      return false;
    }

    // get cMF object in mvs.get(0) field of cIz
    Object cMFObject = array.get(0);

    // cLT.ZtL should == "plugin_window.svg"
    boolean hasPluginWindowSVG = ((String) Util.getField(cMFObject, "ZtL")).equals(
        "plugin_window.svg");
    if (!hasPluginWindowSVG) {
      return false;
    }

    return (boolean) Util.getField(cMFObject, "Eq");
  }

  public Object parameterArea() {
    ArrayList<Object> mvSObject = (ArrayList<Object>) Util.getField(device, "XsJ.Xwz.ZtL.Xwz.mvS");

    if (mvSObject == null || mvSObject.size() < 2) {
      return null;
    }
    // get the djf object at index 1 of the mvS array
    Object djfObject = mvSObject.get(1);

    Object lsSObject = Util.getField(djfObject, "lsS");

    if (lsSObject.toString().equals("PARAMETERS")) {
      return djfObject;
    }

    return null;
  }

  private long[] getOffsetOfParamAreaRelativeToDevice(Object paramArea) {

    long x_accumulator = 0;
    long y_accumulator = 0;
    long width = 0;
    long height = 0;

    long[] deviceGeom = GuiElement.getDimensions(paramArea);

    width = deviceGeom[2];
    height = deviceGeom[3];

    Object currentObject = paramArea;
    while (currentObject != null) {

      long[] geom = GuiElement.getDimensions(currentObject);
      x_accumulator += geom[0];
      y_accumulator += geom[1];

      if (Device.isDevice(currentObject)) {
        break;
      }

      // "recurse" into the parent object and repeat
      currentObject = GuiElement.getParentGUIObject(currentObject);
      if (currentObject == null) {
        return null;
      }
    }
    return new long[] { x_accumulator, y_accumulator, width, height };
  }

  public boolean isThirdPartyDevice() {

    if (Util.getField(device, "lsS.SvE").toString().equals("device contents")) {
      // This is a native device, so we can stop here,
      // as opposed to a "vst3 plugin device contents"
      // or "clap plugin device contents" or something else.
      return false;
    }

    // We have a third party device, so we can stop here
    return true;
  }

  public String windowTitle() {
    return (String) Util.getField(device, "lsS.C31");
  }

  public UUID UUID() {
    return (UUID) Util.getField(device, "lsS.lsS");
  }

  public String getPluginName() {
    // TODO: Install the default whitelist somewhere user editable, and expose these names
    // in a better way, or scrap altogether.

    // get the device.XsJ.lsS.Yqp field == The device name
    return (String) Util.getField(device, "XsJ.lsS.Yqp");
  }
}
