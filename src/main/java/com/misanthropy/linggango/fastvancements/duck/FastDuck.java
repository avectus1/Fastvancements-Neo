package com.misanthropy.linggango.fastvancements.duck;

import net.minecraft.world.item.ItemStack;

public interface FastDuck {
    interface TriggerInstanceDuck {
        /** True if this criterion cannot become satisfied because of a change to {@code changed}. */
        boolean fastvancements$canFastFail(ItemStack changed);
    }

    interface PlayerThrottleDuck {
        /** @param which 0 = location, 1 = tick */
        boolean fastvancements$shouldSkip(int which, int skip);
    }
}
