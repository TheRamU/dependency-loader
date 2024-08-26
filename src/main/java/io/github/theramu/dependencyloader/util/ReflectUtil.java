package io.github.theramu.dependencyloader.util;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;

/**
 * @author TheRamU
 * @since 2022/7/24 11:02
 */
public class ReflectUtil {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadJarFile(File file) throws Exception {
        ClassLoader classLoader = ReflectUtil.class.getClassLoader();

        Class<?> urlClassLoaderClass = findClassContainingField(classLoader.getClass(), "ucp");
        if (urlClassLoaderClass == null) {
            throw new IllegalStateException("Could not find URLClassLoader class");
        }

        Field ucpField = urlClassLoaderClass.getDeclaredField("ucp");
        long ucpOffset = UNSAFE.objectFieldOffset(ucpField);
        Object ucp = UNSAFE.getObject(classLoader, ucpOffset);

        Field pathField = ucp.getClass().getDeclaredField("path");
        long pathOffset = UNSAFE.objectFieldOffset(pathField);
        Collection<URL> path = (Collection<URL>) UNSAFE.getObject(ucp, pathOffset);

        Field urlsField;
        try {
            urlsField = ucp.getClass().getDeclaredField("unopenedUrls");
        } catch (NoSuchFieldException e) {
            urlsField = ucp.getClass().getDeclaredField("urls");
        }
        long urlsOffset = UNSAFE.objectFieldOffset(urlsField);
        Collection<URL> urls = (Collection<URL>) UNSAFE.getObject(ucp, urlsOffset);

        URL url = file.toURI().toURL();
        if (!path.contains(url)) {
            path.add(url);
            urls.add(url);
        }
    }

    private static Class<?> findClassContainingField(Class<?> clazz, String fieldName) {
        if (hasField(clazz, fieldName)) {
            return clazz;
        } else {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) return null;
            return findClassContainingField(superclass, fieldName);
        }
    }

    private static boolean hasField(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
