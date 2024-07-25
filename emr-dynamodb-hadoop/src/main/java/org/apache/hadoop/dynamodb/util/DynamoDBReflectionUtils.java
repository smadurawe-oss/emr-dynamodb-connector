package org.apache.hadoop.dynamodb.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * Reflected-utility methods for DynamoDB connector pkg
 */
public class DynamoDBReflectionUtils {

  private static final Log log = LogFactory.getLog(DynamoDBReflectionUtils.class);

  // Default no-arg constructor reflection logic
  public static <T> T createInstanceOf(String className, Configuration conf) {
    return createInstance(className, conf, null, null);
  }

  // constructor with-args reflection logic
  public static <T> T createInstanceOfWithParams(
      String className,
      Configuration conf,
      Class<?>[] paramTypes,
      Object[] params) {
    return createInstance(className, conf, paramTypes, params);
  }

  // checks if class has method available in it
  public static boolean hasFactoryMethod(String className, String methodName) {
    Class<?> clazz = getClass(className);
    return Arrays.stream(clazz.getMethods())
        .anyMatch(method -> method.getName().equals(methodName));
  }

  // factory-based reflection logic that uses a method for object construction
  @SuppressWarnings("unchecked")
  public static <T> T createInstanceFromFactory(
      String className,
      Configuration conf,
      String methodName) {
    try {
      Class<?> clazz = getClass(className);
      Method m = clazz.getDeclaredMethod(methodName);
      m.setAccessible(true);
      T instance = (T) m.invoke(null);
      log.info("Successfully loaded class: " + className);
      ReflectionUtils.setConf(instance, conf);
      log.debug("Configured instance to use conf");
      return instance;

    } catch (NoSuchMethodException e) {
      log.error("Method not found for object construction: " + methodName);
      throw new RuntimeException("Unable to find static method to load class: " + className, e);
    } catch (InvocationTargetException e) {
      log.error("Exception found when invoking method for object construction: " + methodName);
      throw new RuntimeException("Unable to load class: " + className, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Class being loaded is not accessible: " + className, e);
    }
  }

  // checks if class can be loaded
  private static Class<?> getClass(String className) {
    try {
      return Class.forName(className, true, getContextOrDefaultClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to locate class to load via reflection: " + className, e);
    }
  }

  // constructor-based reflection logic
  @SuppressWarnings("unchecked")
  private static <T> T createInstance(
      String className,
      Configuration conf,
      Class<?>[] paramTypes,
      Object[] params) {
    try {
      Class<?> clazz = getClass(className);
      Constructor<T> ctor = paramTypes == null
          ? (Constructor<T>) clazz.getDeclaredConstructor()
          : (Constructor<T>) clazz.getDeclaredConstructor(paramTypes);
      ctor.setAccessible(true);
      T instance = ctor.newInstance(params);
      log.info("Successfully loaded class: " + className);
      ReflectionUtils.setConf(instance, conf);
      log.debug("Configured instance to use conf");
      return instance;

    } catch (NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException("Unable to find constructor of class: " + className, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Class being loaded is not accessible: " + className, e);
    } catch (InstantiationException e) {
      throw new RuntimeException("Unable to instantiate class: " + className, e);
    }
  }

  private static ClassLoader getContextOrDefaultClassLoader() {
    return Optional.of(Thread.currentThread().getContextClassLoader())
        .orElseGet(DynamoDBReflectionUtils::getDefaultClassLoader);
  }

  private static ClassLoader getDefaultClassLoader() {
    return DynamoDBReflectionUtils.class.getClassLoader();
  }
}
