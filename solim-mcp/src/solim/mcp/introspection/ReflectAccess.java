package solim.mcp.introspection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import arc.util.Log;
import arc.util.Nullable;

public final class ReflectAccess {
    private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> MISS_CACHE = new ConcurrentHashMap<>();

    private ReflectAccess() {
    }

    private static String fieldKey(Class<?> type, String name) {
        return type.getName() + "#" + name;
    }

    private static String methodKey(Class<?> type, String name, Class<?>[] params) {
        StringBuilder sb = new StringBuilder(type.getName()).append("#").append(name).append('(');
        for (Class<?> p : params) {
            sb.append(p.getName()).append(',');
        }
        return sb.append(')').toString();
    }

    public static @Nullable Field field(Class<?> type, String name) {
        String key = fieldKey(type, name);
        Field cached = FIELD_CACHE.get(key);
        if (cached != null)
            return cached;
        if (Boolean.TRUE.equals(MISS_CACHE.get(key)))
            return null;
        Field found = null;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                found = f;
                break;
            } catch (NoSuchFieldException ignored) {

            }
        }
        if (found != null) {
            FIELD_CACHE.put(key, found);
        } else {
            MISS_CACHE.put(key, Boolean.TRUE);
            Log.warn("[solim-mcp] reflective field '{0}' not found on {1}; introspection degraded", name,
                    type.getName());
        }
        return found;
    }

    public static @Nullable Method method(Class<?> type, String name, Class<?>... params) {
        String key = methodKey(type, name, params);
        Method cached = METHOD_CACHE.get(key);
        if (cached != null)
            return cached;
        if (Boolean.TRUE.equals(MISS_CACHE.get(key)))
            return null;
        Method found = null;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                found = m;
                break;
            } catch (NoSuchMethodException ignored) {

            }
        }
        if (found != null) {
            METHOD_CACHE.put(key, found);
        } else {
            MISS_CACHE.put(key, Boolean.TRUE);
            Log.warn("[solim-mcp] reflective method '{0}' not found on {1}; introspection degraded",
                    name, type.getName());
        }
        return found;
    }

    public static Object read(Field field, Object target) {
        if (field == null || (target == null && !isStatic(field)))
            return null;
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            try {
                field.setAccessible(true);
                return field.get(target);
            } catch (RuntimeException | IllegalAccessException retryFailed) {
                Log.warn("[solim-mcp] read of '{0}' failed", field.getName());
                return null;
            }
        }
    }

    public static Object invoke(Method method, Object target) {
        if (method == null || target == null)
            return null;
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            Log.warn("[solim-mcp] invoke of '{0}' failed: {1}", method.getName(), e.getMessage());
            return null;
        }
    }

    public static boolean isInstance(Object value, Class<?> type) {
        return value != null && type.isInstance(value);
    }

    public static boolean isStatic(Field field) {
        return field != null && Modifier.isStatic(field.getModifiers());
    }
}
