package mindustrytool.services.crash;

import arc.struct.Seq;
import arc.util.Strings;
import java.util.function.Supplier;

/** Single Responsibility: enrich raw crash log with enabled-feature metadata. */
public final class CrashLogEnricher {

	private final Supplier<Seq<String>> enabledFeatureNames;

	public CrashLogEnricher(Supplier<Seq<String>> enabledFeatureNames) {
		this.enabledFeatureNames = enabledFeatureNames;
	}

	/**
	 * If log contains a double-newline separator, inserts enabled-feature list after first block.
	 * Otherwise returns log unchanged. Caller already verified the log contains "mindustry-tool".
	 */
	public String enrich(String log) {
		if (log == null) return "";

		int separatorIndex = log.indexOf("\n\n");
		if (separatorIndex == -1) {
			return log;
		}

		String firstPart = log.substring(0, separatorIndex);
		String secondPart = log.substring(separatorIndex);

		Seq<String> names;
		try {
			names = enabledFeatureNames.get();
		} catch (Exception e) {
			names = new Seq<>();
		}

		String featureString = (names != null && names.size > 0) ? Strings.join(",", names) : "None";
		return firstPart + "\nEnabled features: " + featureString + secondPart;
	}
}
