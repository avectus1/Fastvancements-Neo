package com.misanthropy.linggango.fastvancements.mixin;

import com.misanthropy.linggango.fastvancements.core.FastvancementsConfig;
import com.misanthropy.linggango.fastvancements.duck.FastDuck;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InventoryChangeTrigger.TriggerInstance.class)
public abstract class InventoryChangeTriggerInstanceMixin implements FastDuck.TriggerInstanceDuck {
    @Unique
    private boolean fastvancements$analyzed;
    /** Union of every item any of this criterion's predicates can match, or null if that set is unbounded. */
    @Unique
    private Set<Item> fastvancements$items;

    @Override
    public boolean fastvancements$canFastFail(ItemStack changed) {
        if (!FastvancementsConfig.fastItemMatching) return false;
        if (!fastvancements$analyzed) {
            fastvancements$items = fastvancements$collectItems();
            fastvancements$analyzed = true;
        }
        Set<Item> items = fastvancements$items;
        return items != null && !items.contains(changed.getItem());
    }

    @Unique
    private Set<Item> fastvancements$collectItems() {
        InventoryChangeTrigger.TriggerInstance self = (InventoryChangeTrigger.TriggerInstance) (Object) this;
        // Slot-count conditions can be affected by any item, and an empty item list matches everything.
        if (self.items().isEmpty() || !InventoryChangeTrigger.TriggerInstance.Slots.ANY.equals(self.slots())) return null;
        Set<Item> result = new ReferenceOpenHashSet<>();
        try {
            for (ItemPredicate predicate : self.items()) {
                Optional<HolderSet<Item>> items = predicate.items();
                if (items.isEmpty()) return null;
                for (Holder<Item> holder : items.get()) result.add(holder.value());
            }
        } catch (RuntimeException e) {
            return null; // e.g. unbound tag: just don't optimize this criterion
        }
        return result;
    }
}
