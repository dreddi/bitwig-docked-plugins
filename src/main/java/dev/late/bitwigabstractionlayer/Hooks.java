package dev.late.bitwigabstractionlayer;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.NamedElement.WithDescriptor;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import dev.late.main.Util;
import dev.late.main.Agent;

public class Hooks {


  public static class PaintEvent {

    public static void init(Instrumentation instrumentation) {
      // hooks every PaintEvent, specifically at the point where the device chain is
      // repainted.

      // filters for method calls on the device chain container class.
      Junction<NamedElement> classFilter = ElementMatchers.named(
          "adQ");
      Junction<WithDescriptor> methodFilter = ElementMatchers.named(
              "hWS") // specifically hook only the cHE.hWS() methods with the following
          // signature
          .and(ElementMatchers.hasDescriptor(
              "(Lcom/bitwig/graphics/Amg;LcHE;Lcom/bitwig/base/geom/kml;)V"));

      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(PaintEvent.class)))
          .installOn(instrumentation);
    }

    @Advice.OnMethodExit
    private static void _exit(@Advice.This Object instance) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodExit instead to sidestep this problem.
      _onMethodExit(instance);
    }

    public static void _onMethodExit(Object instance) {
      Agent.handlePaintEventAtDeviceChain(instance);
    }

  }


  public static class FileDialogConstructor {

    public static void init(Instrumentation instrumentation) {
      // hooks every factory call to construct a file dialog widget

      // filters for method calls on com.bitwig.x11_windowing_system.X11FileDialog
      Junction<NamedElement> classFilter = ElementMatchers.named(
          "com.bitwig.x11_windowing_system.X11FileDialog");
      Junction<WithDescriptor> methodFilter =
          ElementMatchers.named("FKC").and(ElementMatchers.hasDescriptor("()V"));

      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(FileDialogConstructor.class)))
          .installOn(instrumentation);
    }

    @Advice.OnMethodExit
    private static void _exit(@Advice.This Object instance) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodExit instead to sidestep this problem.
      _onMethodExit(instance);
    }

    public static void _onMethodExit(Object instance) {
      if (instance.getClass().getName().equals("com.bitwig.x11_windowing_system.X11FileDialog")) {
        Agent.handleFileDialogConstructor(instance);
      }
    }

  }


  public static class LongRectangleConstructor {

    public static void init(Instrumentation instrumentation) {
      // hooks recalculation of GUI rectangles, to maintain our modification on
      // the device's param area rectangles, even after the GUI geometry is
      // invalidated by a change in text or other elements.

      Junction<NamedElement> classFilter = ElementMatchers.named("cHE");
      Junction<WithDescriptor> methodFilter = ElementMatchers.named("nGP");

      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(LongRectangleConstructor.class)))
          .installOn(instrumentation);
    }

    @Advice.OnMethodExit
    private static void _exit(@Advice.This Object instance,
        @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodExit instead to sidestep this problem.
      _onMethodExit(instance, returnValue);
    }

    public static void _onMethodExit(Object instance, Object returnValue) {
      if (!instance.getClass().getName().equals("djf")) {
        return;
      }
      // If we are not the 'PARAMETERS' area of a device, then do nothing.
      Object desc = Util.getField(instance, "lsS");
      boolean isParamArea = desc != null && desc.toString().equals("PARAMETERS");
      boolean isChildOfDevice = instance.toString().contains("/adX");
      if (!isParamArea || !isChildOfDevice) {
        return;
      }

      Agent.handleParameterAreaRectConstructor(instance, returnValue);

    }

  }


  public static class BrowserWidgetConstructor {

    public static void init(Instrumentation instrumentation) {
      // hooks every factory call to construct a browser widget

      Junction<NamedElement> classFilter = ElementMatchers.named(
          "cFf"); // filters for calls on the cFf factory method
      Junction<WithDescriptor> methodFilter = ElementMatchers.named("hWS")
          .and(ElementMatchers.hasDescriptor("(LcFb;LcHi;LcHE;)LcFf;"));

      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(BrowserWidgetConstructor.class)))
          .installOn(instrumentation);
    }

    @Advice.OnMethodExit
    private static void _exit(
        @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodExit instead to sidestep this problem.
      _onMethodExit(returnValue);
    }

    public static void _onMethodExit(Object returnValue) {
      if (Agent.browserState == null) {
        return;
      }
      Agent.browserState.browserModalObjectCache.add(returnValue);
    }

  }


  public static class ModalWidgetConstructor {

    public static void init(Instrumentation instrumentation) {
      // hooks every factory call to constructing a modal widget

      Junction<NamedElement> classFilter = ElementMatchers.named("cFo");
      Junction<WithDescriptor> methodFilter = ElementMatchers.named("hWS").and(
          ElementMatchers.hasDescriptor("(LcHi;LcHE;LcFn;Ljava/lang/Runnable;)LcFo;"));

      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(ModalWidgetConstructor.class)))
          .installOn(instrumentation);
    }

    @Advice.OnMethodExit
    private static void _exit(
        @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodExit instead to sidestep this problem.
      _onMethodExit(returnValue);
    }

    public static void _onMethodExit(Object returnValue) {
      if (Agent.browserState == null) {
        return;
      }
      Agent.browserState.browserModalObjectCache.add(returnValue);
    }

  }


  public static class MouseButtonPressedEvent {

    public static void init(Instrumentation instrumentation) {

      Junction<NamedElement> classFilter = ElementMatchers.named("cCw");
      Junction<WithDescriptor> methodFilter = ElementMatchers.named("hWS")
          .and(ElementMatchers.hasDescriptor("(LdBr;)V"));

      // hooks every MouseButtonPressedEvent, to catch when the param area button is
      // clicked (to toggle the device whitelist)
      new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
          .type(classFilter)
          .transform(
              (builder, typeDescription, classLoader, module, protectionDomain) -> builder.method(
                      methodFilter)
                  .intercept(Advice.to(MouseButtonPressedEvent.class)))
          .installOn(instrumentation);
    }


    @Advice.OnMethodEnter
    private static void _enter(@Advice.This(optional = true) Object instance,
        @Advice.AllArguments Object[] allArgs) {
      // Debuggers don't seem to be able to break inside this method, and it doesn't show up in
      // a stack trace, so we use _onMethodEnter instead to sidestep this problem.
      _onMethodEnter(instance, allArgs);
    }

    public static void _onMethodEnter(Object instance, Object[] allArgs) {
      // Check if argument0 is of class dBS (which is a MouseButtonPressedEvent)
      if (allArgs[0].toString().contains("MouseButtonPressedEvent")) {
        Agent.handleMouseButtonPressedEvent(instance, allArgs);
      }

    }

  }

}