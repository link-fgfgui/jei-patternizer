package io.github.linkfgfgui.jeipatternizer.client;

import io.github.linkfgfgui.jeipatternizer.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * Base JEI plugin whose only job is to publish the {@link IJeiRuntime} for the encode pipeline.
 * <p>
 * JEI discovers plugins differently per loader: Forge scans for {@code @JeiPlugin} classes while
 * Fabric uses the {@code jei_mod_plugin} entrypoint. Each loader module therefore provides a tiny
 * annotated subclass, and this shared class stays unannotated to avoid being registered twice on
 * Forge.
 */
public class JeiPatternizerJeiPlugin implements IModPlugin {

	private static final ResourceLocation UID = new ResourceLocation(Constants.MOD_ID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		JeiRuntimeHolder.set(jeiRuntime);
	}

	@Override
	public void onRuntimeUnavailable() {
		JeiRuntimeHolder.set(null);
	}
}
