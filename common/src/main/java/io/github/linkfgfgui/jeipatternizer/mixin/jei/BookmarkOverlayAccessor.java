package io.github.linkfgfgui.jeipatternizer.mixin.jei;

import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes JEI's internal {@link BookmarkList} so the collector can read bookmarked recipes.
 */
@Mixin(value = BookmarkOverlay.class, remap = false)
public interface BookmarkOverlayAccessor {

	@Accessor(value = "bookmarkList", remap = false)
	BookmarkList jeipatternizer$getBookmarkList();
}
