package io.github.linkfgfgui.jeipatternizer.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * Platform-neutral key hook. Pressing the patternize key while an AE2/RS encoding terminal is
 * open starts a batch encode session for the recipe page currently shown in JEI.
 */
public final class PatternizeInput {

	private PatternizeInput() {
	}

	public static void onKeyPressed(int keyCode, int scanCode, Screen screen) {
		if (!JeiPatternizerKeys.isPatternizeKey(keyCode, scanCode)) {
			return;
		}
		PatternizerSession.requestStart(screen);
	}
}
