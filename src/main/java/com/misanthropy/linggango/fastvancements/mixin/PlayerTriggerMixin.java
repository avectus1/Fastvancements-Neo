package com.misanthropy.linggango.fastvancements.mixin;

import com.misanthropy.linggango.fastvancements.core.FastvancementsConfig;
import com.misanthropy.linggango.fastvancements.duck.FastDuck;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerTrigger is shared by location, tick, slept_in_bed, hero_of_the_village, voluntary_exile and
 * avoid_vibration. Only the two periodic ones (location, tick) are throttled; the others are one-shot events.
 */
@Mixin(PlayerTrigger.class)
public abstract class PlayerTriggerMixin {
    /** 0 = not resolved yet, 1 = location, 2 = tick, 3 = never throttle */
    @Unique
    private byte fastvancements$kind;

    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
    private void fastvancements$throttle(ServerPlayer player, CallbackInfo ci) {
        if (!FastvancementsConfig.locationTickOpt) return;
        int skip = FastvancementsConfig.locationSkipTicks;
        if (skip <= 0) return;

        byte kind = fastvancements$kind;
        if (kind == 0) {
            Object self = this;
            kind = self == CriteriaTriggers.LOCATION ? (byte) 1 : self == CriteriaTriggers.TICK ? (byte) 2 : (byte) 3;
            fastvancements$kind = kind;
        }
        if (kind == 3) return;

        if (((FastDuck.PlayerThrottleDuck) player).fastvancements$shouldSkip(kind - 1, skip)) ci.cancel();
    }
}
