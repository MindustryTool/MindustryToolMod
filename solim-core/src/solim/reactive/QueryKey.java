package solim.reactive;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, structured cache identity key for {@link Query} and {@link QueryCache}.
 */
public final class QueryKey {
	private final Object[] parts;
	private final int hashCode;

	private QueryKey(Object[] parts) {
		this.parts = parts;
		this.hashCode = Arrays.deepHashCode(parts);
	}

	public static QueryKey of(Object... parts) {
		Objects.requireNonNull(parts, "parts cannot be null");
		Object[] copy = new Object[parts.length];
		System.arraycopy(parts, 0, copy, 0, parts.length);
		return new QueryKey(copy);
	}

	public Object[] getParts() {
		Object[] copy = new Object[parts.length];
		System.arraycopy(parts, 0, copy, 0, parts.length);
		return copy;
	}

	public int size() {
		return parts.length;
	}

	public Object get(int index) {
		return parts[index];
	}

	public boolean startsWith(QueryKey prefix) {
		if (prefix == null || prefix.parts.length > this.parts.length) {
			return false;
		}
		for (int i = 0; i < prefix.parts.length; i++) {
			if (!Objects.deepEquals(this.parts[i], prefix.parts[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof QueryKey)) return false;
		QueryKey queryKey = (QueryKey) o;
		return Arrays.deepEquals(parts, queryKey.parts);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return Arrays.deepToString(parts);
	}
}
