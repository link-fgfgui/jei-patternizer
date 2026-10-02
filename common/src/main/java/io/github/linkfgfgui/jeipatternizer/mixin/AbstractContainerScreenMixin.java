package io.github.linkfgfgui.jeipatternizer.mixin;

import io.github.linkfgfgui.jeipatternizer.client.PatternizerSession;
import io.github.linkfgfgui.jeipatternizer.integrated.PatternTerminalApi;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks closing the encoding terminal with ESC while a batch encode is running, so the pipeline
 * is not torn apart mid-session.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
	private void jeipatternizer$preventClose(CallbackInfo callbackInfo) {
		Screen screen = (Screen) (Object) this;
		if (PatternizerSession.isOperating() && PatternTerminalApi.isValidEncodingScreen(screen)) {
			callbackInfo.cancel();
		}
	}
}
