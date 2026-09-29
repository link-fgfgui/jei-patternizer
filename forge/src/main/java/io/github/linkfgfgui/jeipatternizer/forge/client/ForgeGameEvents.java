package io.github.linkfgfgui.jeipatternizer.forge.client;

import io.github.linkfgfgui.jeipatternizer.client.PatternizeInput;
import net.minecraftforge.client.event.ScreenEvent;

public final class ForgeGameEvents {

	private ForgeGameEvents() {
	}

	public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
		PatternizeInput.onKeyPressed(event.getKeyCode(), event.getScanCode(), event.getScreen());
	}
}
