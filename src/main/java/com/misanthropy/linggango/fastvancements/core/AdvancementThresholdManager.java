package com.misanthropy.linggango.fastvancements.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

/**
 * Collects every item-count boundary used by minecraft:inventory_changed criteria.
 * <p>
 * A boundary {@code b} means "the result of some count check can flip between count b-1 and b":
 * {@code min: m} contributes m, {@code max: M} contributes M+1 and an exact count N contributes N and N+1.
 * When a slot only changes its count (same item, same components) and no boundary lies between the old
 * and the new count, no count predicate can change its answer, so the trigger can be skipped.
 */
public final class AdvancementThresholdManager extends SimplePreparableReloadListener<int[]> {
    private static final String INVENTORY_CHANGED = "minecraft:inventory_changed";

    private static volatile int[] boundaries = new int[0];

    @Override
    protected int[] prepare(ResourceManager manager, ProfilerFiller profiler) {
        IntSortedSet set = new IntAVLTreeSet();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("advancement", rl -> rl.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                collect(GsonHelper.parse(reader), set);
            } catch (Exception ignored) {
                // Broken files are reported by vanilla's own advancement loader.
            }
        }
        return set.toIntArray();
    }

    @Override
    protected void apply(int[] data, ResourceManager manager, ProfilerFiller profiler) {
        boundaries = data;
    }

    private static void collect(JsonObject root, IntSortedSet out) {
        if (!(root.get("criteria") instanceof JsonObject criteria)) return;
        for (Map.Entry<String, JsonElement> c : criteria.entrySet()) {
            if (!(c.getValue() instanceof JsonObject criterion)) continue;
            JsonElement trigger = criterion.get("trigger");
            if (trigger == null || !trigger.isJsonPrimitive() || !INVENTORY_CHANGED.equals(trigger.getAsString())) continue;
            if (!(criterion.get("conditions") instanceof JsonObject conditions)) continue;
            if (!(conditions.get("items") instanceof com.google.gson.JsonArray items)) continue;
            for (JsonElement item : items) {
                if (item instanceof JsonObject predicate) collectCount(predicate.get("count"), out);
            }
        }
    }

    private static void collectCount(JsonElement count, IntSortedSet out) {
        if (count == null) return;
        try {
            if (count.isJsonPrimitive()) {
                int n = count.getAsInt();
                out.add(n);
                out.add(n + 1);
            } else if (count instanceof JsonObject range) {
                if (range.get("min") instanceof JsonElement min && min.isJsonPrimitive()) out.add(min.getAsInt());
                if (range.get("max") instanceof JsonElement max && max.isJsonPrimitive()) out.add(max.getAsInt() + 1);
            }
        } catch (RuntimeException ignored) {
            // Not a number; vanilla will reject the advancement anyway.
        }
    }

    /**
     * @return true when a count change from {@code prev} to {@code stack.getCount()} of the same item could
     *         change the outcome of any inventory_changed criterion.
     */
    public static boolean countChangeMatters(int prev, ItemStack stack) {
        int cur = stack.getCount();
        if (prev <= 0 || cur <= 0 || prev == cur) return true;
        int lo = Math.min(prev, cur), hi = Math.max(prev, cur);
        // Becoming full / no longer full changes the "slots.full" count.
        int max = stack.getMaxStackSize();
        if (lo < max && max <= hi) return true;
        // Any boundary b with lo < b <= hi?
        int[] b = boundaries;
        int i = Arrays.binarySearch(b, lo + 1);
        if (i < 0) i = -i - 1;
        return i < b.length && b[i] <= hi;
    }
}
