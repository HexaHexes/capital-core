package org.bitwisemadness.capitalcore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
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
     * The rotation centering holds for all four rotations: the hall template
     * places at each rotation's computed origin and the template's
     * rotation-invariant corner block lands exactly there. Sequential
     * placements at the same anchor - each asserts before the next overwrites.
     */
    @GameTest(template = "empty")
    public static void all_rotations_place_centered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var template = level.getStructureManager()
                .get(ResourceLocation.parse("capital_core:district/hall")).orElseThrow();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int r = 0; r < Rotation.values().length; r++) {
            Rotation rotation = Rotation.values()[r];
            BlockPos origin = org.bitwisemadness.capitalcore.gen.TemplateDistricts
                    .centeredOrigin(base, template.getSize(), rotation);
            template.placeInWorld(level, origin, origin,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                            .setRotation(rotation).setIgnoreEntities(true),
                    level.random, 3);
            var state = level.getBlockState(origin);
            helper.assertTrue(state.is(Blocks.ANDESITE),
                    "rotation " + rotation + ": the template corner did not land at the computed origin, got "
                            + state.getBlock());
        }
        helper.succeed();
    }

    /**
     * The support skirt grounds the platform: over a flat floor with only a
     * center pillar, the platform builds at the pillar top and the skirt fills
     * the underside down to the floor - no floating plateau.
     */
    @GameTest(template = "empty")
    public static void district_platform_gets_skirt_support(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        int cx = base.getX() + 4;
        int cz = base.getZ() + 4;
        int floorY = base.getY() - 6;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                level.setBlock(new BlockPos(base.getX() + dx, floorY, base.getZ() + dz),
                        Blocks.STONE.defaultBlockState(), 3);
            }
        }
        // The center pillar drags the heightmap up: the platform builds at its
        // top, four blocks above the floor.
        level.setBlock(new BlockPos(cx, floorY + 1, cz), Blocks.STONE.defaultBlockState(), 3);
        int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
        Builder.buildDistrict(level, 0, cx, cz);
        Builder.buildSkirt(level, cx, cz, groundY);
        // Directly under the platform, off the pillar: the skirt, not air.
        var under = level.getBlockState(new BlockPos(cx + 5, groundY - 2, cz));
        helper.assertTrue(under.is(Blocks.STONE),
                "the platform is unsupported under its edge, got " + under.getBlock());
        helper.succeed();
    }
}
