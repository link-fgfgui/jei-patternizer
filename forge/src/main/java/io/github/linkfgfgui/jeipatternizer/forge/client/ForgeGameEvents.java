package io.github.linkfgfgui.jeipatternizer.forge.client;

import io.github.linkfgfgui.jeipatternizer.client.PatternizeInput;
import io.github.linkfgfgui.jeipatternizer.client.ReloadMemory;
import net.minecraftforge.client.event.ScreenEvent;

public final class ForgeGameEvents {

	private ForgeGameEvents() {
	}

	public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
		PatternizeInput.onKeyPressed(event.getKeyCode(), event.getScanCode(), event.getScreen());
	}

	public static void onScreenOpening(ScreenEvent.Opening event) {
		ReloadMemory.onScreenOpening(event.getScreen());
	}
}
