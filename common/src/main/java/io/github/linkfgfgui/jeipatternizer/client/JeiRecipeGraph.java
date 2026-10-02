package io.github.linkfgfgui.jeipatternizer.client;

import com.jeicrafter.api.RecipeGraph;
import com.jeicrafter.api.RecipeGraphContext;
import com.jeicrafter.api.RecipeRequest;
import com.jeicrafter.api.RecipeStep;
import com.jeicrafter.api.SimpleRecipeStep;
import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.mixin.jei.BookmarkOverlayAccessor;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link RecipeGraph} backed exclusively by JEI's recipe bookmarks list (following JEI Crafter).
 * <p>
 * Does not guess recipes across JEI globally. Dependencies are only resolved if the player has
 * bookmarked the exact recipe in JEI.
 */
final class JeiRecipeGraph implements RecipeGraph {

	private final IJeiRuntime runtime;

	JeiRecipeGraph(IJeiRuntime runtime) {
		this.runtime = runtime;
	}

	@Override
	public boolean supports(RecipeRequest request, RecipeGraphContext context) {
		return true;
	}

	@Override
	public Optional<RecipeStep> resolve(RecipeRequest request, RecipeGraphContext context) {
		IJeiRuntime jeiRuntime = context.jeiRuntime();
		try {
			if (request.recipe().isPresent() && request.recipeCategory().isPresent()) {
				Object recipe = request.recipe().get();
				IRecipeCategory<?> category = request.recipeCategory().get();
				if (recipe instanceof ITagInfoRecipe) {
					return resolveCraftableStep(request.target(), jeiRuntime);
				}
				return createStep(category, recipe, request.target(), jeiRuntime);
			}
			ItemStack target = request.target();
			if (target.isEmpty()) {
				return Optional.empty();
			}
			for (RecipeBookmark<?, ?> bookmark : findMatchingBookmarks(target, jeiRuntime)) {
				Optional<RecipeStep> step;
				if (bookmark.getRecipe() instanceof ITagInfoRecipe) {
					step = resolveCraftableStep(target, jeiRuntime);
				} else {
					step = createStep(
						bookmark.getRecipeCategory(),
						bookmark.getRecipe(),
						outputOf(bookmark, jeiRuntime),
						jeiRuntime
					);
				}
				if (step.isPresent()) {
					return step;
				}
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			Constants.LOG.warn("Failed to resolve bookmarked recipe for {}", request.target(), exception);
			return Optional.empty();
		}
	}

	public static ItemStack outputOf(RecipeBookmark<?, ?> bookmark, IJeiRuntime runtime) {
		Optional<ItemStack> bookmarkOutput = bookmark.getRecipeOutput().getIngredient(VanillaTypes.ITEM_STACK);
		if (bookmarkOutput.isPresent() && !bookmarkOutput.get().isEmpty()) {
			return bookmarkOutput.get().copy();
		}
		Optional<IRecipeLayoutDrawable<?>> layout = createLayout(bookmark.getRecipeCategory(), bookmark.getRecipe(), runtime);
		if (layout.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return firstOutput(layout.get());
	}

	public static List<RecipeBookmark<?, ?>> findMatchingBookmarks(ItemStack target, IJeiRuntime runtime) {
		if (target.isEmpty()) {
			return List.of();
		}
		List<RecipeBookmark<?, ?>> matches = new ArrayList<>();
		BookmarkList bookmarkList = getBookmarkList(runtime);
		if (bookmarkList == null) {
			return List.of();
		}
		IStackHelper stackHelper = runtime.getJeiHelpers().getStackHelper();
		for (IElement<?> element : bookmarkList.getElements()) {
			Optional<IBookmark> bookmark = element.getBookmark();
			if (bookmark.isPresent() && bookmark.get() instanceof RecipeBookmark<?, ?> recipeBookmark) {
				ItemStack output = outputOf(recipeBookmark, runtime);
				if (!output.isEmpty() && stackHelper.isEquivalent(output, target, UidContext.Ingredient)) {
					matches.add(recipeBookmark);
				}
			}
		}
		return matches;
	}

	public static List<RecipeBookmark<?, ?>> getAllRecipeBookmarks(IJeiRuntime runtime) {
		BookmarkList bookmarkList = getBookmarkList(runtime);
		if (bookmarkList == null) {
			return List.of();
		}
		List<RecipeBookmark<?, ?>> recipeBookmarks = new ArrayList<>();
		for (IElement<?> element : bookmarkList.getElements()) {
			Optional<IBookmark> bookmark = element.getBookmark();
			if (bookmark.isPresent() && bookmark.get() instanceof RecipeBookmark<?, ?> recipeBookmark) {
				recipeBookmarks.add(recipeBookmark);
			}
		}
		return recipeBookmarks;
	}

	public static BookmarkList getBookmarkList(IJeiRuntime runtime) {
		if (runtime == null) {
			return null;
		}
		IBookmarkOverlay overlay = runtime.getBookmarkOverlay();
		if (overlay instanceof BookmarkOverlayAccessor accessor) {
			return accessor.jeipatternizer$getBookmarkList();
		}
		return null;
	}

	private static Optional<RecipeStep> resolveCraftableStep(ItemStack item, IJeiRuntime runtime) {
		if (item.isEmpty()) {
			return Optional.empty();
		}
		IFocus<ItemStack> focus = runtime.getJeiHelpers().getFocusFactory()
			.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, item);
		IRecipeManager recipeManager = runtime.getRecipeManager();
		IRecipeCategory<CraftingRecipe> category = recipeManager.getRecipeCategory(RecipeTypes.CRAFTING);
		if (category == null) {
			return Optional.empty();
		}
		return recipeManager.createRecipeLookup(RecipeTypes.CRAFTING)
			.limitFocus(List.of(focus))
			.get()
			.map(recipe -> createStep(category, recipe, item, runtime).orElse(null))
			.filter(Objects::nonNull)
			.findFirst();
	}

	public static Optional<RecipeStep> createStep(IRecipeCategory<?> category, Object recipe, ItemStack outputHint, IJeiRuntime runtime) {
		Optional<IRecipeLayoutDrawable<?>> layout = createLayout(category, recipe, runtime);
		if (layout.isEmpty()) {
			return Optional.empty();
		}
		IRecipeLayoutDrawable<?> drawable = layout.get();
		ItemStack output = outputHint.isEmpty() ? firstOutput(drawable) : outputHint.copy();
		if (output.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new SimpleRecipeStep(category, recipe, drawable, output));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Optional<IRecipeLayoutDrawable<?>> createLayout(IRecipeCategory<?> category, Object recipe, IJeiRuntime runtime) {
		Optional<IRecipeLayoutDrawable<?>> layout = (Optional) runtime.getRecipeManager().createRecipeLayoutDrawable(
			(IRecipeCategory) category,
			recipe,
			runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()
		);
		return layout;
	}

	public static ItemStack firstOutput(IRecipeLayoutDrawable<?> layout) {
		return layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT).stream()
			.findFirst()
			.flatMap(IRecipeSlotView::getDisplayedItemStack)
			.map(ItemStack::copy)
			.orElse(ItemStack.EMPTY);
	}

	public static ItemStack firstInput(IRecipeSlotView slot) {
		return slot.getIngredients(VanillaTypes.ITEM_STACK)
			.filter(stack -> !stack.isEmpty())
			.findFirst()
			.orElse(ItemStack.EMPTY);
	}
}
