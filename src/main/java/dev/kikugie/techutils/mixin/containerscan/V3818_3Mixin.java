package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import dev.kikugie.techutils.util.ItemPredicateUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.SequencedMap;
import java.util.function.Supplier;

@Mixin(V3818_3.class)
public class V3818_3Mixin {
	@ModifyReturnValue(method = "components", at = @At("RETURN"))
	private static SequencedMap<String, Supplier<TypeTemplate>> addSupportForPredicatePlaceholders(
			SequencedMap<String, Supplier<TypeTemplate>> original,
			Schema schema
	) {
		original.put(
//				BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.CUSTOM_DATA).toString(),
				"minecraft:custom_data",
				() -> DSL.optionalFields(Pair.of(
						ItemPredicateUtils.ROOT_PREDICATE_ID,
						DSL.optionalFields(ItemPredicateUtils.PLACEHOLDER_ID, References.ITEM_STACK.in(schema))
				))
		);
		return original;
	}
}
