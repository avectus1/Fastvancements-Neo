package com.misanthropy.linggango.fastvancements.core;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config keys are identical to the original port so existing fastvancements-common.toml files keep working.
 * Values are mirrored into plain static fields because they are read on very hot paths.
 */
public final class FastvancementsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("Optimizations");
    }

    private static final ModConfigSpec.BooleanValue CONTAINER_MENU = BUILDER
            .comment("Don't re-fire inventory_changed for every player-inventory slot each time a container is opened,",
                    "and remember the previous stack size of changed slots so count-only changes can be skipped.")
            .define("enableContainerMenuOptimization", true);
    private static final ModConfigSpec.BooleanValue INVENTORY_CHANGE = BUILDER
            .comment("Use the optimized inventory_changed trigger (lazy inventory scan, item pre-filtering,",
                    "skipping stack-size changes that cross no count threshold used by any advancement).")
            .define("enableInventoryChangeOptimization", true);
    private static final ModConfigSpec.BooleanValue LOCATION_TICK = BUILDER
            .comment("Throttle the minecraft:location and minecraft:tick triggers per player.",
                    "Other player triggers (slept_in_bed, hero_of_the_village, ...) are never throttled.")
            .define("enableLocationTickOptimization", true);
    private static final ModConfigSpec.IntValue SKIP = BUILDER
            .comment("How many trigger calls to skip after each processed one. 0 = vanilla.",
                    "Vanilla checks location once per second and tick once per tick, so 10 means",
                    "location advancements are checked every ~11 s and tick advancements every 11 ticks.")
            .defineInRange("locationTickSkipTicks", 10, 0, 1000);
    private static final ModConfigSpec.BooleanValue FAST_ITEM = BUILDER
            .comment("Skip criteria whose item list cannot match the changed item without evaluating them.")
            .define("enableFastItemMatching", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean containerMenuOpt = true;
    public static boolean inventoryChangeOpt = true;
    public static boolean locationTickOpt = true;
    public static int locationSkipTicks = 10;
    public static boolean fastItemMatching = true;

    private FastvancementsConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading || event.getConfig().getSpec() != SPEC) return;
        containerMenuOpt = CONTAINER_MENU.getAsBoolean();
        inventoryChangeOpt = INVENTORY_CHANGE.getAsBoolean();
        locationTickOpt = LOCATION_TICK.getAsBoolean();
        locationSkipTicks = SKIP.getAsInt();
        fastItemMatching = FAST_ITEM.getAsBoolean();
    }
}
