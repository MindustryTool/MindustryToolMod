package solim.display.video.foreign;

/**
 * A block of off-heap native memory with the typed accessors the Media Foundation decoder
 * needs. Allocation and lifetime belong to the {@link ForeignRuntime} arena that produced it.
 */
public final class ForeignMemory {

	private final ForeignRuntime runtime;
	private final Object segment;

	ForeignMemory(ForeignRuntime runtime, Object segment) {
		this.runtime = runtime;
		this.segment = segment;
	}

	Object segment() {
		return segment;
	}

	public long address() {
		return runtime.addressOf(segment);
	}

	public boolean isNull() {
		return address() == 0L;
	}

	public int getInt(long offset) {
		return runtime.readInt(segment, offset);
	}

	public long getLong(long offset) {
		return runtime.readLong(segment, offset);
	}

	public short getShort(long offset) {
		return runtime.readShort(segment, offset);
	}

	public ForeignMemory getPointer(long offset) {
		return runtime.pointer(getLong(offset));
	}

	public void setInt(long offset, int value) {
		runtime.writeInt(segment, offset, value);
	}

	public void setLong(long offset, long value) {
		runtime.writeLong(segment, offset, value);
	}

	public void setShort(long offset, short value) {
		runtime.writeShort(segment, offset, value);
	}

	public void setByte(long offset, byte value) {
		runtime.writeByte(segment, offset, value);
	}

	public void clear() {
		runtime.fill(segment, (byte) 0);
	}

	public void writeBytes(byte[] data) {
		runtime.writeBytes(segment, data);
	}

	public void readBytes(byte[] destination, int length) {
		runtime.readBytes(segment, 0L, destination, length);
	}
}
