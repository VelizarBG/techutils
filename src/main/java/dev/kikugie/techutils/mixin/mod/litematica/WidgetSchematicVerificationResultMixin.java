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
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.render.InventoryOverlayType;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.game.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
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

	@WrapWithCondition(method = "postRenderHovered", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/widgets/WidgetSchematicVerificationResult$BlockMismatchInfo;render(Lfi/dy/masa/malilib/render/GuiContext;II)V", remap = true))
	private boolean renderInventoryOverlayIfNecessary(WidgetSchematicVerificationResult.BlockMismatchInfo instance, GuiContext ctx, int x, int y, GuiContext unused, int mouseX, int mouseY, boolean selected) {
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

		renderInventoryOverlay(ctx, BlockInfoAlignment.CENTER, LeftRight.LEFT, 0, left, mouseX, mouseY);

		infoOverlayInstance = infoOverlay;
		renderInventoryOverlay(ctx, BlockInfoAlignment.CENTER, LeftRight.RIGHT, 0, right, mouseX, mouseY);
		infoOverlayInstance = null;

		delayRenderingHoveredStack = false;

		if (hoveredStackToRender != null) {
			InventoryOverlay.renderStackToolTipStyled(ctx, mouseX, mouseY, hoveredStackToRender);
			hoveredStackToRender = null;
		}

		return false;
	}

	/**
	 * Basically a clone of {@link RenderUtils#renderInventoryOverlay(GuiContext, BlockInfoAlignment, LeftRight, int, Level, BlockPos)}
	 */
	@Unique
	private int renderInventoryOverlay(GuiContext guiCtx, BlockInfoAlignment align, LeftRight side,
									   int offY, InventoryBE inventoryBE,
									   double mouseX, double mouseY)
	{
		var nbt = DataConverterNbt.fromVanillaCompound(inventoryBE.saveWithFullMetadata(mc.level.registryAccess()));
		InventoryOverlayContext ctx = new InventoryOverlayContext(InventoryOverlay.getBestInventoryType(inventoryBE, nbt), inventoryBE, inventoryBE, null, nbt, null);

		if (ctx.inv() != null)
		{
			final InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(ctx.type(), ctx.inv().getContainerSize());

//            Litematica.LOGGER.error("render(): type [{}], inv [{}], be [{}], nbt [{}]", ctx.type().name(), ctx.inv().size(), ctx.be() != null, ctx.nbt() != null ? ctx.nbt().getString("id") : new NbtCompound());

			// Try to draw Locked Slots on Crafter Grid
			if (ctx.type() == InventoryOverlayType.CRAFTER)
			{
				Set<Integer> disabledSlots = new HashSet<>();

				if (ctx.data() != null && !ctx.data().isEmpty())
				{
					disabledSlots = DataBlockUtils.getDisabledSlots(ctx.data());
				}
				else if (ctx.be() instanceof CrafterBlockEntity cbe)
				{
					disabledSlots = BlockUtils.getDisabledSlots(cbe);
				}

				return renderInventoryOverlay(guiCtx, align, side, offY, ctx.inv(), ctx.type(), props, disabledSlots, mouseX, mouseY);
			}
			else
			{
				return renderInventoryOverlay(guiCtx, align, side, offY, ctx.inv(), ctx.type(), props, Set.of(), mouseX, mouseY);
			}
		}

		return 0;
	}

	/**
	 * Basically a clone of {@link RenderUtils#renderInventoryOverlay(GuiContext, BlockInfoAlignment, LeftRight, int, Container, InventoryOverlayType, InventoryProperties, Set)}
	 */
	@Unique
	private static int renderInventoryOverlay(GuiContext ctx, BlockInfoAlignment align, LeftRight side, int offY, Container inv, InventoryOverlayType type, InventoryProperties props, Set<Integer> disabledSlots,
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

		InventoryOverlay.renderInventoryBackground(ctx, type, xInv, yInv, props.slotsPerRow, props.totalSlots);
		InventoryOverlay.renderInventoryStacks(ctx, type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, inv.getContainerSize(), disabledSlots, mouseX, mouseY);

        return props.height + compatShift;
	}
}
