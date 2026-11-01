package org.bitwisemadness.capitalcore.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.capitalcore.CapitalCore;
import org.bitwisemadness.capitalcore.gen.Planner;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;

/** Smoke test plus the plan-model persistence proof for the PoC. */
@GameTestHolder(CapitalCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapitalCoreGameTests {

    private CapitalCoreGameTests() {
    }

    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * The plan survives an NBT roundtrip intact: identity, center, item count
     * and the running flag - the shape crash-resume depends on.
     */
    @GameTest(template = "empty")
    public static void plan_persists_through_nbt(GameTestHelper helper) {
        CapitalPlan plan = Planner.plan("poc", "minecraft:overworld",
                new net.minecraft.core.BlockPos(100, 64, -200));
        plan.setRunning(true);
        plan.items().get(0).markDone();
        CapitalPlan restored = CapitalPlan.read(plan.write());
        helper.assertTrue(restored.id().equals("poc"), "plan id did not survive");
        helper.assertTrue(restored.dimension().equals("minecraft:overworld"), "dimension did not survive");
        helper.assertTrue(restored.center().getX() == 100 && restored.center().getZ() == -200,
                "center did not survive");
        helper.assertTrue(restored.items().size() == plan.items().size(), "item count did not survive");
        helper.assertTrue(restored.doneCount() == 1, "completion state did not survive");
        helper.assertTrue(restored.running(), "running flag did not survive");
        helper.succeed();
    }

    /** The PoC layout is a 4x4 district grid: 16 districts + 24 road segments. */
    @GameTest(template = "empty")
    public static void planner_builds_the_512_footprint(GameTestHelper helper) {
        CapitalPlan plan = Planner.plan("layout", "minecraft:overworld",
                new net.minecraft.core.BlockPos(0, 64, 0));
        long districts = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.DISTRICT).count();
        long roads = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.ROAD).count();
        helper.assertTrue(districts == 16, "expected 16 districts, got " + districts);
        helper.assertTrue(roads == 24, "expected 24 road segments, got " + roads);
        int span = Planner.SPACING * (Planner.GRID - 1);
        helper.assertTrue(span + Planner.SPACING == 512, "footprint is not 512 blocks");
        helper.succeed();
    }
}
