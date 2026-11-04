package org.bitwisemadness.capitalcore.gen;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.bitwisemadness.capitalcore.CapitalCore;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;
import org.bitwisemadness.capitalcore.plan.CapitalPlanSavedData;

/**
 * The generation budget controller (spec §5.9): drains every running plan's
 * pending work items on the server tick, stopping the moment the per-tick
 * time budget is spent. Work items are atomic, so a budget cut lands between
 * items; the plan's persisted state makes the next tick - or the next server,
 * after a crash - resume exactly where this one stopped (§5.8).
 */
public final class GenerationDriver {

    /** Per-tick generation budget in nanoseconds (the 50 ms gate has huge headroom). */
    private static final long BUDGET_NANOS = 5_000_000L;

    private final Set<String> resumedLogged = new HashSet<>();

    /** Tick hook: drains all running plans within the budget. */
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        CapitalPlanSavedData data = CapitalPlanSavedData.get(server);
        boolean anyRunning = data.allPlans().stream().anyMatch(CapitalPlan::running);
        if (!anyRunning) {
            return;
        }
        long start = System.nanoTime();
        for (CapitalPlan plan : data.allPlans()) {
            if (!plan.running() || plan.complete()) {
                continue;
            }
            ServerLevel level = server.getLevel(dimension(plan.dimension()));
            if (level == null) {
                continue;
            }
            logResumeOnce(plan);
            while (System.nanoTime() - start < BUDGET_NANOS) {
                CapitalPlan.Item item = firstPending(plan);
                if (item == null) {
                    finish(plan);
                    break;
                }
                execute(level, plan, item);
                data.planChanged();
            }
        }
        long spent = System.nanoTime() - start;
        CapitalCore.LOGGER.debug("capitalgen: tick budget spent {} ns", spent);
    }

    private void logResumeOnce(CapitalPlan plan) {
        if (resumedLogged.add(plan.id()) && plan.doneCount() > 0) {
            CapitalCore.LOGGER.info("capitalgen: resumed plan '{}' with {}/{} items already built",
                    plan.id(), plan.doneCount(), plan.items().size());
        }
    }

    private CapitalPlan.Item firstPending(CapitalPlan plan) {
        for (CapitalPlan.Item item : plan.items()) {
            if (!item.done()) {
                return item;
            }
        }
        return null;
    }

    private void execute(ServerLevel level, CapitalPlan plan, CapitalPlan.Item item) {
        if (item.kind() == CapitalPlan.Kind.DISTRICT) {
            Builder.buildDistrict(level, item.endX(), item.x(), item.z());
        } else {
            Builder.buildRoad(level, item.x(), item.z(), item.endX(), item.endZ());
        }
        item.markDone();
    }

    private void finish(CapitalPlan plan) {
        plan.setRunning(false);
        CapitalCore.LOGGER.info("capitalgen: plan '{}' complete ({} items)", plan.id(), plan.items().size());
    }

    private static ResourceKey<net.minecraft.world.level.Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(name));
    }
}
