package io.github.linkfgfgui.jeipatternizer.fabric.client;

import io.github.linkfgfgui.jeipatternizer.client.JeiPatternizerKeys;
import io.github.linkfgfgui.jeipatternizer.client.PatternizeInput;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;

public class JeiPatternizerClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(JeiPatternizerKeys.PATTERNIZE);

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
			ScreenKeyboardEvents.afterKeyPress(screen).register((openScreen, key, scancode, modifiers) ->
				PatternizeInput.onKeyPressed(key, scancode, openScreen)
			)
		);
	}
}
