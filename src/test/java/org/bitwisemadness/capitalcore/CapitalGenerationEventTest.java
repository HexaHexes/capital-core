package org.bitwisemadness.capitalcore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bitwisemadness.capitalcore.event.CapitalGenerationEvent;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;
import org.junit.jupiter.api.Test;

/** Pure-JVM checks on the generation lifecycle event. */
class CapitalGenerationEventTest {

    @Test
    void carriesPhaseAndCounts() {
        CapitalGenerationEvent event = new CapitalGenerationEvent(
                CapitalGenerationEvent.Phase.ITEM_BUILT, "p", "minecraft:overworld",
                3, 56, CapitalPlan.Kind.ROAD);
        assertEquals(CapitalGenerationEvent.Phase.ITEM_BUILT, event.phase());
        assertEquals("p", event.planId());
        assertEquals("minecraft:overworld", event.dimension());
        assertEquals(3, event.itemsDone());
        assertEquals(56, event.itemsTotal());
        assertEquals(CapitalPlan.Kind.ROAD, event.itemKind());
    }

    @Test
    void itemKindIsNullOutsideItemBuilt() {
        CapitalGenerationEvent event = new CapitalGenerationEvent(
                CapitalGenerationEvent.Phase.COMPLETE, "p", "minecraft:overworld", 56, 56, null);
        assertNull(event.itemKind());
    }

    @Test
    void rejectsInvalidCounts() {
        assertThrows(IllegalArgumentException.class, () -> new CapitalGenerationEvent(
                CapitalGenerationEvent.Phase.CREATED, "p", "minecraft:overworld", -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new CapitalGenerationEvent(
                CapitalGenerationEvent.Phase.CREATED, "p", "minecraft:overworld", 2, 1, null));
        assertThrows(NullPointerException.class, () -> new CapitalGenerationEvent(
                null, "p", "minecraft:overworld", 0, 0, null));
    }
}
