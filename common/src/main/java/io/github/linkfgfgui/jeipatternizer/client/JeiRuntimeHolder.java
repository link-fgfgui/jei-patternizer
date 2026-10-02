package io.github.linkfgfgui.jeipatternizer.client;

import mezz.jei.api.runtime.IJeiRuntime;

import javax.annotation.Nullable;

/**
 * Holds the {@link IJeiRuntime} injected by {@link JeiPatternizerJeiPlugin}.
 * <p>
 * The pipeline needs JEI's recipe manager / transfer manager, but neither loader exposes a
 * loader-neutral way to reach them; a small JEI plugin is the portable answer.
 */
public final class JeiRuntimeHolder {

	@Nullable
	private static volatile IJeiRuntime runtime;

	private JeiRuntimeHolder() {
	}

	public static void set(@Nullable IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Nullable
	public static IJeiRuntime get() {
		return runtime;
	}
}
