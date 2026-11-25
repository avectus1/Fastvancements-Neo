package com.misanthropy.linggango.fastvancements;

import com.misanthropy.linggango.fastvancements.core.AdvancementThresholdManager;
import com.misanthropy.linggango.fastvancements.core.FastvancementsConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(Fastvancements.MODID)
public final class Fastvancements {
    public static final String MODID = "fastvancements";

    public Fastvancements(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, FastvancementsConfig.SPEC);
        modBus.addListener(FastvancementsConfig::onConfigEvent);
        NeoForge.EVENT_BUS.addListener(Fastvancements::onAddReloadListeners);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AdvancementThresholdManager());
    }
}
