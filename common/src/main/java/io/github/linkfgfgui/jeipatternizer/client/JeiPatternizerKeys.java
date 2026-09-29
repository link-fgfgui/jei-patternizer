package io.github.linkfgfgui.jeipatternizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.linkfgfgui.jeipatternizer.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Key bindings. Default is N, matching EMI-Patternizer.
 */
public final class JeiPatternizerKeys {

	public static final KeyMapping PATTERNIZE = new KeyMapping(
		"key.jeipatternizer.patternize",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_N,
		"key.categories.jeipatternizer"
	);

	private JeiPatternizerKeys() {
	}

	public static boolean isPatternizeKey(int keyCode, int scanCode) {
		InputConstants.Key bound = ((KeyMappingAccessor) PATTERNIZE).jeipatternizer$getKey();
		return bound.equals(InputConstants.getKey(keyCode, scanCode));
	}
}
