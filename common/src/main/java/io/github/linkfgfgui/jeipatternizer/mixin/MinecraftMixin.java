package io.github.linkfgfgui.jeipatternizer.mixin;

import io.github.linkfgfgui.jeipatternizer.client.PatternizerSession;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the encode state machine from the client tick. Loader-neutral (unlike Forge/Fabric tick
 * events) and independent of jeicrafter's own tick hook.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void jeipatternizer$tick(CallbackInfo callbackInfo) {
		PatternizerSession.tick();
	}
}
