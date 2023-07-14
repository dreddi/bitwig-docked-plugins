package dev.late.bitwigabstractionlayer;

import java.util.Map;

import dev.late.main.Util;

public class MouseButtonPressedEvent {


  public static Object getGUIElementUnderMouse(Object instance, Object event) {
    // If we have a reference to a hbd object, and the event, we can inflate the event using the
    // secret incantation `cAe var2 = (cAe)instance.rOT.get(argument0.hWS());`

    Object rOT = Util.getFieldFromNamedSuperclass(instance, "rOT", "cCw");
    if (rOT == null) {
      return null;
    }
    Object var2 = ((Map<?, ?>) rOT).get(Util.callNoArgumentMethod(event, "hWS"));

    // Returns the element under the cursor.
    Object iW = Util.getField(var2, "KzU.hWS.iW");
    return iW;
  }
}