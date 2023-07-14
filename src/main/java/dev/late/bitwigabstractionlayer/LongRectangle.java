package dev.late.bitwigabstractionlayer;

import java.lang.reflect.Field;

import dev.late.main.Util;

public class LongRectangle {


  public static void setWidth(Object rect, int width) {
    Field rectWidth = Util.getJavaField(rect, "iW");
    assert rectWidth != null;
    try {
      rectWidth.setLong(rect, width);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}