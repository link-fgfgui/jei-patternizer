package io.github.linkfgfgui.jeipatternizer;

import io.github.linkfgfgui.jeipatternizer.forge.client.ForgeGameEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(Constants.MOD_ID)
public class JeiPatternizer {

	public JeiPatternizer() {
		CommonClass.init();
		if (FMLLoader.getDist() == Dist.CLIENT) {
			MinecraftForge.EVENT_BUS.addListener(ForgeGameEvents::onScreenKeyPressed);
		}
	}
}
