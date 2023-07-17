package dev.late.main;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.io.*;

import dev.late.bitwigabstractionlayer.*;


public class Agent {
  private static HashSet<String> agentConfig = new HashSet<>();
  public static BrowserState browserState = null;

  public static void premain(String arguments, Instrumentation instrumentation) {
    // Read a config file from the JAR and use that list to filter out devices that we don't want to dock
    // by default.
    String filename = "/res/config.txt";
    try {
      InputStream inputStream = Agent.class.getResourceAsStream(filename);
      if (inputStream != null) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
          String data = line.trim();
          if (data.length() > 0) {
            agentConfig.add(data);
          }
        }

        bufferedReader.close();
      } else {
        System.out.println("[bitwig-docked-plugins] config.txt file not found: " + filename);
      }
    } catch (IOException e) {
      System.out.println("[bitwig-docked-plugins] config.txt exists but could not be read.");
      e.printStackTrace();
      return;
    }

    Hooks.PaintEvent.init(instrumentation);
    Hooks.LongRectangleConstructor.init(instrumentation);
    Hooks.FileDialogConstructor.init(instrumentation);
    Hooks.BrowserWidgetConstructor.init(instrumentation);
    Hooks.ModalWidgetConstructor.init(instrumentation);
    Hooks.MouseButtonPressedEvent.init(instrumentation);

    System.out.println("[bitwig-docked-plugins] Successfully initialised.");
  }



  public static void handleMouseButtonPressedEvent(Object instance, Object[] allArgs) {
    // Currently all we do here is check for clicks on the 'parameter list' button in the
    // device wrapper. If we detect a click on that button, we toggle the docked state of the
    // device.

    Object event = allArgs[0];

    Object iW = MouseButtonPressedEvent.getGUIElementUnderMouse(instance, event);
    if (iW == null) {
      return;
    }

    if (DeviceButton.isParameterListButton(iW)) {
      // We want to ignore native devices, so we check if the device is a third party device.
      Device device = Device.getDeviceFromChildOfDevice(iW);
      if (!device.isThirdPartyDevice()) {
        return;
      }

      // Also, if the user was on another tab, like the sidechain-in, or multi-out tab, then we
      // don't want to interfere with them returning to the parameter list view. So do nothing
      // in that case. This should only run when clicking on the parameter list button while
      // it's already selected.
      if(!DeviceButton.isToggledOn(iW)) {
        return;
      }

      // if not, then do nothing. if so, then toggle that device's 'docked' state.
      toggleDeviceWhitelist(device);

    } else if (DeviceButton.isShowPluginWindowButton(iW)) {
      // To force a recalculation of the device geometry after the plugin appears
      // or disappears, we invalidate the drawing area.
      Device device = Device.getDeviceFromChildOfDevice(iW);
      Object drawingArea = device.parameterArea();
      GuiElement.invalidateGUIElement(drawingArea);
    }

  }

  public static void handleFileDialogConstructor(Object instance) {
    // out.println("Caught com.bitwig.x11_windowing_system.X11FileDialog");

    // Save a reference to the file dialog in a variable
    // so that we can access it from the handlePaintEventAtDeviceChain routine
    if (browserState == null) {
      return;
    }
    browserState.lastFileDialog = instance;
  }

  public static void handleParameterAreaRectConstructor(Object instance, Object returnValue) {
    // Here we are catching when the geometry for the parameter area is recalculated.
    // This happens every time the text in the parameters area changes, the whole area being
    // invalidated.
    // We hook the construction of the LongRectangle object to expand the parameter area
    // to fit the plugin window.

    // Get a handle to our device and quit if it doesn't have a currently active plugin window.
    Device device = Device.getDeviceFromChildOfDevice(instance);
    if (!device.isThirdPartyDevice() || !Agent.isDeviceWhitelisted(device)
        || !device.isShowPluginWindowEnabled()) {
      return;
    }
    // out.println("Caught djf param area");

    // Find the dimensions of the plugin window and set the rect to that.
    String longName = device.windowTitle();
    if (longName == null) {
      return;
    }
    int[] dimensions = DisplayUtil.getWindowDimensionsNative(longName);
    if (dimensions == null) {
      return;
    }
    LongRectangle.setWidth(returnValue, dimensions[0]);
  }

  public static void handlePaintEventAtDeviceChain(Object chain) {
    // We want to run our 'business logic' on a regular basis, and also have a reference to
    // the currently visible device chain while doing so.
    // Until we start using our own threads, hooking the regularly scheduled PaintEvent
    // is the simplest way to do it.

    if (browserState == null) {
      browserState = new BrowserState(GuiElement.getRootGUINode(chain));
    }

    // for performance, these are not done inside the for loop below.
    if (browserState.isBrowserOrMenuOrFileDialogOpen()) {
      return;
    }
    if (!DisplayUtil.rateLimiter.shouldAllow()) {
      return;
    }

    ArrayList<Device> chainDevices = DeviceChain.getDevices(chain);
    if (chainDevices == null) {
      return;
    }

    for (Device device : chainDevices) {

      // Check if it is a third party device
      if (!device.isThirdPartyDevice()) {
        continue;
      }
      // Check if it is in our whitelist/blacklist
      if (!Agent.isDeviceWhitelisted(device)) {
        continue;
      }
      // Check if it has a parameter window that can be repurposed and expanded
      Object drawingArea = device.parameterArea();
      if (drawingArea == null) {
        continue;
      }

      // Check if it has a currently open plugin window
      if (!device.isShowPluginWindowEnabled()) {
        // If not, then go no further.
        //
        // But, if the plugin window was just closed while docked, then the device will be
        // left expanded.  We need to collapse it.
        // We do this in the MouseButtonPressedEvent hook...
        continue;
      }

      String longName = device.windowTitle();
      if (longName == null) {
        continue;
      }

      //
      // Modify the device geometry
      //

      long[] geom = GuiElement.getDimensions(drawingArea);

      boolean isDeviceInOriginalState = false;

      if (geom[2] == 340) {
        // TODO: fix this magic number, it probably doesn't work on other screen sizes and
        // scaling levels
        isDeviceInOriginalState = true;
      }
      if (isDeviceInOriginalState) {
        // Force the recalculation of the device geometry, which will be intercepted by
        // handleParameterAreaRectConstructor
        // This will invalidate on every frame until the device has been expanded.
        // TODO: this only needs to happen once, unsure if performance critical though.
        GuiElement.invalidateGUIElement(drawingArea);

        // Until the param area is expanded, we'll not attempt to place the window
        // anywhere. just hide it until the wrapper is expanded and ready.
        DisplayUtil.hideWindow(longName);
        continue;
      }

      //
      // Draw the plugin window on top of the modified device.
      //

      // Draw the window if not fully occluded.
      if (device.isParamAreaOccluded(drawingArea)) {
        DisplayUtil.hideWindow(longName);
      } else {
        // Get the absolute position of the parameter area in terms of display coordinates.
        long[] parameterAreaGeom = GuiElement.calculateAbsolutePosition(
            drawingArea);

        // Sometimes the docked area is partially occluded on the left or right as you scroll
        // through the devices view. Here we determine the true drawable area of our
        // parameterAreaGeom
        long[] result = device.getVisibleAreaRelativeToAbsolutePosition(drawingArea);

        if (result != null) {
          parameterAreaGeom[0] += result[0];
          parameterAreaGeom[2] += result[1];

          // Bitwig, by default, hides plugin windows when browsers, menus, certain
          // modals, and dialogs are open. We want to refrain from trying to
          // map windows during this time, to avoid the plugin crashes that happen when we
          // do.
          //
          // So, we should identify the moments where we are coming back from a browser,
          // and wait a little bit before re-enabling mapping behaviour. To catch the
          // exact moment we are back would require identifying exactly when the BitwigAudioEngine
          // remaps the windows, which we may not have access to in the main process.
          // TODO: hook audio engine or research X11 event listeners
          if (browserState.isAllowedToMapX11Windows()) {
            DisplayUtil.drawWindow(longName, parameterAreaGeom, true);
          } else {
            DisplayUtil.drawWindow(longName, parameterAreaGeom, false);
          }

        }

      }

    }

  }

  public static boolean isDeviceWhitelisted(Device device) {
    return agentConfig.contains(device.getPluginName());
  }

  private static void toggleDeviceWhitelist(Device device) {

    String name = device.getPluginName();

    if (agentConfig.contains(name)) {
      agentConfig.remove(name);

      // Mark the param area for recalculation of its geometry
      GuiElement.invalidateGUIElement(device.parameterArea());

    } else {
      agentConfig.add(name);
    }
  }

}
