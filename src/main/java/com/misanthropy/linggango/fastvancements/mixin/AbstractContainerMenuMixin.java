package com.misanthropy.linggango.fastvancements.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.linggango.fastvancements.core.FastvancementsConfig;
import com.misanthropy.linggango.fastvancements.core.PrevStackSize;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    /**
     * Vanilla seeds lastSlots with EMPTY, so the first broadcastChanges() after opening any container reports
     * every non-empty player-inventory slot as changed and fires inventory_changed once per slot (each with a
     * full inventory scan). Seed player-inventory slots with their current contents instead: nothing changed.
     * (ordinal 0 = slots.add, 1 = lastSlots.add, 2 = remoteSlots.add)
     */
    @ModifyArg(method = "addSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;add(Ljava/lang/Object;)Z", ordinal = 1))
    private Object fastvancements$seedLastSlot(Object original, @Local(argsOnly = true) Slot slot) {
        if (FastvancementsConfig.containerMenuOpt && slot.container instanceof Inventory && slot.hasItem()) {
            return slot.getItem().copy();
        }
        return original;
    }

    /** Remember the old count when only the count of a slot changed, for the inventory_changed trigger. */
    @WrapOperation(method = "triggerSlotListeners", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object fastvancements$stampPrevSize(NonNullList<Object> list, int index, Object value, Operation<Object> original) {
        Object old = original.call(list, index, value);
        if (FastvancementsConfig.containerMenuOpt && old instanceof ItemStack oldStack && value instanceof ItemStack newStack
                && !oldStack.isEmpty() && !newStack.isEmpty() && ItemStack.isSameItemSameComponents(oldStack, newStack)) {
            PrevStackSize.set(newStack, oldStack.getCount());
        }
        return old;
    }
}
