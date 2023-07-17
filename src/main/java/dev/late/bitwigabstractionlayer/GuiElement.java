package dev.late.bitwigabstractionlayer;

import dev.late.main.Util;

public class GuiElement {

  public static long[] calculateAbsolutePosition(Object instance) {
    // The GUI layout is a tree of GUI elements, with each element
    // placed according to its width and height, and an x/y offset
    // from its parent.
    //
    // The root of the tree has its x/y offset relative to the X11 display,
    // so by accumulating the x/y offsets of an elem and all its parents,
    // we can calculate the absolute position of the element on the display.

    long[] deviceGeom = getDimensions(instance);

    // x/y are measured from the top left of the display.
    long xOffset = 0;
    long yOffset = 0;
    long width = deviceGeom[2];
    long height = deviceGeom[3];

    Object currentObject = instance;
    while (currentObject != null) {

      long[] geom = getDimensions(currentObject);
      xOffset += geom[0];
      yOffset += geom[1];

      // "recurse" into the parent object and accumulate x/y offset values
      currentObject = GuiElement.getParentGUIObject(currentObject);
    }
    return new long[]{xOffset, yOffset, width, height};
  }

  public static long[] getDimensions(Object instance) {

    // For performance reasons, we are not using Util.callNoArgumentMethod() here.
    long xOffsetRelativeToParent = (long) Util.getFieldFromNamedSuperclass(instance, "Mzk", "cHE");
    long yOffsetRelativeToParent = (long) Util.getFieldFromNamedSuperclass(instance, "uEr", "cHE");
    long width = (long) Util.getFieldFromNamedSuperclass(instance, "Sp3", "cHE");
    long height = (long) Util.getFieldFromNamedSuperclass(instance, "Y2L", "cHE");

    return new long[]{xOffsetRelativeToParent, yOffsetRelativeToParent, width, height};
  }

  public static Object getParentGUIObject(Object obj) {
    // For performance reasons, we are not using Util.getField(obj, "hWS") here.
    return Util.getFieldFromNamedSuperclass(obj, "hWS", "cIz");
  }

  public static Object getRootGUINode(Object someNode) {

    // Sanity check
    if (someNode == null || !someNode.toString().startsWith("Pn")) {
      return null;
    }

    Object currentObject = someNode;
    while (true) {
      Object parent = getParentGUIObject(currentObject);
      if (parent == null) {
        return currentObject;
      }
      currentObject = parent;
    }
  }

  public static void invalidateGUIElement(Object element) {
    Util.callNoArgumentMethod(element, "cLG");
  }
}
