package io.github.linkfgfgui.jeipatternizer.integrated;

import io.github.linkfgfgui.jeipatternizer.platform.Services;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/**
 * Loader- and mod-neutral view of a pattern terminal used by the encode pipeline.
 * <p>
 * AE2 and Refined Storage are optional, so the concrete implementations are selected by mod id
 * and never referenced unless the corresponding mod is loaded.
 */
public interface PatternTerminalApi {

	String MODE_CRAFTING = "CRAFTING";
	String MODE_PROCESSING = "PROCESSING";

	/** Triggers pattern encoding on the terminal. */
	void encode(boolean simulateClick);

	/** Menu slot index of the encoded-pattern output, or {@code -1} when unavailable. */
	int getEncodedPatternSlot();

	/** Whether the terminal has at least one blank pattern to consume. */
	boolean hasBlankPattern();

	/** Whether the encoded-pattern slot currently holds a freshly encoded pattern. */
	boolean hasEncodedPattern();

	/** Whether the encoded-pattern slot is empty (the previous pattern has been moved out). */
	boolean isEncodedSlotEmpty();

	/** Whether the terminal's recipe grid/output slots hold the transferred recipe (fill completed). */
	boolean hasEncodableInput();

	/** Current terminal mode name (e.g. {@link #MODE_CRAFTING}). */
	String getMode();

	/** Switches the terminal mode (client action, synced back by the server). */
	void setMode(String mode);

	/**
	 * Reads every pattern already stored in an open pattern-access terminal, adds their output ids
	 * to the shared memory, and returns the number of patterns seen.
	 */
	long importExistingPatterns(Level level);

	/** API for an open encoding terminal, or {@code null} if the menu is not a supported terminal. */
	static PatternTerminalApi forEncodingMenu(AbstractContainerMenu menu) {
		if (menu == null) {
			return null;
		}
		if (Services.PLATFORM.isModLoaded("ae2") && AppliedEnergistics2.isEncodingMenu(menu)) {
			return new AppliedEnergistics2(menu);
		}
		if (Services.PLATFORM.isModLoaded("refinedstorage") && RefinedStorage.isEncodingMenu(menu)) {
			return new RefinedStorage(menu);
		}
		return null;
	}

	static boolean isValidEncodingScreen(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (Services.PLATFORM.isModLoaded("ae2") && AppliedEnergistics2.isEncodingScreen(screen)) {
			return true;
		}
		return Services.PLATFORM.isModLoaded("refinedstorage") && RefinedStorage.isEncodingScreen(screen);
	}

	static boolean isValidAccessScreen(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (Services.PLATFORM.isModLoaded("ae2") && AppliedEnergistics2.isAccessScreen(screen)) {
			return true;
		}
		return Services.PLATFORM.isModLoaded("refinedstorage") && RefinedStorage.isAccessScreen(screen);
	}

	static PatternTerminalApi forAccessScreen(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return null;
		}
		if (Services.PLATFORM.isModLoaded("ae2") && AppliedEnergistics2.isAccessScreen(screen)) {
			return new AppliedEnergistics2(containerScreen);
		}
		if (Services.PLATFORM.isModLoaded("refinedstorage") && RefinedStorage.isAccessScreen(screen)) {
			return new RefinedStorage(containerScreen);
		}
		return null;
	}
}
