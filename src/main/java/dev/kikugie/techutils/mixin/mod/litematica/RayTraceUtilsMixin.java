package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.techutils.config.LitematicConfigs;
import fi.dy.masa.litematica.util.RayTraceUtils;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RayTraceUtils.class)
public class RayTraceUtilsMixin {
	@ModifyExpressionValue(method = {"traceFirstStep", "traceLoopSteps"}, at = @At(
		value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
	))
	private static VoxelShape useFullCube(VoxelShape original) {
		return LitematicConfigs.EASY_PLACE_FULL_BLOCKS.getBooleanValue() && !original.isEmpty() ? Shapes.block() : original;
	}
}
