package io.github.linkfgfgui.jeipatternizer;

import net.fabricmc.api.ModInitializer;

public class JeiPatternizer implements ModInitializer {

	@Override
	public void onInitialize() {
		Constants.LOG.info("Hello Fabric world!");
		CommonClass.init();
	}
}
