package com.kuaidui.pure;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

public final class ReflectUtils {
    private static final String TAG = "KuaiDuiPure";

    private ReflectUtils() {}

    public static Class<?> findClass(String name, ClassLoader cl) {
        if (name == null) return null;
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        if (clazz == null) return null;
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                break;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static boolean hookMethod(XposedInterface xposed, String className, ClassLoader cl,
                                     String methodName, Class<?>[] params, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClass(className, cl);
        if (clazz == null) return false;
        Method m = findMethod(clazz, methodName, params);
        if (m == null) return false;
        try {
            xposed.hook(m).intercept(hooker);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "hookMethod failed: " + className + "#" + methodName + " - " + t);
            return false;
        }
    }

    public static int hookAllMethods(XposedInterface xposed, String className, ClassLoader cl,
                                     String methodName, XposedInterface.Hooker hooker) {
        return hookAllMethods(xposed, findClass(className, cl), methodName, hooker);
    }

    public static int hookAllMethods(XposedInterface xposed, Class<?> clazz, String methodName,
                                     XposedInterface.Hooker hooker) {
        if (clazz == null || methodName == null) return 0;
        int count = 0;
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                try {
                    m.setAccessible(true);
                    xposed.hook(m).intercept(hooker);
                    count++;
                } catch (Throwable t) {
                    Log.w(TAG, "hookAllMethods failed: " + m + " - " + t);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "getDeclaredMethods failed: " + clazz.getName() + " - " + t);
        }
        return count;
    }

    public static int hookAllMethodsDefault(XposedInterface xposed, String className, ClassLoader cl,
                                            String methodName, Object objectResult) {
        Class<?> clazz = findClass(className, cl);
        if (clazz == null) return 0;
        int count = 0;
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                try {
                    m.setAccessible(true);
                    Class<?> rt = m.getReturnType();
                    Object result;
                    if (rt == void.class) result = null;
                    else if (rt == boolean.class || rt == Boolean.class) result = Boolean.FALSE;
                    else if (rt == int.class || rt == Integer.class) result = 0;
                    else if (rt == long.class || rt == Long.class) result = 0L;
                    else result = objectResult;
                    final Object finalResult = result;
                    xposed.hook(m).intercept(chain -> finalResult);
                    count++;
                } catch (Throwable t) {
                    Log.w(TAG, "hookAllMethodsDefault failed: " + m + " - " + t);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "getDeclaredMethods failed: " + clazz.getName() + " - " + t);
        }
        return count;
    }

    public static void setField(Object target, String name, Object value) {
        if (target == null || name == null) return;
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return;
            }
            c = c.getSuperclass();
        }
    }
}
