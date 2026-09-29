package io.github.linkfgfgui.jeipatternizer.forge.client;

import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.client.JeiPatternizerKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClientEvents {

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(JeiPatternizerKeys.PATTERNIZE);
	}
}
