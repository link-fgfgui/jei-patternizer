package io.github.linkfgfgui.jeipatternizer.client;

import io.github.linkfgfgui.jeipatternizer.config.JeiPatternizerConfig;
import io.github.linkfgfgui.jeipatternizer.integrated.PatternTerminalApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * When the player opens a pattern-access terminal, wait for the server to finish streaming the
 * stored patterns to the client, then import their output ids into {@link EncodedItems} so the
 * next encode session skips recipes the player already has.
 */
public final class ReloadMemory {

	private ReloadMemory() {
	}

	public static void onScreenOpening(Screen screen) {
		if (!PatternTerminalApi.isValidAccessScreen(screen)) {
			return;
		}
		PatternTerminalApi api = PatternTerminalApi.forAccessScreen(screen);
		if (api == null) {
			return;
		}
		JeiPatternizerConfig.load();
		EncodedItems.clear();
		Minecraft minecraft = Minecraft.getInstance();
		long delay = JeiPatternizerConfig.delayBeforeRead();
		CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() ->
			minecraft.execute(() -> {
				long patternCount = api.importExistingPatterns(minecraft.level);
				if (patternCount > 0 && JeiPatternizerConfig.showLoadMessage() && minecraft.player != null) {
					minecraft.player.displayClientMessage(
						Component.translatable("chat.jeipatternizer.loaded", EncodedItems.size(), patternCount),
						false
					);
				}
			})
		);
	}
}
