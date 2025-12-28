package dev.kikugie.techutils.mixin.mod.litematica;

import fi.dy.masa.litematica.util.ItemUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.IdentityHashMap;

@Mixin(value = ItemUtils.class, remap = false)
public interface ItemUtilsAccessor {
	@Accessor("ITEMS_FOR_STATES")
	static IdentityHashMap<BlockState, ItemStack> getItemsForStates() {
		throw new UnsupportedOperationException();
	}
}
