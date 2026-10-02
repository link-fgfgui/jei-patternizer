package io.github.linkfgfgui.jeipatternizer.client;

import com.jeicrafter.api.RecipeStep;
import io.github.linkfgfgui.jeipatternizer.Constants;
import io.github.linkfgfgui.jeipatternizer.config.JeiPatternizerConfig;
import io.github.linkfgfgui.jeipatternizer.integrated.PatternTerminalApi;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Main-thread state machine that encodes a collected list of recipes one at a time:
 * fill the terminal via JEI transfer, encode, then shift-click the finished pattern into the
 * player's inventory. Driven from {@code Minecraft.tick}.
 */
public final class PatternizerSession {

	private static final int FILL_TIMEOUT_TICKS = 60;
	private static final int MODE_TIMEOUT_TICKS = 20;
	private static final int ENCODE_TIMEOUT_TICKS = 60;
	private static final int MOVE_TIMEOUT_TICKS = 60;

	@Nullable
	private static Session active;

	private PatternizerSession() {
	}

	public static boolean isOperating() {
		return active != null;
	}

	/** Called from the key handler while a supported terminal menu is open. */
	public static void requestStart(Screen screen) {
		if (active != null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null) {
			return;
		}
		AbstractContainerMenu menu = player.containerMenu;
		PatternTerminalApi api = PatternTerminalApi.forEncodingMenu(menu);
		if (api == null) {
			// Not an encoding terminal; the key is shared with the rest of the game.
			return;
		}
		IJeiRuntime runtime = JeiRuntimeHolder.get();
		if (runtime == null) {
			message(player, "chat.jeipatternizer.no_jei");
			return;
		}
		JeiPatternizerConfig.load();
		List<RecipeStep> steps = RecipeTreeCollector.collect(runtime, screen, player, menu);
		if (steps.isEmpty()) {
			message(player, "chat.jeipatternizer.nothing");
			return;
		}
		active = new Session(api, menu, player, minecraft.gameMode, runtime, steps);
		Constants.LOG.info("Encode session started with {} recipe(s)", steps.size());
	}

	/** Called at the tail of {@code Minecraft.tick}. */
	public static void tick() {
		Session session = active;
		if (session == null) {
			return;
		}
		try {
			session.tick();
		} catch (RuntimeException exception) {
			Constants.LOG.error("Encode session threw; aborting", exception);
			session.abort("chat.jeipatternizer.aborted");
		}
	}

	private static void message(LocalPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), false);
	}

	private enum Phase {
		FILL, WAIT_MODE, WAIT_FILL, ENCODE, WAIT_ENCODED, MOVE, WAIT_MOVE
	}

	private static final class Session {

		private final PatternTerminalApi api;
		private final AbstractContainerMenu menu;
		private final LocalPlayer player;
		private final MultiPlayerGameMode gameMode;
		private final IJeiRuntime runtime;
		private final List<RecipeStep> steps;
		private int index;
		private int encodedCount;
		private Phase phase = Phase.FILL;
		private int waitTicks;

		private Session(
			PatternTerminalApi api,
			AbstractContainerMenu menu,
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			IJeiRuntime runtime,
			List<RecipeStep> steps
		) {
			this.api = api;
			this.menu = menu;
			this.player = player;
			this.gameMode = gameMode;
			this.runtime = runtime;
			this.steps = steps;
		}

		private void tick() {
			LocalPlayer current = Minecraft.getInstance().player;
			if (current == null || current.containerMenu != menu) {
				abort("chat.jeipatternizer.terminal_closed");
				return;
			}
			if (index >= steps.size()) {
				finish();
				return;
			}
			RecipeStep step = steps.get(index);
			switch (phase) {
				case FILL -> fill(step);
				case WAIT_MODE -> waitMode(step);
				case WAIT_FILL -> waitFill(step);
				case ENCODE -> encodeStep();
				case WAIT_ENCODED -> waitEncoded(step);
				case MOVE -> move();
				case WAIT_MOVE -> waitMove(step);
			}
		}

		private void fill(RecipeStep step) {
			if (!api.hasBlankPattern()) {
				abort("chat.jeipatternizer.no_blank");
				return;
			}
			String desired = desiredMode(step);
			if (!desired.equals(api.getMode())) {
				api.setMode(desired);
				phase = Phase.WAIT_MODE;
				waitTicks = 0;
				return;
			}
			transfer(step);
		}

		private void waitMode(RecipeStep step) {
			if (desiredMode(step).equals(api.getMode())) {
				transfer(step);
				return;
			}
			if (waitTicks++ > MODE_TIMEOUT_TICKS) {
				abort("chat.jeipatternizer.mode_failed");
			}
		}

		private void transfer(RecipeStep step) {
			boolean ok;
			try {
				ok = RecipeTransferUtil.transferRecipe(
					runtime.getRecipeTransferManager(),
					menu,
					step.recipeLayout(),
					player,
					false
				);
			} catch (RuntimeException exception) {
				Constants.LOG.error("Recipe transfer threw", exception);
				ok = false;
			}
			if (!ok) {
				// One unsupported category should not kill the whole batch; skip and keep going.
				skip(step, "chat.jeipatternizer.transfer_failed");
				return;
			}
			playClickSound();
			phase = Phase.WAIT_FILL;
			waitTicks = 0;
		}

		private void waitFill(RecipeStep step) {
			if (api.hasEncodableInput()) {
				phase = Phase.ENCODE;
				waitTicks = 0;
				return;
			}
			if (waitTicks++ > FILL_TIMEOUT_TICKS) {
				skip(step, "chat.jeipatternizer.transfer_failed");
			}
		}

		private void encodeStep() {
			api.encode(JeiPatternizerConfig.simulateClick());
			phase = Phase.WAIT_ENCODED;
			waitTicks = 0;
		}

		private void waitEncoded(RecipeStep step) {
			if (api.hasEncodedPattern()) {
				phase = Phase.MOVE;
				waitTicks = 0;
				return;
			}
			if (waitTicks++ > ENCODE_TIMEOUT_TICKS) {
				if (!api.hasBlankPattern()) {
					abort("chat.jeipatternizer.no_blank");
				} else {
					skip(step, "chat.jeipatternizer.encode_failed");
				}
			}
		}

		private void move() {
			int slot = api.getEncodedPatternSlot();
			if (slot < 0) {
				abort("chat.jeipatternizer.encode_failed");
				return;
			}
			gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, player);
			phase = Phase.WAIT_MOVE;
			waitTicks = 0;
		}

		private void waitMove(RecipeStep step) {
			if (api.isEncodedSlotEmpty()) {
				EncodedItems.add(step.output());
				encodedCount++;
				advance();
				return;
			}
			if (waitTicks++ > MOVE_TIMEOUT_TICKS) {
				// The finished pattern is stuck in the terminal (usually a full inventory);
				// continuing would only pile up more stuck patterns.
				abort("chat.jeipatternizer.move_failed");
			}
		}

		private void skip(RecipeStep step, String reasonKey) {
			Constants.LOG.warn("Skipping recipe {}: {}", step.output().getHoverName().getString(), reasonKey);
			message(player, "chat.jeipatternizer.skipped", step.output().getHoverName());
			advance();
		}

		private void advance() {
			index++;
			waitTicks = 0;
			if (index >= steps.size()) {
				finish();
			} else {
				phase = Phase.FILL;
			}
		}

		private void playClickSound() {
			if (JeiPatternizerConfig.playSound()) {
				Minecraft.getInstance().getSoundManager()
					.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			}
		}

		private static String desiredMode(RecipeStep step) {
			return RecipeTypes.CRAFTING.equals(step.recipeCategory().getRecipeType())
				? PatternTerminalApi.MODE_CRAFTING
				: PatternTerminalApi.MODE_PROCESSING;
		}

		private void finish() {
			Constants.LOG.info("Encode session finished: {} pattern(s) encoded", encodedCount);
			message(player, "chat.jeipatternizer.done", encodedCount);
			active = null;
		}

		private void abort(String reasonKey) {
			Constants.LOG.warn("Encode session aborted at recipe {}/{}: {}", index + 1, steps.size(), reasonKey);
			message(player, reasonKey);
			active = null;
		}
	}
}
