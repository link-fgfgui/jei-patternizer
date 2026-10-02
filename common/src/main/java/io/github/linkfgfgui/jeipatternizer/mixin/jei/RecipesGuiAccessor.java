package io.github.linkfgfgui.jeipatternizer.mixin.jei;

import mezz.jei.gui.recipes.IRecipeGuiLogic;
import mezz.jei.gui.recipes.RecipesGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes JEI's internal {@link IRecipeGuiLogic} so the collector can read the recipe page the
 * player is currently looking at. Mirrors jeicrafter's own accessor.
 */
@Mixin(value = RecipesGui.class, remap = false)
public interface RecipesGuiAccessor {

	@Accessor(value = "logic", remap = false)
	IRecipeGuiLogic jeipatternizer$getLogic();
}
