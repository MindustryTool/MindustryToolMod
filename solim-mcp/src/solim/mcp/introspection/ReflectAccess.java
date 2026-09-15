package solim.mcp.introspection;

import arc.util.Log;
import arc.util.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cached, gracefully-degrading reflective access to Solim internals.
 *
 * <p>Every lookup is cached (including misses) so a failed lookup is resolved once and degraded
 * to {@code null} on subsequent calls instead of throwing. This keeps the debug server functional
 * if a Solim field or method is renamed, moved, or removed.
 */
@SuppressWarnings("deprecation")
public final class ReflectAccess {
	private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, Boolean> MISS_CACHE = new ConcurrentHashMap<>();

	private ReflectAccess() {}

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

	/** Looks up a field on the type or its superclasses, or {@code null} if it does not exist. */
	public static @Nullable Field field(Class<?> type, String name) {
		String key = fieldKey(type, name);
		Field cached = FIELD_CACHE.get(key);
		if (cached != null) return cached;
		if (Boolean.TRUE.equals(MISS_CACHE.get(key))) return null;
		Field found = null;
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				found = f;
				break;
			} catch (NoSuchFieldException ignored) {
				// continue up the hierarchy
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

	/** Looks up an instance method on the type or its superclasses, or {@code null} if missing. */
	public static @Nullable Method method(Class<?> type, String name, Class<?>... params) {
		String key = methodKey(type, name, params);
		Method cached = METHOD_CACHE.get(key);
		if (cached != null) return cached;
		if (Boolean.TRUE.equals(MISS_CACHE.get(key))) return null;
		Method found = null;
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				Method m = c.getDeclaredMethod(name, params);
				m.setAccessible(true);
				found = m;
				break;
			} catch (NoSuchMethodException ignored) {
				// continue up the hierarchy
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

	/** Sets accessible on a field and returns it, or {@code null} if the field could not be made usable. */
	public static @Nullable Field accessible(Field field) {
		if (field == null) return null;
		if (!field.isAccessible()) {
			try {
				field.setAccessible(true);
			} catch (RuntimeException e) {
				Log.warn("[solim-mcp] cannot access field {0}", field.getName());
				return null;
			}
		}
		return field;
	}

	/** Safely reads a field value, returning {@code null} on any reflective failure. Static fields accept a {@code null} target. */
	public static Object read(Field field, Object target) {
		if (field == null || accessible(field) == null || (target == null && !isStatic(field))) return null;
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			Log.warn("[solim-mcp] read of '{0}' failed", field.getName());
			return null;
		}
	}

	/** Safely invokes a no-arg method, returning {@code null} on any reflective failure. */
	public static Object invoke(Method method, Object target) {
		if (method == null || target == null || !makeAccessible(method)) return null;
		try {
			return method.invoke(target);
		} catch (ReflectiveOperationException e) {
			Log.warn("[solim-mcp] invoke of '{0}' failed: {1}", method.getName(), e.getMessage());
			return null;
		}
	}

	private static boolean makeAccessible(Method method) {
		if (!method.isAccessible()) {
			try {
				method.setAccessible(true);
			} catch (RuntimeException e) {
				Log.warn("[solim-mcp] cannot access method {0}", method.getName());
				return false;
			}
		}
		return true;
	}

	/** Whether the cast to a type is possible; used to gate introspection on schema compatibility. */
	public static boolean isInstance(Object value, Class<?> type) {
		return value != null && type.isInstance(value);
	}

	/** Whether the given class is static or an instance-held inner/anon class. */
	public static boolean isStatic(Field field) {
		return field != null && Modifier.isStatic(field.getModifiers());
	}
}