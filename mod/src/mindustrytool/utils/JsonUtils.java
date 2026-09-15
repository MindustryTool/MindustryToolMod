package mindustrytool.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.List;

public final class JsonUtils {

	private static final ObjectMapper mapper = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.registerModule(new JavaTimeModule());

	public static ObjectMapper mapper() {
		return mapper;
	}

	public static JsonNode readTree(String json) throws IOException {
		return mapper.readTree(json);
	}

	private JsonUtils() {}

	public static String toJson(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toJsonPretty(Object object) {
		try {
			return mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(Class<T> clazz, String json) {
		try {
			return mapper.readValue(json, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> List<T> fromJsonArray(Class<T> clazz, String json) {
		try {
			return mapper.readerForListOf(clazz).readValue(json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
