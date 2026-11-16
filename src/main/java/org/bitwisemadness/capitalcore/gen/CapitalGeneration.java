package org.bitwisemadness.capitalcore.gen;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import org.bitwisemadness.capitalcore.event.CapitalGenerationEvent;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;
import org.bitwisemadness.capitalcore.plan.CapitalPlanSavedData;

/**
 * Public generation-request facade: plan, persist, and run a capital in one
 * call, for consumers that generate ahead of demand.
 */
public final class CapitalGeneration {

    private CapitalGeneration() {
    }

    /**
     * Plans a capital at the given center in the given level, persists it, and
     * marks it running. A plan with the same id already present is left
     * unchanged.
     *
     * @return true when a new plan was created and started
     */
    public static boolean request(String id, ServerLevel level, BlockPos center) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(center, "center");
        CapitalPlanSavedData data = CapitalPlanSavedData.get(level.getServer());
        if (data.get(id) != null) {
            return false;
        }
        CapitalPlan plan = Planner.plan(id, level.dimension().location().toString(), level, center);
        data.put(plan);
        plan.setRunning(true);
        data.planChanged();
        NeoForge.EVENT_BUS.post(new CapitalGenerationEvent(CapitalGenerationEvent.Phase.CREATED,
                id, plan.dimension(), 0, plan.items().size(), null));
        return true;
    }
}
