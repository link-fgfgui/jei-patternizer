package io.github.linkfgfgui.jeipatternizer.mixin.ae2;

import appeng.client.gui.WidgetContainer;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes AE2's named widget map so the encode button can be looked up by id.
 */
@Mixin(value = WidgetContainer.class, remap = false)
public interface WidgetContainerAccessor {

	@Accessor(value = "widgets", remap = false)
	Map<String, AbstractWidget> jeipatternizer$getWidgets();
}
