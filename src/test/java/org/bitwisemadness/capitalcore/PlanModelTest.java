package org.bitwisemadness.capitalcore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.bitwisemadness.capitalcore.gen.Planner;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;
import org.junit.jupiter.api.Test;

/** Pure-JVM checks on the plan model and the PoC layout invariants. */
class PlanModelTest {

    @Test
    void plannerEmitsDistrictsAndRoads() {
        CapitalPlan plan = Planner.plan("t", "minecraft:overworld", new BlockPos(0, 64, 0));
        long districts = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.DISTRICT).count();
        long roads = plan.items().stream()
                .filter(i -> i.kind() == CapitalPlan.Kind.ROAD).count();
        assertEquals(16, districts);
        assertEquals(24, roads);
    }

    @Test
    void planSurvivesNbtRoundtrip() {
        CapitalPlan plan = Planner.plan("t", "minecraft:overworld", new BlockPos(10, 64, -30));
        plan.setRunning(true);
        plan.items().get(3).markDone();
        CapitalPlan restored = CapitalPlan.read(plan.write());
        assertEquals("t", restored.id());
        assertEquals("minecraft:overworld", restored.dimension());
        assertEquals(new BlockPos(10, 64, -30), restored.center());
        assertEquals(plan.items().size(), restored.items().size());
        assertEquals(1, restored.doneCount());
        assertTrue(restored.running());
    }

    @Test
    void completeNextDrainsInOrder() {
        CapitalPlan plan = Planner.plan("t", "minecraft:overworld", new BlockPos(0, 64, 0));
        int size = plan.items().size();
        for (int i = 0; i < size; i++) {
            assertTrue(plan.completeNext());
        }
        assertTrue(plan.complete());
        assertTrue(!plan.completeNext());
    }
}
