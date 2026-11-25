package com.misanthropy.linggango.fastvancements.core;

import net.minecraft.world.item.ItemStack;

/**
 * Hands the previous count of a slot from AbstractContainerMenu#triggerSlotListeners to the
 * inventory_changed trigger that the listeners call right after.
 * <p>
 * Vanilla passes a fresh copy of the slot's stack to the listeners, so this is matched by identity.
 * Using a side channel instead of a field on ItemStack keeps every ItemStack in the game 8 bytes smaller.
 */
public final class PrevStackSize {
    private record Pending(ItemStack stack, int prevCount) {}

    private static volatile Pending pending;

    private PrevStackSize() {}

    public static void set(ItemStack stack, int prevCount) {
        pending = new Pending(stack, prevCount);
    }

    /** @return the previous count if {@code stack} is the copy that was just stamped, otherwise -1. */
    public static int get(ItemStack stack) {
        Pending p = pending;
        return p != null && p.stack == stack ? p.prevCount : -1;
    }
}
