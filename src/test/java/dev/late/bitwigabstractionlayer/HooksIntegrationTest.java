package dev.late.bitwigabstractionlayer;

import static org.junit.jupiter.api.Assertions.fail;

import net.bytebuddy.jar.asm.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

class HooksIntegrationTest {

  static URLClassLoader bitwig_jar;

  @BeforeAll
  static void init() throws MalformedURLException {
    String pathToJar = "src/test/resources/reference_jars/4.3.10/bitwig.jar";
    File file = new File(pathToJar);
    if (!file.exists()) {
      fail(pathToJar + " not found");
    }
    // turn file into a URLClassLoader
    URL[] urls = new URL[]{file.toURI().toURL()};
    bitwig_jar = new URLClassLoader(urls);
  }

  @Test
  void testInit() throws Throwable {
    // We are interested in testing that bitwig-internal methods we hook into actually exist.
    // To do this, we mock the Hooks.instantiateByteBuddyAgent call and validate its arguments
    // against the target bitwig.jar file.

    try (MockedStatic<Hooks> mocked = Mockito.mockStatic(Hooks.class)) {
      Instrumentation instrumentation = Mockito.mock(Instrumentation.class);

      mocked.when(() -> Hooks.instantiateByteBuddyAgent(
          Mockito.any(Instrumentation.class),
          Mockito.any(String.class),
          Mockito.any(String.class),
          Mockito.any(String.class),
          Mockito.any(Class.class)
      )).thenAnswer(invocation -> {
        // This is the code that will be executed when the method is called
//        System.out.println("Mocked method called with" + Arrays.toString(invocation.getArguments()));
        String className = (String) invocation.getArguments()[1];
        String methodName = (String) invocation.getArguments()[2];
        String methodDescriptor = (String) invocation.getArguments()[3];

        // Search the given class for a method with the given descriptor
        Class<?> currentClass = bitwig_jar.loadClass(className);
        for (Method method : currentClass.getDeclaredMethods()) {
          if (method.getName().equals(methodName)) {
            // check if the method has the correct signature
            String desc = Type.getMethodDescriptor(method);
            if (desc.equals(methodDescriptor)) {
              return null;
            }
          }
        }
        fail("Method " + methodName + methodDescriptor + " not found in " + className);
        return null;
      });

      // Call every init() method we can find at the path Hooks.*.init() and test it
      Class<?>[] classes = Hooks.class.getDeclaredClasses();
      for (Class<?> cls : classes) {
        Method method = cls.getDeclaredMethod("init", Instrumentation.class);
//        System.out.println("Testing " + cls + "." + method.getName());
        method.invoke(null, instrumentation);
      }

    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      throw e.getCause();
    }
  }

}