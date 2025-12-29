package dev.kikugie.techutils.feature.containerscan.verifier;

import dev.kikugie.techutils.util.ItemPredicateUtils;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.glfw.GLFW;

@NullMarked
public class ItemPredicateEntryScreen extends Screen {
	private static final Component TITLE = Component.translatable("item_predicate_entry_screen.title");
	private static final Component INPUT_TEXT = Component.translatable("item_predicate_entry_screen.input");
	private final LocalPlayer player;
	private ItemStack placeholder;
	private String initInput;
	protected EditBox consoleCommandTextField;
	protected Button doneButton;
	protected Button cancelButton;

	public ItemPredicateEntryScreen(LocalPlayer player) {
		super(GameNarrator.NO_TITLE);
		this.player = player;
	}

	public ItemPredicateEntryScreen(LocalPlayer player, ItemStack placeholder) {
		this(player);
		this.placeholder = placeholder;
	}

	public ItemPredicateEntryScreen(LocalPlayer player, String input) {
		this(player);
		this.initInput = input;
	}

	public ItemPredicateEntryScreen(LocalPlayer player, String input, ItemStack placeholder) {
		this(player, input);
		this.placeholder = placeholder;
	}

	protected void commitAndClose() {
		var stack = ItemPredicateUtils.createPredicateStack(consoleCommandTextField.getValue(), placeholder);


		int selectedSlot = player.getInventory().getSelectedSlot();
		player.getInventory().setItem(selectedSlot, stack);
		this.minecraft.gameMode.handleCreativeModeItemAdd(stack, 36 + selectedSlot);
		this.player.inventoryMenu.broadcastChanges();

		this.minecraft.setScreen(null);
	}

	@Override
	protected void init() {
		this.doneButton = this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> this.commitAndClose()).bounds(this.width / 2 - 4 - 150, this.height / 16 + 120 + 12, 150, 20).build()
		);
		this.cancelButton = this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(this.width / 2 + 4, this.height / 16 + 120 + 12, 150, 20).build()
		);
		this.consoleCommandTextField = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, Component.translatable("advMode.command"));
		this.consoleCommandTextField.setMaxLength(100000);
		this.consoleCommandTextField.setValue(initInput);
		this.addWidget(this.consoleCommandTextField);
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.consoleCommandTextField);
	}

	@Override
	public void resize(int width, int height) {
		String string = this.consoleCommandTextField.getValue();
		this.init(width, height);
		this.consoleCommandTextField.setValue(string);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (super.keyPressed(input)) {
			return true;
		} else if (input.key() != GLFW.GLFW_KEY_ENTER && input.key() != GLFW.GLFW_KEY_KP_ENTER) {
			return false;
		} else {
			this.commitAndClose();
			return true;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, TITLE, this.width / 2, 20, 16777215);
		graphics.drawString(this.font, INPUT_TEXT, this.width / 2 - 150 + 1, 40, 10526880);
		this.consoleCommandTextField.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderTransparentBackground(graphics);
	}
}
