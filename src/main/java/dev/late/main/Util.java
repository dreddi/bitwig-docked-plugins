package dev.late.main;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Util {

  public static Object callNoArgumentMethod(Object instance, String methodName) {
    // Call a method that takes no arguments, by searching up the inheritance hierarchy
    // for the correct method to invoke, then doing so.

    try {
      Method method = null;
      Class<?> clazz = instance.getClass();
      while (method == null && clazz != null) {
        try {
          method = clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException ignored) {
        }
        clazz = clazz.getSuperclass();
      }
      if (method == null) {
        throw new NoSuchMethodException(methodName);
      }

      return method.invoke(instance); // here

    } catch (NoSuchMethodException e) {
      System.err.println("Error: Method '" + methodName + "' not found on instance " + instance);
    } catch (IllegalAccessException e) {
      System.err.println(
          "Error: Cannot access method '" + methodName + "' on instance " + instance);
    } catch (InvocationTargetException e) {
      System.err.println(
          "Error: Exception thrown while calling method '" + methodName + "' on instance "
              + instance);
      e.getTargetException().printStackTrace();
    } catch (Exception e) {
      System.err.println(
          "Error: Unknown error while calling method '" + methodName + "' on instance " + instance);
      e.printStackTrace();
    }
    return null;
  }

  public static Object getField(Object obj, String fieldName) {
    // Convenience method for getting the value at a field, where you may want to dig
    // a few references deep into an object (like obj.field1.fieldB.fieldX).

    String[] fieldNames = fieldName.split("\\.");
    int iterationDepth = fieldNames.length;

    // if we are getting a value at some nested field obj.field1.fieldB.fieldX
    // we get the value at the first field and then repeat the process on the returned object

    Object resultObj = obj;
    Class<?> currentClass = obj.getClass();

    for (int i = 0; i < iterationDepth; i++) {
      fieldName = fieldNames[i];

      getValue:
      while (currentClass != null) {
        Field[] fields = currentClass.getDeclaredFields();
        for (Field field : fields) {
          if (field.getName().equals(fieldName)) {
            try {
              field.setAccessible(true);
              resultObj = field.get(resultObj); // here
              if (resultObj == null) {
                return null;
              }
              currentClass = resultObj.getClass();
              break getValue;
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          }
        }
        currentClass = currentClass.getSuperclass();
      }
    }
    return resultObj == obj ? null : resultObj;
  }

  public static Object getFieldFromNamedSuperclass(Object obj, String fieldName,
      String superclassName) {
    // Convenience method for breaking the encapsulation that would normally prevent a
    // user of an object from calling methods on the object's superclass. 

    Class<?> superclass = obj.getClass();

    while (!superclass.getName().equals(superclassName)) {
      superclass = superclass.getSuperclass();
      if (superclass == null) {
        throw new RuntimeException(
            "Could not find superclass " + superclassName + " in object hierarchy of"
                + obj.getClass().getName());
      }
    }

    Object fieldObject = null;
    try {
      Field field = superclass.getDeclaredField(fieldName);
      field.setAccessible(true);

      fieldObject = field.get(obj);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
    return fieldObject;
  }

  public static Field getJavaField(Object instance, String fieldName) {
    // Convenience method for getting the java field for a field,
    // Does not support drilling down into nested objects.
    Class<?> clazz = instance.getClass();

    while (clazz != null) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        if (field.getName().equals(fieldName)) {
          field.setAccessible(true);
          return field;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return null;

  }
}
