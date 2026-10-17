package solim.mcp.introspection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;
import solim.reactive.Effect;
import solim.reactive.Signal;

class ReflectAccessTest extends SolimEnv {

	@Test
	void readsPrivateField() {
		Signal<Integer> signal = Signal.of(7);
		Field value = ReflectAccess.field(Signal.class, "value");
		assertNotNull(value);
		assertEquals(7, ReflectAccess.read(value, signal));
	}

	@Test
	void missingFieldReturnsNull() {
		assertNull(ReflectAccess.field(Signal.class, "definitelyMissing"));
	}

	@Test
	void fieldCached() {
		Field a = ReflectAccess.field(Signal.class, "value");
		Field b = ReflectAccess.field(Signal.class, "value");
		assertNotNull(a);
		assertSame(a, b, "Resolved fields should be cached");
	}

	@Test
	void invokesPackagePrivateCountMethod() {
		Signal<Integer> signal = Signal.of(1);
		Effect e = Effect.of(signal, v -> {});
		Method observers = ReflectAccess.method(Signal.class, "observerCount");
		assertNotNull(observers);
		assertEquals(1, ReflectAccess.invoke(observers, signal));
		e.dispose();
	}

	@Test
	void missingMethodReturnsNull() {
		assertNull(ReflectAccess.method(Signal.class, "definitelyMissing"));
	}

	@Test
	void readOfNullTargetReturnsNull() {
		assertNull(ReflectAccess.read(ReflectAccess.field(Signal.class, "value"), (Object) null));
	}
}