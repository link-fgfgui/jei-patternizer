package io.github.linkfgfgui.jeipatternizer.client;

import com.jeicrafter.api.RecipeGraphContext;
import com.jeicrafter.api.RecipeRequest;
import com.jeicrafter.api.RecipeStep;
import com.jeicrafter.api.SimpleRecipeStep;
import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.mixin.jei.RecipesGuiAccessor;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.recipes.IRecipeGuiLogic;
import mezz.jei.gui.recipes.RecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the list of recipes to encode from the player's JEI bookmarks or current recipe page.
 * <p>
 * Roots come from either the currently open JEI recipe page or, as a fallback, all bookmarked
 * recipes in JEI. Dependency expansion strictly resolves from JEI recipe bookmarks (learning from
 * JEI Crafter) rather than guessing recipes globally. The tree is flattened, deduplicated by recipe
 * identity, filtered against {@link EncodedItems}, and clustered by recipe category.
 */
public final class RecipeTreeCollector {

	/** Guard against pathological recipe chains; identity dedup already breaks cycles. */
	private static final int MAX_STEPS = 1024;

	private RecipeTreeCollector() {
	}

	public static List<RecipeStep> collect(IJeiRuntime runtime, Screen screen, LocalPlayer player, AbstractContainerMenu menu) {
		List<RecipeStep> roots = resolveRoots(runtime, screen, menu);
		if (roots.isEmpty()) {
			return List.of();
		}
		JeiRecipeGraph graph = new JeiRecipeGraph(runtime);
		RecipeGraphContext context = new RecipeGraphContext(runtime, player, menu);

		Set<Object> seen = new HashSet<>();
		List<RecipeStep> ordered = new ArrayList<>();
		Deque<RecipeStep> stack = new ArrayDeque<>();
		for (int i = roots.size() - 1; i >= 0; i--) {
			stack.push(roots.get(i));
		}
		while (!stack.isEmpty() && ordered.size() < MAX_STEPS) {
			RecipeStep step = stack.pop();
			if (!seen.add(step.identity())) {
				continue;
			}
			if (!EncodedItems.contains(step.output())) {
				ordered.add(step);
			}
			// Push children in reverse so they are visited in slot order.
			List<ItemStack> inputs = inputs(step);
			for (int i = inputs.size() - 1; i >= 0; i--) {
				ItemStack input = inputs.get(i);
				if (input.isEmpty()) {
					continue;
				}
				Optional<RecipeStep> child = graph.resolve(RecipeRequest.forDependency(input, step), context);
				child.ifPresent(c -> {
					if (!seen.contains(c.identity())) {
						stack.push(c);
					}
				});
			}
		}
		// Cluster by category so the encode pipeline groups the same terminal mode together.
		ordered.sort(Comparator.comparing(step -> {
			RecipeType<?> type = step.recipeCategory().getRecipeType();
			return type == null ? "" : type.getUid().toString();
		}));
		Constants.LOG.debug("Collected {} recipe step(s) from {} root(s)", ordered.size(), roots.size());
		return ordered;
	}

	private static List<RecipeStep> resolveRoots(IJeiRuntime runtime, Screen screen, AbstractContainerMenu menu) {
		List<RecipeStep> roots = new ArrayList<>();
		// 1. If currently viewing a JEI recipe GUI, take the visible recipe(s) as root
		if (screen instanceof RecipesGui recipesGui) {
			try {
				IRecipeGuiLogic logic = ((RecipesGuiAccessor) recipesGui).jeipatternizer$getLogic();
				int availableHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
				for (RecipeLayoutWithButtons<?> withButtons : logic.getVisibleRecipeLayoutsWithButtons(availableHeight, 4, menu)) {
					IRecipeLayoutDrawable<?> layout = withButtons.recipeLayout();
					ItemStack output = JeiRecipeGraph.firstOutput(layout);
					if (output.isEmpty()) {
						continue;
					}
					roots.add(new SimpleRecipeStep(layout.getRecipeCategory(), layout.getRecipe(), layout, output));
				}
			} catch (RuntimeException exception) {
				Constants.LOG.warn("Failed to read the current JEI recipe page", exception);
			}
		}
		// 2. Fallback: if not viewing a recipe page (e.g. on the terminal screen), use all recipe bookmarks as roots
		if (roots.isEmpty()) {
			for (RecipeBookmark<?, ?> bookmark : JeiRecipeGraph.getAllRecipeBookmarks(runtime)) {
				ItemStack output = JeiRecipeGraph.outputOf(bookmark, runtime);
				JeiRecipeGraph.createStep(bookmark.getRecipeCategory(), bookmark.getRecipe(), output, runtime)
					.ifPresent(roots::add);
			}
		}
		return roots;
	}

	private static List<ItemStack> inputs(RecipeStep step) {
		List<ItemStack> inputs = new ArrayList<>();
		for (IRecipeSlotView slot : step.recipeLayout().getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT)) {
			ItemStack input = JeiRecipeGraph.firstInput(slot);
			if (!input.isEmpty()) {
				inputs.add(input);
			}
		}
		return inputs;
	}
}
