package io.github.linkfgfgui.jeipatternizer.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * In-memory set of already-encoded output item ids, used to skip recipes the player already
 * has patterns for. Populated as the encode session runs and re-synced when the player opens a
 * pattern access terminal.
 */
public final class EncodedItems {

	private static final Set<String> ITEMS = new HashSet<>();

	private EncodedItems() {
	}

	public static boolean contains(ItemStack stack) {
		return !stack.isEmpty() && ITEMS.contains(idOf(stack));
	}

	public static boolean containsAll(Collection<ItemStack> stacks) {
		for (ItemStack stack : stacks) {
			if (!contains(stack)) {
				return false;
			}
		}
		return true;
	}

	public static boolean add(ItemStack stack) {
		return !stack.isEmpty() && ITEMS.add(idOf(stack));
	}

	public static boolean addId(String id) {
		return id != null && !id.isEmpty() && ITEMS.add(id);
	}

	public static void clear() {
		ITEMS.clear();
	}

	public static int size() {
		return ITEMS.size();
	}

	public static String idOf(ItemStack stack) {
		return stack.getItemHolder().unwrapKey()
			.map(ResourceKey::location)
			.map(Object::toString)
			.orElse("");
	}
}
