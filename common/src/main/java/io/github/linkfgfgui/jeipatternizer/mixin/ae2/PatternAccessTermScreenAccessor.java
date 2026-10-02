package io.github.linkfgfgui.jeipatternizer.mixin.ae2;

import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.me.patternaccess.PatternContainerRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

/**
 * Exposes the pattern-access terminal's client-side store of stored patterns for reverse sync.
 */
@Mixin(value = PatternAccessTermScreen.class, remap = false)
public interface PatternAccessTermScreenAccessor {

	@Accessor(value = "byId", remap = false)
	HashMap<Long, PatternContainerRecord> jeipatternizer$getById();
}
