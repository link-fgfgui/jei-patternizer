package io.github.linkfgfgui.jeipatternizer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lightweight JSON config stored in the platform config directory.
 * Values mirror EMI-Patternizer's timing knobs so the later encode pipeline can reuse them.
 */
public final class JeiPatternizerConfig {
	private static final String FILE_NAME = Constants.MOD_ID + ".json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static long delayPerOperation = 60L;
	private static long delayAdditionalPerPattern = 20L;
	private static long delayBeforeRead = 1000L;
	private static boolean playSound = true;
	private static boolean simulateClick = false;
	private static boolean showLoadMessage = true;

	private JeiPatternizerConfig() {
	}

	public static void load() {
		Path configFile = Services.PLATFORM.getConfigDir().resolve(FILE_NAME);
		if (Files.isRegularFile(configFile)) {
			try (Reader reader = Files.newBufferedReader(configFile)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				delayPerOperation = getLong(json, "delayPerOperation", delayPerOperation);
				delayAdditionalPerPattern = getLong(json, "delayAdditionalPerPattern", delayAdditionalPerPattern);
				delayBeforeRead = getLong(json, "delayBeforeRead", delayBeforeRead);
				playSound = getBoolean(json, "playSound", playSound);
				simulateClick = getBoolean(json, "simulateClick", simulateClick);
				showLoadMessage = getBoolean(json, "showLoadMessage", showLoadMessage);
			} catch (Exception exception) {
				Constants.LOG.error("Failed to read config file {}", configFile, exception);
			}
		} else {
			save();
		}
	}

	public static void save() {
		Path configFile = Services.PLATFORM.getConfigDir().resolve(FILE_NAME);
		try {
			Files.createDirectories(configFile.getParent());
			try (Writer writer = Files.newBufferedWriter(configFile)) {
				JsonObject json = new JsonObject();
				json.addProperty("delayPerOperation", delayPerOperation);
				json.addProperty("delayAdditionalPerPattern", delayAdditionalPerPattern);
				json.addProperty("delayBeforeRead", delayBeforeRead);
				json.addProperty("playSound", playSound);
				json.addProperty("simulateClick", simulateClick);
				json.addProperty("showLoadMessage", showLoadMessage);
				GSON.toJson(json, writer);
			}
		} catch (IOException exception) {
			Constants.LOG.error("Failed to write config file {}", configFile, exception);
		}
	}

	public static long delayPerOperation() {
		return delayPerOperation;
	}

	public static long delayAdditionalPerPattern() {
		return delayAdditionalPerPattern;
	}

	public static long delayBeforeRead() {
		return delayBeforeRead;
	}

	public static boolean playSound() {
		return playSound;
	}

	public static boolean simulateClick() {
		return simulateClick;
	}

	public static boolean showLoadMessage() {
		return showLoadMessage;
	}

	private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
		return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
	}

	private static long getLong(JsonObject json, String key, long defaultValue) {
		return json.has(key) ? json.get(key).getAsLong() : defaultValue;
	}
}
