package io.github.linkfgfgui.jeipatternizer.integrated;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.me.patternaccess.PatternContainerRecord;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import appeng.util.inv.AppEngInternalInventory;
import io.github.linkfgfgui.jeipatternizer.client.EncodedItems;
import io.github.linkfgfgui.jeipatternizer.mixin.ae2.AEBaseScreenAccessor;
import io.github.linkfgfgui.jeipatternizer.mixin.ae2.PatternAccessTermScreenAccessor;
import io.github.linkfgfgui.jeipatternizer.mixin.ae2.WidgetContainerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Applied Energistics 2 integration, compiled against AE2's own classes (a {@code modCompileOnly}
 * dependency, so this class is only ever loaded while AE2 is present).
 */
final class AppliedEnergistics2 implements PatternTerminalApi {

	private static final String ENCODE_BUTTON = "encodePattern";
	private static final List<SlotSemantic> INPUT_SEMANTICS = List.of(
		SlotSemantics.CRAFTING_RESULT,
		SlotSemantics.PROCESSING_INPUTS,
		SlotSemantics.PROCESSING_OUTPUTS
	);

	@Nullable
	private final PatternEncodingTermMenu encodingMenu;
	@Nullable
	private final PatternAccessTermScreen<?> accessScreen;

	AppliedEnergistics2(AbstractContainerMenu menu) {
		this.encodingMenu = menu instanceof PatternEncodingTermMenu patternMenu ? patternMenu : null;
		this.accessScreen = null;
	}

	AppliedEnergistics2(AbstractContainerScreen<?> screen) {
		this.encodingMenu = screen.getMenu() instanceof PatternEncodingTermMenu patternMenu ? patternMenu : null;
		this.accessScreen = screen instanceof PatternAccessTermScreen<?> patternScreen ? patternScreen : null;
	}

	static boolean isEncodingMenu(AbstractContainerMenu menu) {
		return menu instanceof PatternEncodingTermMenu;
	}

	static boolean isEncodingScreen(Screen screen) {
		return screen instanceof PatternEncodingTermScreen<?>;
	}

	static boolean isAccessScreen(Screen screen) {
		return screen instanceof PatternAccessTermScreen<?>;
	}

	@Override
	public void encode(boolean simulateClick) {
		if (simulateClick && clickEncodeButton()) {
			return;
		}
		if (encodingMenu != null) {
			encodingMenu.encode();
		}
	}

	private static boolean clickEncodeButton() {
		Screen current = Minecraft.getInstance().screen;
		if (!(current instanceof AEBaseScreen<?> aeScreen)) {
			return false;
		}
		WidgetContainer container = ((AEBaseScreenAccessor) aeScreen).jeipatternizer$getWidgets();
		AbstractWidget widget = ((WidgetContainerAccessor) container).jeipatternizer$getWidgets().get(ENCODE_BUTTON);
		if (widget instanceof Button button) {
			button.onPress();
			return true;
		}
		return false;
	}

	@Override
	public int getEncodedPatternSlot() {
		List<Slot> slots = slots(SlotSemantics.ENCODED_PATTERN);
		return slots.isEmpty() ? -1 : slots.get(0).index;
	}

	@Override
	public boolean hasBlankPattern() {
		for (Slot slot : slots(SlotSemantics.BLANK_PATTERN)) {
			if (!slot.getItem().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasEncodedPattern() {
		List<Slot> slots = slots(SlotSemantics.ENCODED_PATTERN);
		return !slots.isEmpty() && !slots.get(0).getItem().isEmpty();
	}

	@Override
	public boolean isEncodedSlotEmpty() {
		for (Slot slot : slots(SlotSemantics.ENCODED_PATTERN)) {
			if (!slot.getItem().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean hasEncodableInput() {
		for (SlotSemantic semantic : INPUT_SEMANTICS) {
			for (Slot slot : slots(semantic)) {
				if (!slot.getItem().isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getMode() {
		return encodingMenu == null ? MODE_CRAFTING : encodingMenu.getMode().name();
	}

	@Override
	public void setMode(String mode) {
		if (encodingMenu != null) {
			encodingMenu.setMode(EncodingMode.valueOf(mode));
		}
	}

	private List<Slot> slots(SlotSemantic semantic) {
		return encodingMenu == null ? List.of() : encodingMenu.getSlots(semantic);
	}

	@Override
	public long importExistingPatterns(Level level) {
		if (accessScreen == null) {
			return 0;
		}
		long count = 0;
		for (PatternContainerRecord record : ((PatternAccessTermScreenAccessor) accessScreen).jeipatternizer$getById().values()) {
			AppEngInternalInventory inventory = record.getInventory();
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStackInSlot(i);
				if (stack.isEmpty()) {
					continue;
				}
				count++;
				IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
				if (details == null) {
					continue;
				}
				for (GenericStack output : details.getOutputs()) {
					EncodedItems.addId(output.what().getId().toString());
				}
			}
		}
		return count;
	}
}
