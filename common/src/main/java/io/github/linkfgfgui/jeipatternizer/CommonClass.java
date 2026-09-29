package io.github.linkfgfgui.jeipatternizer;

import io.github.linkfgfgui.jeipatternizer.config.JeiPatternizerConfig;
import io.github.linkfgfgui.jeipatternizer.platform.Services;

public final class CommonClass {

	private CommonClass() {
	}

	public static void init() {
		Constants.LOG.info(
			"JEI Patternizer initialized on {}! Environment: {}",
			Services.PLATFORM.getPlatformName(),
			Services.PLATFORM.getEnvironmentName()
		);
		JeiPatternizerConfig.load();

		if (!Services.PLATFORM.isModLoaded("jeicrafter")) {
			Constants.LOG.warn("JEI Crafter is not loaded; recipe-tree pattern encoding will be unavailable.");
		}
	}
}
