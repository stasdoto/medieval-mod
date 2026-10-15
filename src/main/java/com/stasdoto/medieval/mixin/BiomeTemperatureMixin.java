package com.stasdoto.medieval.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

/**
 * Minecraft cools the air by about one degree every eight blocks of height, which on a 1:1 Earth would bury every plateau
 * above ~600 m (Castile, Anatolia, Iran, Jerusalem) in snow. Real air cools ~6.5 °C per km: the drop is made six times
 * gentler. The snow of the real high mountains comes from the Earth generator's own snow line.
 */
@Mixin(Biome.class)
public abstract class BiomeTemperatureMixin {
	@Inject(method = "getHeightAdjustedTemperature(Lnet/minecraft/core/BlockPos;I)F", at = @At("RETURN"), cancellable = true, require = 0)
	private void medieval$gentleLapse(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> cir) {
		int above = pos.getY() - (seaLevel + 17);
		if (above > 0) {
			cir.setReturnValue(cir.getReturnValueF() + above * 0.05F / 40.0F * (5.0F / 6.0F));
		}
	}
}
