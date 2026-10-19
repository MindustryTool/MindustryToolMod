package solim.display.video.foreign;

import arc.util.Log;
import arc.util.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflective bridge over the Foreign Function & Memory API (JDK 22+).
 *
 * <p>The mod compiles against Java 17, where {@code java.lang.foreign} does not exist, so
 * every entry point is reached reflectively and only resolved when the API is present at
 * runtime. Mindustry's shipped Windows runtime is a jlink image with just {@code java.base}
 * and {@code jdk.unsupported}; it omits {@code java.util.logging}, which JNA's core classes
 * require, so JNA cannot be used there while this API can.
 */
public final class ForeignRuntime {

	private static final String ROOT = "java.lang.foreign.";
	private static final long POINTER_SIZE = 8;

	private static volatile ForeignRuntime singleton;
	private static volatile boolean resolved;

	private final Object arena;
	private final Object linker;
	private final Object loaderLookup;
	private final Object[] noOptions;
	private final Class<?> layoutClass;
	private final Method allocate;
	private final Method lookupFind;
	private final Method downcallHandle;
	private final Method descriptorOf;
	private final Method segmentAddress;
	private final Method segmentOfAddress;
	private final Method segmentReinterpret;
	private final Method segmentFill;
	private final Method copyInto;
	private final Method copyOut;
	private final Method getInt;
	private final Method getLong;
	private final Method getShort;
	// private final Method getByte;
	private final Method setInt;
	private final Method setLong;
	private final Method setShort;
	private final Method setByte;
	private final Object layoutInt;
	private final Object layoutLong;
	private final Object layoutShort;
	private final Object layoutByte;
	private final Object layoutAddress;
	// Decoders are opened and closed on worker threads, so both caches are written
	// concurrently. A plain HashMap can be corrupted by that, which surfaces as a crash
	// inside the JVM rather than as an exception.
	private final Map<Long, MethodHandle> functions = new ConcurrentHashMap<>();
	private final Map<String, Long> symbols = new ConcurrentHashMap<>();

	private ForeignRuntime() throws ReflectiveOperationException {
		Class<?> arenaClass = Class.forName(ROOT + "Arena");
		Class<?> segmentClass = Class.forName(ROOT + "MemorySegment");
		Class<?> lookupClass = Class.forName(ROOT + "SymbolLookup");
		Class<?> linkerClass = Class.forName(ROOT + "Linker");
		Class<?> descriptorClass = Class.forName(ROOT + "FunctionDescriptor");
		layoutClass = Class.forName(ROOT + "MemoryLayout");
		Class<?> valueLayoutClass = Class.forName(ROOT + "ValueLayout");
		Class<?> intLayoutClass = Class.forName(ROOT + "ValueLayout$OfInt");
		Class<?> longLayoutClass = Class.forName(ROOT + "ValueLayout$OfLong");
		Class<?> shortLayoutClass = Class.forName(ROOT + "ValueLayout$OfShort");
		Class<?> byteLayoutClass = Class.forName(ROOT + "ValueLayout$OfByte");
		Class<?> optionClass = Class.forName(ROOT + "Linker$Option");
		Class<?> optionArrayClass = Array.newInstance(optionClass, 0).getClass();

		arena = arenaClass.getMethod("ofShared").invoke(null);
		allocate = arenaClass.getMethod("allocate", long.class);
		linker = linkerClass.getMethod("nativeLinker").invoke(null);
		loaderLookup = lookupClass.getMethod("loaderLookup").invoke(null);
		lookupFind = lookupClass.getMethod("find", String.class);
		downcallHandle = linkerClass.getMethod("downcallHandle", segmentClass, descriptorClass, optionArrayClass);
		descriptorOf = descriptorClass.getMethod("of", layoutClass, Array.newInstance(layoutClass, 0).getClass());
		segmentAddress = segmentClass.getMethod("address");
		segmentOfAddress = segmentClass.getMethod("ofAddress", long.class);
		segmentReinterpret = segmentClass.getMethod("reinterpret", long.class);
		segmentFill = segmentClass.getMethod("fill", byte.class);
		copyInto = segmentClass.getMethod("copy", Object.class, int.class, segmentClass, valueLayoutClass, long.class, int.class);
		copyOut = segmentClass.getMethod("copy", segmentClass, valueLayoutClass, long.class, Object.class, int.class, int.class);
		getInt = segmentClass.getMethod("get", intLayoutClass, long.class);
		getLong = segmentClass.getMethod("get", longLayoutClass, long.class);
		getShort = segmentClass.getMethod("get", shortLayoutClass, long.class);
		// getByte = segmentClass.getMethod("get", byteLayoutClass, long.class);
		setInt = segmentClass.getMethod("set", intLayoutClass, long.class, int.class);
		setLong = segmentClass.getMethod("set", longLayoutClass, long.class, long.class);
		setShort = segmentClass.getMethod("set", shortLayoutClass, long.class, short.class);
		setByte = segmentClass.getMethod("set", byteLayoutClass, long.class, byte.class);
		layoutInt = valueLayoutClass.getField("JAVA_INT").get(null);
		layoutLong = valueLayoutClass.getField("JAVA_LONG").get(null);
		layoutShort = valueLayoutClass.getField("JAVA_SHORT").get(null);
		layoutByte = valueLayoutClass.getField("JAVA_BYTE").get(null);
		layoutAddress = valueLayoutClass.getField("ADDRESS").get(null);
		noOptions = (Object[]) Array.newInstance(optionClass, 0);
	}

	public static boolean isAvailable() {
		return instance() != null;
	}

	public static ForeignRuntime get() {
		ForeignRuntime value = instance();
		if (value == null)
			throw new IllegalStateException("Foreign Function & Memory API is not available");
		return value;
	}

	private static @Nullable ForeignRuntime instance() {
		if (resolved)
			return singleton;
		synchronized (ForeignRuntime.class) {
			if (!resolved) {
				try {
					singleton = new ForeignRuntime();
				} catch (Throwable error) {
					Log.debug("SolimVideo: native support unavailable (" + error + ")");
				}
				resolved = true;
			}
		}
		return singleton;
	}

	public void loadLibrary(String name) {
		System.loadLibrary(name);
	}

	public long symbol(String name) {
		Long cached = symbols.get(name);
		if (cached != null)
			return cached;
		try {
			Optional<?> found = (Optional<?>) lookupFind.invoke(loaderLookup, name);
			if (!found.isPresent())
				throw new IllegalStateException("Native symbol not found: " + name);
			long address = (long) segmentAddress.invoke(found.get());
			symbols.put(name, address);
			return address;
		} catch (ReflectiveOperationException error) {
			throw fail("Resolve symbol " + name, error);
		}
	}

	public ForeignMemory allocate(long bytes) {
		try {
			return new ForeignMemory(this, allocate.invoke(arena, bytes));
		} catch (ReflectiveOperationException error) {
			throw fail("Allocate native memory", error);
		}
	}

	public ForeignMemory pointer(long address) {
		try {
			return new ForeignMemory(this, segmentOfAddress.invoke(null, address));
		} catch (ReflectiveOperationException error) {
			throw fail("Create native pointer", error);
		}
	}

	public ForeignMemory view(long address, long bytes) {
		try {
			return new ForeignMemory(this, segmentReinterpret.invoke(pointer(address).segment(), bytes));
		} catch (ReflectiveOperationException error) {
			throw fail("View native memory", error);
		}
	}

	public int invoke(long address, Object... arguments) {
		MethodHandle handle = handleFor(address, arguments);
		Object[] values = new Object[arguments.length];
		for (int index = 0; index < arguments.length; index++)
			values[index] = arguments[index] instanceof ForeignMemory
					? ((ForeignMemory) arguments[index]).segment()
					: arguments[index];
		try {
			return (int) handle.invokeWithArguments(values);
		} catch (Throwable error) {
			throw new IllegalStateException("Native call failed", error);
		}
	}

	public int vtableCall(ForeignMemory object, int methodIndex, Object[] arguments) {
		long vtable = readPointer(object.address());
		long function = readPointer(vtable + methodIndex * POINTER_SIZE);
		Object[] callArguments = new Object[arguments.length + 1];
		callArguments[0] = object;
		System.arraycopy(arguments, 0, callArguments, 1, arguments.length);
		return invoke(function, callArguments);
	}

	public ForeignMemory wideString(String value) {
		byte[] encoded = value.getBytes(StandardCharsets.UTF_16LE);
		ForeignMemory memory = allocate(encoded.length + 2L);
		memory.writeBytes(encoded);
		return memory;
	}

	long addressOf(Object segment) {
		try {
			return (long) segmentAddress.invoke(segment);
		} catch (ReflectiveOperationException error) {
			throw fail("Read native address", error);
		}
	}

	int readInt(Object segment, long offset) {
		try {
			return (int) getInt.invoke(segment, layoutInt, offset);
		} catch (ReflectiveOperationException error) {
			throw fail("Read int", error);
		}
	}

	long readLong(Object segment, long offset) {
		try {
			return (long) getLong.invoke(segment, layoutLong, offset);
		} catch (ReflectiveOperationException error) {
			throw fail("Read long", error);
		}
	}

	short readShort(Object segment, long offset) {
		try {
			return (short) getShort.invoke(segment, layoutShort, offset);
		} catch (ReflectiveOperationException error) {
			throw fail("Read short", error);
		}
	}

	void writeInt(Object segment, long offset, int value) {
		try {
			setInt.invoke(segment, layoutInt, offset, value);
		} catch (ReflectiveOperationException error) {
			throw fail("Write int", error);
		}
	}

	void writeLong(Object segment, long offset, long value) {
		try {
			setLong.invoke(segment, layoutLong, offset, value);
		} catch (ReflectiveOperationException error) {
			throw fail("Write long", error);
		}
	}

	void writeShort(Object segment, long offset, short value) {
		try {
			setShort.invoke(segment, layoutShort, offset, value);
		} catch (ReflectiveOperationException error) {
			throw fail("Write short", error);
		}
	}

	void writeByte(Object segment, long offset, byte value) {
		try {
			setByte.invoke(segment, layoutByte, offset, value);
		} catch (ReflectiveOperationException error) {
			throw fail("Write byte", error);
		}
	}

	void fill(Object segment, byte value) {
		try {
			segmentFill.invoke(segment, value);
		} catch (ReflectiveOperationException error) {
			throw fail("Clear native memory", error);
		}
	}

	void writeBytes(Object segment, byte[] data) {		try {
			copyInto.invoke(null, data, 0, segment, layoutByte, 0L, data.length);
		} catch (ReflectiveOperationException error) {
			throw fail("Write native bytes", error);
		}
	}

	void readBytes(Object segment, long offset, byte[] destination, int length) {
		try {
			copyOut.invoke(null, segment, layoutByte, offset, destination, 0, length);
		} catch (ReflectiveOperationException error) {
			throw fail("Read native bytes", error);
		}
	}

	private long readPointer(long address) {
		return readLong(view(address, POINTER_SIZE).segment(), 0L);
	}

	private MethodHandle handleFor(long address, Object[] arguments) {
		MethodHandle cached = functions.get(address);
		if (cached != null)
			return cached;
		try {
			Object layouts = Array.newInstance(layoutClass, arguments.length);
			for (int index = 0; index < arguments.length; index++)
				Array.set(layouts, index, layoutOf(arguments[index]));
			Object descriptor = descriptorOf.invoke(null, layoutInt, layouts);
			MethodHandle created = (MethodHandle) downcallHandle.invoke(linker, pointer(address).segment(), descriptor, noOptions);
			functions.put(address, created);
			return created;
		} catch (ReflectiveOperationException error) {
			throw fail("Resolve native function", error);
		}
	}

	private Object layoutOf(Object argument) {
		if (argument instanceof ForeignMemory)
			return layoutAddress;
		if (argument instanceof Long)
			return layoutLong;
		if (argument instanceof Short)
			return layoutShort;
		if (argument instanceof Byte)
			return layoutByte;
		return layoutInt;
	}

	private static IllegalStateException fail(String operation, ReflectiveOperationException error) {
		Throwable cause = error instanceof InvocationTargetException && error.getCause() != null
				? error.getCause()
				: error;
		return new IllegalStateException(operation + " failed: " + cause, cause);
	}
}
