package solim.mcp.introspection;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Method;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Reads the current state of a discovered reactive instance via reflection and maps it to a stable
 * debug DTO. Never mutates the inspected instance.
 */
public final class SignalIntrospector {
	private SignalIntrospector() {}

	/** Builds the stable DTO for a single discovered reactive instance. */
	public static ObjectNode snapshot(ReactiveRef ref, ObjectNode node) {
		node.put("id", ref.id());
		node.put("kind", ref.kind.name());
		node.put("location", ref.location);
		if (ref.sourceElement != null) node.put("element", ref.sourceElement);
		if (ref.fieldName != null) node.put("field", ref.fieldName);

		Object instance = ref.instance;
		Class<?> type = instance.getClass();

		switch (ref.kind) {
			case SIGNAL:
				node.set("value", IntrospectionJson.renderValue(ReflectAccess.read(ReflectAccess.field(type, "value"), instance)));
				node.put("valueClass", valueClass(instance));
				node.put("listeners", intOf(type, instance, "listenerCount"));
				node.put("observers", intOf(type, instance, "observerCount"));
				break;
			case COMPUTED:
				node.set("value", IntrospectionJson.renderValue(ReflectAccess.read(ReflectAccess.field(type, "cachedValue"), instance)));
				node.put("valueClass", valueClass(instance));
				node.put("listeners", intOf(type, instance, "listenerCount"));
				node.put("observers", intOf(type, instance, "observerCount"));
				node.put("dependencies", intOf(type, instance, "dependencyCount"));
				node.put("dirty", boolOf(type, instance, "isDirty"));
				node.put("disposed", boolOf(type, instance, "isDisposed"));
				break;
			case EFFECT:
				node.put("dependencies", intOf(type, instance, "dependencyCount"));
				node.put("disposed", boolOf(type, instance, "isDisposed"));
				break;
		case SUBSCRIPTION:
			node.put("disposed", boolOf(type, instance, "isDisposed"));
			break;
		case MAP:
			Object peeked = instance instanceof Readable ? ((Readable<?>) instance).peek() : null;
			node.set("value", IntrospectionJson.renderValue(peeked));
			node.put("valueClass", peeked != null ? peeked.getClass().getName() : "null");
			int mapObservers = intOf(type, instance, "mapObserverCount");
			int keyObservers = intOf(type, instance, "observerCount");
			node.put("observers", Math.max(mapObservers, keyObservers));
			node.put("cachedReadables", intOf(type, instance, "cachedReadableCount"));
			break;
		default:
			break;
		}
		return node;
	}

	private static String valueClass(Object instance) {
		Object value = instance instanceof Signal
			? ReflectAccess.read(ReflectAccess.field(instance.getClass(), "value"), instance)
			: ReflectAccess.read(ReflectAccess.field(instance.getClass(), "cachedValue"), instance);
		return value != null ? value.getClass().getName() : "null";
	}

	private static int intOf(Class<?> type, Object target, String methodName) {
		Method m = ReflectAccess.method(type, methodName);
		if (m == null) return -1;
		Object result = ReflectAccess.invoke(m, target);
		return result instanceof Number ? ((Number) result).intValue() : -1;
	}

	private static boolean boolOf(Class<?> type, Object target, String methodName) {
		Method m = ReflectAccess.method(type, methodName);
		if (m == null) return false;
		Object result = ReflectAccess.invoke(m, target);
		return Boolean.TRUE.equals(result);
	}
}
