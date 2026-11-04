package org.bitwisemadness.capitalcore.gen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;

/**
 * The layout planner: turns a center position into a persisted work-item plan.
 * The PoC layout is a 4x4 district grid at 128-block spacing - a 512-block
 * footprint - with road segments between orthogonally adjacent districts
 * (SPIKE-CAPITAL-GEN-01 plan-then-place, spec §5.3/§5.5).
 */
public final class Planner {

    /** Districts per axis. */
    public static final int GRID = 4;

    /** Spacing between district centers, in blocks. */
    public static final int SPACING = 128;

    private Planner() {
    }

    /**
     * Builds the plan for a capital centered on {@code center} in
     * {@code dimension}. District centers sit on the 4x4 grid; road segments
     * connect orthogonally adjacent districts.
     */
    public static CapitalPlan plan(String id, String dimension, BlockPos center) {
        List<CapitalPlan.Item> items = new ArrayList<>();
        int half = (GRID - 1) / 2;
        int first = -half * SPACING;
        int[][] centers = new int[GRID * GRID][2];
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                int dx = center.getX() + first + gx * SPACING;
                int dz = center.getZ() + first + gz * SPACING;
                centers[gx * GRID + gz] = new int[]{dx, dz};
                // x2 carries the stable district ordinal: the interior template
                // selector must be identical across crash-resumes.
                items.add(new CapitalPlan.Item(CapitalPlan.Kind.DISTRICT, dx, dz, gx * GRID + gz, dz, false));
            }
        }
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                int[] here = centers[gx * GRID + gz];
                if (gx + 1 < GRID) {
                    int[] right = centers[(gx + 1) * GRID + gz];
                    items.add(new CapitalPlan.Item(CapitalPlan.Kind.ROAD,
                            here[0], here[1], right[0], right[1], false));
                }
                if (gz + 1 < GRID) {
                    int[] down = centers[gx * GRID + gz + 1];
                    items.add(new CapitalPlan.Item(CapitalPlan.Kind.ROAD,
                            here[0], here[1], down[0], down[1], false));
                }
            }
        }
        return new CapitalPlan(id, dimension, center, items);
    }
}
