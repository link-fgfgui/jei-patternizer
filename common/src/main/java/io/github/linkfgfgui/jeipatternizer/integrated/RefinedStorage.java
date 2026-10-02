package io.github.linkfgfgui.jeipatternizer.integrated;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/**
 * Refined Storage placeholder.
 * <p>
 * EMI-Patternizer targets RS2 (Minecraft 1.21), whose API is completely different from the
 * Refined Storage 1.x line shipped for 1.20.1. Until that port is written, RS reports "not a
 * terminal" so the encode pipeline stays AE2-only instead of half-working against unknown slots.
 */
final class RefinedStorage implements PatternTerminalApi {

	RefinedStorage(AbstractContainerMenu menu) {
	}

	RefinedStorage(AbstractContainerScreen<?> screen) {
	}

	static boolean isEncodingMenu(AbstractContainerMenu menu) {
		return false;
	}

	static boolean isEncodingScreen(Screen screen) {
		return false;
	}

	static boolean isAccessScreen(Screen screen) {
		return false;
	}

	@Override
	public void encode(boolean simulateClick) {
	}

	@Override
	public int getEncodedPatternSlot() {
		return -1;
	}

	@Override
	public boolean hasBlankPattern() {
		return false;
	}

	@Override
	public boolean hasEncodedPattern() {
		return false;
	}

	@Override
	public boolean isEncodedSlotEmpty() {
		return true;
	}

	@Override
	public boolean hasEncodableInput() {
		return false;
	}

	@Override
	public String getMode() {
		return MODE_CRAFTING;
	}

	@Override
	public void setMode(String mode) {
	}

	@Override
	public long importExistingPatterns(Level level) {
		return 0;
	}
}
