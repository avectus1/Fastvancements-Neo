package fvtest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@Mod("fvtest")
@GameTestHolder("fvtest")
@PrefixGameTestTemplate(false)
public class FastvancementsTests {
    public FastvancementsTests() {}

    private static AdvancementHolder adv(GameTestHelper h, String id) {
        AdvancementHolder a = h.getLevel().getServer().getAdvancements().get(ResourceLocation.parse(id));
        if (a == null) throw new IllegalStateException("missing advancement " + id);
        return a;
    }

    private static boolean done(GameTestHelper h, ServerPlayer p, String id) {
        return p.getAdvancements().getOrStartProgress(adv(h, id)).isDone();
    }

    private static void revoke(GameTestHelper h, ServerPlayer p, String id, String criterion) {
        p.getAdvancements().revoke(adv(h, id), criterion);
    }

    private static void sync(ServerPlayer p) {
        p.containerMenu.broadcastChanges();
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        System.out.println("[FVTEST] " + (ok ? "PASS " : "FAIL ") + what);
        h.assertTrue(ok, what);
    }

    @GameTest(template = "empty")
    public static void vanillaRootAdvancement(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().add(new ItemStack(Items.CRAFTING_TABLE));
        sync(p);
        check(h, done(h, p, "minecraft:story/root"), "picking up a crafting table grants story/root");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void countThreshold(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 1));
        sync(p);
        check(h, done(h, p, "fvtest:any_diamond"), "first diamond grants any_diamond");
        check(h, !done(h, p, "fvtest:five_diamonds"), "1 diamond doesn't satisfy count>=5");
        p.getInventory().getItem(0).grow(2);
        sync(p);
        check(h, !done(h, p, "fvtest:five_diamonds"), "3 diamonds doesn't satisfy count>=5");
        p.getInventory().getItem(0).grow(2);
        sync(p);
        check(h, done(h, p, "fvtest:five_diamonds"), "growing the same stack to 5 grants five_diamonds");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void countOnlyChangesAreSkipped(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        sync(p);
        revoke(h, p, "fvtest:any_diamond", "c");
        p.getInventory().getItem(0).grow(1);
        sync(p);
        check(h, !done(h, p, "fvtest:any_diamond"), "2->3 count-only change is skipped (optimization active)");
        p.getInventory().setItem(5, new ItemStack(Items.DIAMOND));
        sync(p);
        check(h, done(h, p, "fvtest:any_diamond"), "diamond in a new slot is processed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullSlotCounts(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 63));
        sync(p);
        check(h, !done(h, p, "fvtest:one_full"), "63/64 is not a full slot");
        p.getInventory().getItem(0).grow(1);
        sync(p);
        check(h, done(h, p, "fvtest:one_full"), "63->64 makes exactly one full slot (slots.full = 1)");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void playerCondition(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.STICK));
        sync(p);
        check(h, !done(h, p, "fvtest:burning_stick"), "stick while NOT on fire must not grant burning_stick");
        p.setRemainingFireTicks(200);
        p.getInventory().setItem(1, new ItemStack(Items.STICK));
        sync(p);
        check(h, done(h, p, "fvtest:burning_stick"), "stick while on fire grants burning_stick");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void multiItem(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
        sync(p);
        p.getInventory().setItem(1, new ItemStack(Items.DIRT));
        sync(p);
        check(h, !done(h, p, "fvtest:multi"), "diamond + dirt doesn't satisfy diamond+emerald");
        p.getInventory().setItem(2, new ItemStack(Items.EMERALD));
        sync(p);
        check(h, done(h, p, "fvtest:multi"), "diamond + emerald grants multi");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oneShotPlayerTriggersNotThrottled(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        CriteriaTriggers.SLEPT_IN_BED.trigger(p);
        check(h, done(h, p, "minecraft:adventure/sleep_in_bed"), "first slept_in_bed grants sleep_in_bed");
        revoke(h, p, "minecraft:adventure/sleep_in_bed", "slept_in_bed");
        CriteriaTriggers.SLEPT_IN_BED.trigger(p);
        check(h, done(h, p, "minecraft:adventure/sleep_in_bed"), "second slept_in_bed right after is not dropped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tickTriggerThrottled(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        CriteriaTriggers.TICK.trigger(p);
        check(h, done(h, p, "fvtest:tick_adv"), "first tick trigger is processed");
        revoke(h, p, "fvtest:tick_adv", "c");
        for (int i = 0; i < 10; i++) CriteriaTriggers.TICK.trigger(p);
        check(h, !done(h, p, "fvtest:tick_adv"), "next 10 tick triggers are skipped (skip=10)");
        CriteriaTriggers.TICK.trigger(p);
        check(h, done(h, p, "fvtest:tick_adv"), "11th tick trigger is processed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openingContainerDoesNotRefireInventory(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        sync(p);
        revoke(h, p, "fvtest:any_diamond", "c");
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.threeRows(id, inv), Component.literal("test")));
        check(h, p.containerMenu instanceof ChestMenu, "chest menu opened");
        check(h, !done(h, p, "fvtest:any_diamond"), "opening a chest doesn't re-fire inventory_changed for unchanged slots");
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND));
        sync(p);
        check(h, done(h, p, "fvtest:any_diamond"), "real change while chest is open is still detected");
        p.closeContainer();
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void zzBenchmark(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        net.minecraft.world.item.Item[] fill = {Items.DIRT, Items.COBBLESTONE, Items.OAK_LOG, Items.STONE, Items.SAND, Items.GRAVEL,
                Items.TORCH, Items.COAL, Items.BREAD, Items.OAK_PLANKS, Items.GRANITE, Items.ANDESITE, Items.DIORITE, Items.STICK};
        for (int i = 0; i < 30; i++) p.getInventory().setItem(i, new ItemStack(fill[i % fill.length], 10 + i));
        sync(p);
        net.minecraft.world.item.Item[] swap = {Items.DIRT, Items.COBBLESTONE, Items.OAK_LOG, Items.GRAVEL};
        for (int round = 0; round < 3; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < 200_000; i++) {
                ItemStack s = p.getInventory().getItem(0);
                if ((i & 1) == 0) s.grow(1); else s.shrink(1);
                sync(p);
            }
            long t1 = System.nanoTime();
            for (int i = 0; i < 200_000; i++) {
                p.getInventory().setItem(31, new ItemStack(swap[i & 3], 1));
                sync(p);
            }
            long t2 = System.nanoTime();
            for (int i = 0; i < 2_000; i++) {
                p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.threeRows(id, inv), Component.literal("b")));
                p.closeContainer();
            }
            long t3 = System.nanoTime();
            System.out.printf("[FVBENCH] round %d: count-only change %.0f ns/op | item swap %.0f ns/op | open+close chest %.0f ns/op%n",
                    round, (t1 - t0) / 200_000.0, (t2 - t1) / 200_000.0, (t3 - t2) / 2_000.0);
        }
        h.succeed();
    }
}
