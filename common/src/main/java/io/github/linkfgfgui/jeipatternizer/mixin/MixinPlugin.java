package io.github.linkfgfgui.jeipatternizer.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the AE2 accessors only when AE2 is present, so the mod has no hard dependency on it.
 * <p>
 * Uses {@code Class.forName} instead of a loader mod-list API so it works identically on Forge and
 * Fabric during mixin application.
 */
public class MixinPlugin implements IMixinConfigPlugin {

	private static final String AE2_MIXIN_PREFIX = "io.github.linkfgfgui.jeipatternizer.mixin.ae2";
	private static final String AE2_MARKER = "appeng.menu.me.items.PatternEncodingTermMenu";

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith(AE2_MIXIN_PREFIX)) {
			return isClassPresent(AE2_MARKER);
		}
		return true;
	}

	private static boolean isClassPresent(String className) {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class.forName(className, false, classLoader);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
