package io.github.linkfgfgui.jeipatternizer.client;

import io.github.linkfgfgui.jeipatternizer.Constants;
import net.minecraft.client.gui.screens.Screen;

/**
 * Platform-neutral key hook. Encoding is not implemented yet; this only proves the MDK wires
 * screen input through to common code.
 */
public final class PatternizeInput {

	private PatternizeInput() {
	}

	public static void onKeyPressed(int keyCode, int scanCode, Screen screen) {
		if (!JeiPatternizerKeys.isPatternizeKey(keyCode, scanCode)) {
			return;
		}
		Constants.LOG.info(
			"Patternize key pressed on {}",
			screen == null ? "null" : screen.getClass().getName()
		);
	}
}
