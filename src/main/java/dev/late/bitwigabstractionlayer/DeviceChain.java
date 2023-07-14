package dev.late.bitwigabstractionlayer;

import java.util.ArrayList;

import dev.late.main.Util;

public class DeviceChain {


  public static boolean isDeviceChain(Object adQObject) {
    return adQObject.getClass().getName().equals("adQ");
  }

  public static ArrayList<Device> getDevices(Object chain) {
    ArrayList<Object> children = (ArrayList<Object>) Util.getField(chain, "mvS");
    if(children == null) {
      return null;
    }

    ArrayList<Device> devices = new ArrayList<>();
    for (Object child : children) {
      if (Device.isDevice(child)) {
        devices.add(new Device(child));
      }
    }
    return devices;
  }
}

