package com.misanthropy.linggango.fastvancements.mixin;

import com.misanthropy.linggango.fastvancements.duck.FastDuck;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Per-player throttle counters live on the player itself: no UUID-keyed maps, no boxing,
 * nothing to clean up on logout, and they reset naturally on respawn.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements FastDuck.PlayerThrottleDuck {
    @Unique
    private int fastvancements$locationSkip;
    @Unique
    private int fastvancements$tickSkip;

    @Override
    public boolean fastvancements$shouldSkip(int which, int skip) {
        if (which == 0) {
            int left = fastvancements$locationSkip;
            if (left > 0) {
                fastvancements$locationSkip = Math.min(left, skip) - 1;
                return true;
            }
            fastvancements$locationSkip = skip;
        } else {
            int left = fastvancements$tickSkip;
            if (left > 0) {
                fastvancements$tickSkip = Math.min(left, skip) - 1;
                return true;
            }
            fastvancements$tickSkip = skip;
        }
        return false;
    }
}
