package solim.mcp.introspection;

import arc.util.Nullable;

/**
 * A discovered reactive instance (signal, computed, effect, or subscription) plus where it was
 * found, as a human-readable location such as {@code "feature-settings-dialog/minScale/::Signal"}.
 */
public final class ReactiveRef {
	public final Object instance;
	public final ReactiveKind kind;
	public final String location;
	public final @Nullable String sourceElement;
	public final @Nullable String fieldName;

	public ReactiveRef(Object instance, ReactiveKind kind, String location, @Nullable String sourceElement,
			@Nullable String fieldName) {
		this.instance = instance;
		this.kind = kind;
		this.location = location;
		this.sourceElement = sourceElement;
		this.fieldName = fieldName;
	}

	public String id() {
		return location;
	}
}