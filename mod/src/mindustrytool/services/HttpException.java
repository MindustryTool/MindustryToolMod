package mindustrytool.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import arc.util.Nullable;

public class HttpException extends RuntimeException {

	private final int statusCode;
	private final @Nullable JsonNode errorBody;
	private final @Nullable String rawBody;
	private final Map<String, List<String>> headers;

	public HttpException(
			int statusCode,
			@Nullable JsonNode errorBody,
			@Nullable String rawBody,
			@Nullable Map<String, List<String>> headers) {
		super("HTTP " + statusCode + (rawBody != null && !rawBody.trim().isEmpty() ? ": " + rawBody : ""));
		this.statusCode = statusCode;
		this.errorBody = errorBody;
		this.rawBody = rawBody;
		this.headers = headers != null ? headers : Collections.emptyMap();
	}

	public int statusCode() {
		return statusCode;
	}

	public @Nullable JsonNode errorBody() {
		return errorBody;
	}

	public @Nullable String rawBody() {
		return rawBody;
	}

	public Map<String, List<String>> headers() {
		return headers;
	}

	public @Nullable String header(String name) {
		if (headers == null || name == null) return null;
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (name.equalsIgnoreCase(entry.getKey())) {
				List<String> values = entry.getValue();
				return (values != null && !values.isEmpty()) ? values.get(0) : null;
			}
		}
		return null;
	}
}
