package org.bitwisemadness.capitalcore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.capitalcore.CapitalCore;
import org.bitwisemadness.capitalcore.gen.Builder;
import org.bitwisemadness.capitalcore.gen.Planner;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;

/** Engine coverage: the model, the layout, and the authored interiors. */
@GameTestHolder(CapitalCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapitalCoreGameTests {

    private CapitalCoreGameTests() {
    }

    /** Smoke test: the mod loads and the gametest harness runs. */
    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }

    /** The plan model survives the NBT roundtrip - the crash-resume shape. */
    @GameTest(template = "empty")
    public static void plan_persists_through_nbt(GameTestHelper helper) {
        CapitalPlan plan = Planner.plan("poc", "minecraft:overworld",
                helper.getLevel(), new BlockPos(100, 64, -200));
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

    /** The PoC layout: a 4x4 district grid - 16 districts, 24 roads, 16 skirts. */
    @GameTest(template = "empty")
    public static void planner_builds_the_512_footprint(GameTestHelper helper) {
        CapitalPlan plan = Planner.plan("layout", "minecraft:overworld",
                helper.getLevel(), new BlockPos(0, 64, 0));
        long districts = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.DISTRICT).count();
        long roads = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.ROAD).count();
        long skirts = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.SKIRT).count();
        helper.assertTrue(districts == 16, "expected 16 districts, got " + districts);
        helper.assertTrue(roads == 24, "expected 24 road segments, got " + roads);
        helper.assertTrue(skirts == 16, "expected 16 support skirts, got " + skirts);
        int span = Planner.SPACING * (Planner.GRID - 1);
        helper.assertTrue(span + Planner.SPACING == 512, "footprint is not 512 blocks");
        helper.succeed();
    }

    /**
     * An authored district interior resolves from the datapack and places on
     * the planner's platform without error - the Option C contract (authored
     * pieces fill the interiors the planner lays out).
     */
    @GameTest(template = "empty")
    public static void district_interior_places(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        var template = helper.getLevel().getStructureManager()
                .get(ResourceLocation.parse("capital_core:district/hall"));
        helper.assertTrue(template.isPresent(), "the hall interior template did not resolve");
        var size = template.get().getSize();
        helper.assertTrue(size.getX() == 15 && size.getY() == 9 && size.getZ() == 15,
                "unexpected template size: " + size);
        Builder.buildDistrict(helper.getLevel(), 0, anchor.getX(), anchor.getZ());
        BlockPos origin = anchor.offset(-size.getX() / 2, 1, -size.getZ() / 2);
        var state = helper.getLevel().getBlockState(origin);
        helper.assertTrue(state.is(Blocks.ANDESITE),
                "expected andesite at the template origin, got " + state.getBlock());
        helper.succeed();
    }

    /**
     * Terrain adaptation on a slope (5.6/5.7): the heightmap rises across the
     * footprint, so the platform sits mid-slope. The carve clears the
     * up-slope above the platform; the skirt grounds the down-slope; the
     * authored interior fills the carved space. One test, one terrain - the
     * three terrain gametests were merged because their +-10-block terrain
     * writes crossed the gametest framework's one-block test regions and
     * interfered with each other's assertions.
     */
    @GameTest(template = "empty")
    public static void terrain_adaptation_on_a_slope(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        int cx = base.getX() + 10;
        int cz = base.getZ() + 10;
        // A stone slope rising across the footprint: the heightmap at the
        // center is mid-slope, the +x side is higher, the -x side lower.
        for (int dx = -10; dx <= 10; dx++) {
            int h = base.getY() + Math.min(10, Math.max(0, dx + 5));
            for (int dz = -10; dz <= 10; dz++) {
                for (int y = base.getY() - 2; y <= h; y++) {
                    level.setBlock(new BlockPos(base.getX() + dx, y, base.getZ() + dz),
                            Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
        Builder.buildDistrict(level, 0, cx, cz);
        Builder.buildSkirt(level, cx, cz, ground);
        // The up-slope above the platform is carved to air.
        var upslope = level.getBlockState(new BlockPos(cx + 6, ground + 3, cz));
        helper.assertTrue(upslope.is(Blocks.AIR),
                "the up-slope survived the carve, got " + upslope.getBlock());
        // The interior floor is the hall's andesite.
        var floor = level.getBlockState(new BlockPos(cx + 2, ground + 1, cz + 2));
        helper.assertTrue(floor.is(Blocks.ANDESITE),
                "the interior floor did not place, got " + floor.getBlock());
        // The down-slope under the platform is skirt-grounded.
        var skirt = level.getBlockState(new BlockPos(cx - 8, ground - 3, cz));
        helper.assertTrue(skirt.is(Blocks.STONE),
                "the down-slope is not skirt-grounded, got " + skirt.getBlock());
        helper.succeed();
        // The terrain cleanup: the slope's writes reset to air so the later
        // gametests in the batch see the pristine void, not this test's stone.
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int y = base.getY() - 2; y <= base.getY() + 14; y++) {
                    level.setBlock(new BlockPos(base.getX() + dx, y, base.getZ() + dz),
                            Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

}
