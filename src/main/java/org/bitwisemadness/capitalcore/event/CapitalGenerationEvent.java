package org.bitwisemadness.capitalcore.event;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;

/**
 * Lifecycle event for capital generation, fired on the server event bus at
 * the existing generation paths. Notification only; consumers observe or
 * audit generation.
 */
public final class CapitalGenerationEvent extends Event {

    /** The lifecycle phase the event reports. */
    public enum Phase { CREATED, RESUMED, ITEM_BUILT, COMPLETE }

    private final Phase phase;
    private final String planId;
    private final String dimension;
    private final long itemsDone;
    private final long itemsTotal;
    private final CapitalPlan.Kind itemKind;

    public CapitalGenerationEvent(Phase phase, String planId, String dimension,
                                  long itemsDone, long itemsTotal, CapitalPlan.Kind itemKind) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.planId = Objects.requireNonNull(planId, "planId");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        if (itemsDone < 0) {
            throw new IllegalArgumentException("itemsDone must not be negative");
        }
        if (itemsTotal < itemsDone) {
            throw new IllegalArgumentException("itemsTotal must not be below itemsDone");
        }
        this.itemsDone = itemsDone;
        this.itemsTotal = itemsTotal;
        this.itemKind = itemKind;
    }

    /** The lifecycle phase. */
    public Phase phase() {
        return phase;
    }

    /** The plan id. */
    public String planId() {
        return planId;
    }

    /** The dimension the plan generates in, as a string key. */
    public String dimension() {
        return dimension;
    }

    /** Completed work items at the time of the event. */
    public long itemsDone() {
        return itemsDone;
    }

    /** Total work items in the plan. */
    public long itemsTotal() {
        return itemsTotal;
    }

    /** The built item kind for {@link Phase#ITEM_BUILT}; null otherwise. */
    public CapitalPlan.Kind itemKind() {
        return itemKind;
    }
}
