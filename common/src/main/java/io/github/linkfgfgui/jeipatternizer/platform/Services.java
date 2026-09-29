package io.github.linkfgfgui.jeipatternizer.platform;

import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {

	public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

	private Services() {
	}

	public static <T> T load(Class<T> clazz) {
		final T loadedService = ServiceLoader.load(clazz)
			.findFirst()
			.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}
}
