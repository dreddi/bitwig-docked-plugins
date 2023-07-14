package dev.late.bitwigabstractionlayer;

import dev.late.main.Util;

public class DeviceButton {

  public static boolean isParameterListButton(Object iW) {
    if (iW == null || !iW.getClass().getName().equals("cMF")) {
      return false;
    }

    String svg = (String) Util.getField(iW, "ZtL");
    return svg.equals("plugin_parameters.svg");
  }

  public static boolean isToggledOn(Object iW) {
    Boolean isButtonActive = (Boolean) Util.getField(iW, "Eq");
    return Boolean.TRUE.equals(isButtonActive);
  }
}