package com.misanthropy.linggango.fastvancements.mixin;

import com.misanthropy.linggango.fastvancements.core.AdvancementThresholdManager;
import com.misanthropy.linggango.fastvancements.core.FastvancementsConfig;
import com.misanthropy.linggango.fastvancements.core.PrevStackSize;
import com.misanthropy.linggango.fastvancements.duck.FastDuck;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same result as vanilla's InventoryChangeTrigger#trigger (including the "player" condition check), but:
 * <ul>
 *   <li>returns before scanning the inventory when the player has no inventory_changed listeners,</li>
 *   <li>skips criteria whose item list can't contain the changed item,</li>
 *   <li>skips count-only changes that cross no count boundary used by any advancement,</li>
 *   <li>scans the inventory at most once, and only if some criterion actually needs it.</li>
 * </ul>
 * Criteria that have a "player" condition are always fully evaluated, because the player's state can make
 * them pass even when the inventory change itself is irrelevant.
 */
@Mixin(InventoryChangeTrigger.class)
public abstract class InventoryChangeTriggerMixin {

    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void fastvancements$trigger(ServerPlayer player, Inventory inventory, ItemStack stack, CallbackInfo ci) {
        if (!FastvancementsConfig.inventoryChangeOpt) return;
        ci.cancel();

        PlayerAdvancements advancements = player.getAdvancements();
        Set<CriterionTrigger.Listener<InventoryChangeTrigger.TriggerInstance>> listeners =
                ((SimpleCriterionTriggerAccessor<InventoryChangeTrigger.TriggerInstance>) (Object) this)
                        .fastvancements$getPlayers().get(advancements);
        if (listeners == null || listeners.isEmpty()) return;

        int prev = FastvancementsConfig.containerMenuOpt ? PrevStackSize.get(stack) : -1;
        boolean irrelevantCountChange = prev > 0 && !AdvancementThresholdManager.countChangeMatters(prev, stack);

        boolean scanned = false;
        int full = 0, empty = 0, occupied = 0;
        LootContext context = null;
        List<CriterionTrigger.Listener<InventoryChangeTrigger.TriggerInstance>> matched = null;

        for (CriterionTrigger.Listener<InventoryChangeTrigger.TriggerInstance> listener : listeners) {
            InventoryChangeTrigger.TriggerInstance instance = listener.trigger();
            Optional<ContextAwarePredicate> playerPredicate = instance.player();

            if (playerPredicate.isEmpty()) {
                if (irrelevantCountChange) continue;
                if (((FastDuck.TriggerInstanceDuck) (Object) instance).fastvancements$canFastFail(stack)) continue;
            }

            if (!scanned) {
                scanned = true;
                for (int i = 0, size = inventory.getContainerSize(); i < size; i++) {
                    ItemStack slot = inventory.getItem(i);
                    if (slot.isEmpty()) {
                        empty++;
                    } else {
                        occupied++;
                        if (slot.getCount() >= slot.getMaxStackSize()) full++;
                    }
                }
            }

            if (!instance.matches(inventory, stack, full, empty, occupied)) continue;

            if (playerPredicate.isPresent()) {
                if (context == null) context = EntityPredicate.createContext(player, player);
                if (!playerPredicate.get().matches(context)) continue;
            }

            if (matched == null) matched = new ArrayList<>(2);
            matched.add(listener);
        }

        // Run after iterating: awarding a criterion removes listeners from the set.
        if (matched != null) {
            for (CriterionTrigger.Listener<InventoryChangeTrigger.TriggerInstance> listener : matched) {
                listener.run(advancements);
            }
        }
    }
}
