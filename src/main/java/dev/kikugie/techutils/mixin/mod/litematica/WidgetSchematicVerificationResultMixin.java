package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.kikugie.techutils.feature.containerscan.LinkedStorageEntry;
import dev.kikugie.techutils.feature.containerscan.verifier.BlockMismatchExtension;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;

import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.delayRenderingHoveredStack;
import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.hoveredStackToRender;
import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.infoOverlayInstance;

@Mixin(value = WidgetSchematicVerificationResult.class, remap = false)
public abstract class WidgetSchematicVerificationResultMixin<InventoryBE extends BlockEntity & Container> extends WidgetListEntrySortable<GuiSchematicVerifier.BlockMismatchEntry> {
	public WidgetSchematicVerificationResultMixin(int x, int y, int width, int height, @Nullable GuiSchematicVerifier.BlockMismatchEntry entry, int listIndex) {
		super(x, y, width, height, entry, listIndex);
	}

	@Shadow @Final private GuiSchematicVerifier.BlockMismatchEntry mismatchEntry;

	@Shadow protected abstract boolean shouldRenderAsSelected();

	@Unique
	private dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay infoOverlay;

	@WrapWithCondition(method = "postRenderHovered", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/widgets/WidgetSchematicVerificationResult$BlockMismatchInfo;render(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/Minecraft;)V", remap = true))
	private boolean renderInventoryOverlayIfNecessary(WidgetSchematicVerificationResult.BlockMismatchInfo instance, GuiGraphics graphics, int x, int y, Minecraft mc, GuiGraphics unused, int mouseX, int mouseY, boolean selected) {
		//noinspection unchecked
		var inventories = mismatchEntry.blockMismatch == null ? null : ((BlockMismatchExtension<InventoryBE>) mismatchEntry.blockMismatch).getInventories$techutils();
		if (inventories == null) {
			return true;
		}

		if (shouldRenderAsSelected()) {
			if (!selected) {
				return false;
			}
		} else {
			mouseX = 0;
			mouseY = 0;
		}

		InventoryBE left = inventories.getLeft();
		InventoryBE right = inventories.getRight();
		if (infoOverlay == null) {
			infoOverlay = new dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay(new LinkedStorageEntry(BlockPos.ZERO, right, left));
		}

		delayRenderingHoveredStack = true;

		renderInventoryOverlay(BlockInfoAlignment.CENTER, LeftRight.LEFT, 0, mc, graphics, left, mouseX, mouseY);

		infoOverlayInstance = infoOverlay;
		renderInventoryOverlay(BlockInfoAlignment.CENTER, LeftRight.RIGHT, 0, mc, graphics, right, mouseX, mouseY);
		infoOverlayInstance = null;

		delayRenderingHoveredStack = false;

		if (hoveredStackToRender != null) {
			InventoryOverlay.renderStackToolTipStyled(graphics, mouseX, mouseY, hoveredStackToRender, mc);
			hoveredStackToRender = null;
		}

		return false;
	}

	/**
	 * Basically a clone of {@link RenderUtils#renderInventoryOverlay(GuiGraphics, BlockInfoAlignment, LeftRight, int, Level, BlockPos, Minecraft)}
	 */
	@Unique
	private int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
									   Minecraft mc, GuiGraphics drawContext, InventoryBE inventoryBE,
									   double mouseX, double mouseY)
	{
		var nbt = inventoryBE.saveWithFullMetadata(mc.level.registryAccess());
		InventoryOverlay.Context ctx = new InventoryOverlay.Context(InventoryOverlay.getBestInventoryType(inventoryBE, nbt), inventoryBE, inventoryBE, null, nbt, null);

		if (ctx.inv() != null)
		{
			final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(ctx.type(), ctx.inv().getContainerSize());

//            Litematica.LOGGER.error("render(): type [{}], inv [{}], be [{}], nbt [{}]", ctx.type().name(), ctx.inv().size(), ctx.be() != null, ctx.nbt() != null ? ctx.nbt().getString("id") : new NbtCompound());

			// Try to draw Locked Slots on Crafter Grid
			if (ctx.type() == InventoryOverlay.InventoryRenderType.CRAFTER)
			{
				Set<Integer> disabledSlots = new HashSet<>();

				if (ctx.nbt() != null && !ctx.nbt().isEmpty())
				{
					disabledSlots = NbtBlockUtils.getDisabledSlotsFromNbt(ctx.nbt());
				}
				else if (ctx.be() instanceof CrafterBlockEntity cbe)
				{
					disabledSlots = BlockUtils.getDisabledSlots(cbe);
				}

				return renderInventoryOverlay(drawContext, side, offY, ctx.inv(), ctx.type(), props, disabledSlots, mc, align, mouseX, mouseY);
			}
			else
			{
				return renderInventoryOverlay(drawContext, side, offY, ctx.inv(), ctx.type(), props, Set.of(), mc, align, mouseX, mouseY);
			}
		}

		return 0;
	}

	/**
	 * Basically a clone of {@link RenderUtils#renderInventoryOverlay(GuiGraphics, BlockInfoAlignment, LeftRight, int, Container, InventoryOverlay.InventoryRenderType, InventoryOverlay.InventoryProperties, Set, Minecraft)}
	 */
	@Unique
	private static int renderInventoryOverlay(GuiGraphics drawContext, LeftRight side, int offY, Container inv, InventoryOverlay.InventoryRenderType type, InventoryOverlay.InventoryProperties props, Set<Integer> disabledSlots, Minecraft mc, BlockInfoAlignment align,
											  double mouseX, double mouseY)
	{
		int xInv = 0;
		int yInv = 0;
		int compatShift = OverlayRenderer.calculateCompatYShift();

		switch (align)
		{
			case CENTER:
				xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
				yInv = GuiUtils.getScaledWindowHeight() / 2 - props.height - offY;
				break;
			case TOP_CENTER:
				xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = offY + compatShift;
				break;
		}

		if      (side == LeftRight.LEFT)  { xInv -= (props.width / 2 + 4); }
		else if (side == LeftRight.RIGHT) { xInv += (props.width / 2 + 4); }

//		fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

		InventoryOverlay.renderInventoryBackground(drawContext, type, xInv, yInv, props.slotsPerRow, props.totalSlots, mc);
		InventoryOverlay.renderInventoryStacks(drawContext, type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, inv.getContainerSize(), disabledSlots, mc, mouseX, mouseY);

        return props.height + compatShift;
	}
}
