package mindustrytool.utils;

import arc.util.Log;
import arc.util.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, fail-safe reflection utility.
 * <p>
 * Unlike standard reflection or Arc's {@code Reflect} which only inspects
 * directly declared members on the concrete runtime class, this utility traverses
 * the full class hierarchy up to {@link Object}. This ensures compatibility when
 * other mods (such as agzam4mod) subclass or wrap Mindustry UI fragments and classes.
 * Lookups are cached to avoid repeated reflection overhead.
 */
@SuppressWarnings("unchecked")
public final class ReflectUtil {

    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Field NO_FIELD;
    private static final Method NO_METHOD;

    static {
        Field sentinelField = null;
        Method sentinelMethod = null;
        try {
            sentinelField = Sentinel.class.getDeclaredField("SENTINEL");
            sentinelMethod = Sentinel.class.getDeclaredMethod("sentinel");
        } catch (Exception ignored) {
            // Should never occur
        }
        NO_FIELD = sentinelField;
        NO_METHOD = sentinelMethod;
    }

    private static final class Sentinel {
        @SuppressWarnings("unused")
        private static final int SENTINEL = 0;

        @SuppressWarnings("unused")
        private static void sentinel() {
        }
    }

    private ReflectUtil() {
    }

    /**
     * Finds a field by searching the given class and all of its superclasses.
     *
     * @param type the starting class
     * @param name the field name
     * @return the accessible {@link Field}, or {@code null} if not found
     */
    public static @Nullable Field findField(@Nullable Class<?> type, @Nullable String name) {
        if (type == null || name == null || type == Object.class) {
            return null;
        }

        String key = type.getName() + "#" + name;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached == NO_FIELD ? null : cached;
        }

        Field resolved = null;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field f = current.getDeclaredField(name);
                f.setAccessible(true);
                resolved = f;
                break;
            } catch (NoSuchFieldException ignored) {
                // Continue up the inheritance hierarchy
            } catch (Exception e) {
                Log.debug("ReflectUtil: Error accessing field @ on @: @", name, current.getName(), e.getMessage());
            }
        }

        FIELD_CACHE.put(key, resolved != null ? resolved : NO_FIELD);
        return resolved;
    }

    /**
     * Safely reads the value of a field on an object, traversing superclasses.
     * Returns {@code null} on failure without throwing an exception.
     */
    public static <T> @Nullable T getOrNull(@Nullable Object object, @Nullable String name) {
        if (object == null || name == null) {
            return null;
        }
        return getOrNull(object.getClass(), object, name);
    }

    /**
     * Safely reads the value of a field starting from a specific class type.
     * Returns {@code null} on failure without throwing an exception.
     */
    public static <T> @Nullable T getOrNull(@Nullable Class<?> type, @Nullable Object object, @Nullable String name) {
        Field field = findField(type, name);
        if (field == null) {
            return null;
        }
        try {
            return (T) field.get(object);
        } catch (Exception e) {
            Log.debug("ReflectUtil: Failed to get field @ on @: @", name, type != null ? type.getName() : "null", e.getMessage());
            return null;
        }
    }

    /**
     * Safely sets the value of a field on an object, traversing superclasses.
     *
     * @return {@code true} if successful, {@code false} otherwise
     */
    public static boolean setSafe(@Nullable Object object, @Nullable String name, @Nullable Object value) {
        if (object == null || name == null) {
            return false;
        }
        return setSafe(object.getClass(), object, name, value);
    }

    /**
     * Safely sets the value of a field starting from a specific class type.
     *
     * @return {@code true} if successful, {@code false} otherwise
     */
    public static boolean setSafe(@Nullable Class<?> type, @Nullable Object object, @Nullable String name, @Nullable Object value) {
        Field field = findField(type, name);
        if (field == null) {
            return false;
        }
        try {
            field.set(object, value);
            return true;
        } catch (Exception e) {
            Log.debug("ReflectUtil: Failed to set field @ on @: @", name, type != null ? type.getName() : "null", e.getMessage());
            return false;
        }
    }

    /**
     * Finds a method by searching the given class and all of its superclasses.
     *
     * @param type           the starting class
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return the accessible {@link Method}, or {@code null} if not found
     */
    public static @Nullable Method findMethod(@Nullable Class<?> type, @Nullable String name, Class<?>... parameterTypes) {
        if (type == null || name == null || type == Object.class) {
            return null;
        }

        String key = type.getName() + "#" + name + Arrays.toString(parameterTypes);
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == NO_METHOD ? null : cached;
        }

        Method resolved = null;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Method m = current.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                resolved = m;
                break;
            } catch (NoSuchMethodException ignored) {
                // Continue up the inheritance hierarchy
            } catch (Exception e) {
                Log.debug("ReflectUtil: Error accessing method @ on @: @", name, current.getName(), e.getMessage());
            }
        }

        METHOD_CACHE.put(key, resolved != null ? resolved : NO_METHOD);
        return resolved;
    }

    /**
     * Safely invokes a method on an object, traversing superclasses.
     * Returns {@code null} on failure without throwing an exception.
     */
    public static <T> @Nullable T invokeOrNull(@Nullable Object object, @Nullable String name, @Nullable Object[] args, Class<?>... parameterTypes) {
        if (object == null || name == null) {
            return null;
        }
        return invokeOrNull(object.getClass(), object, name, args, parameterTypes);
    }

    /**
     * Safely invokes a method on an object starting from a specific class type.
     * Returns {@code null} on failure without throwing an exception.
     */
    public static <T> @Nullable T invokeOrNull(@Nullable Class<?> type, @Nullable Object object, @Nullable String name, @Nullable Object[] args, Class<?>... parameterTypes) {
        Method method = findMethod(type, name, parameterTypes);
        if (method == null) {
            return null;
        }
        try {
            return (T) method.invoke(object, args);
        } catch (Exception e) {
            Log.debug("ReflectUtil: Failed to invoke method @ on @: @", name, type != null ? type.getName() : "null", e.getMessage());
            return null;
        }
    }

    /**
     * Clears all cached reflection entries.
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
        METHOD_CACHE.clear();
    }
}
