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



  public static boolean isShowPluginWindowButton(Object button) {

    boolean isDeviceButton = button != null && button.getClass().getName().equals("cMF");
    
    if (!isDeviceButton) {
      return false;
    }

    // cLT.ZtL should == "plugin_window.svg"
    boolean hasPluginWindowSVG = ((String) Util.getField(button, "ZtL")).equals(
        "plugin_window.svg");
    if (!hasPluginWindowSVG) {
      return false;
    }

    return true;

  }

  public static boolean isToggledOn(Object iW) {
    Boolean isButtonActive = (Boolean) Util.getField(iW, "Eq");
    return Boolean.TRUE.equals(isButtonActive);
  }
}