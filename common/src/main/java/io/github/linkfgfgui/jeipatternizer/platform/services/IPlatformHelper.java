package io.github.linkfgfgui.jeipatternizer.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {

	String getPlatformName();

	Path getConfigDir();

	boolean isModLoaded(String modId);

	boolean isDevelopmentEnvironment();

	default String getEnvironmentName() {
		return isDevelopmentEnvironment() ? "development" : "production";
	}
}
