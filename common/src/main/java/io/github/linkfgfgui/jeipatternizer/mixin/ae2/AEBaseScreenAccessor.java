package io.github.linkfgfgui.jeipatternizer.mixin.ae2;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the (protected) widget container of an AE2 screen so {@code simulateClick} can press the
 * "encodePattern" button exactly like the player would.
 */
@Mixin(value = AEBaseScreen.class, remap = false)
public interface AEBaseScreenAccessor {

	@Accessor(value = "widgets", remap = false)
	WidgetContainer jeipatternizer$getWidgets();
}
