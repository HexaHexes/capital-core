package org.bitwisemadness.capitalcore.plan;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The world-scoped home for every {@link CapitalPlan}: persisted under the
 * overworld's {@code DimensionDataStorage} so plans survive restarts and
 * crashes (the verified 1.21.1 API - a {@code Factory} with a supplier
 * constructor, a deserializer and a null {@code DataFixTypes}).
 */
public final class CapitalPlanSavedData extends SavedData {

    private static final String DATA_NAME = "capital_generation_plans";

    private final Map<String, CapitalPlan> plans = new HashMap<>();

    public static CapitalPlanSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static final SavedData.Factory<CapitalPlanSavedData> FACTORY =
            new SavedData.Factory<>(CapitalPlanSavedData::new, CapitalPlanSavedData::load, null);

    public CapitalPlanSavedData() {
    }

    private CapitalPlanSavedData(CompoundTag tag) {
        ListTag planTags = tag.getList("plans", Tag.TAG_COMPOUND);
        for (int i = 0; i < planTags.size(); i++) {
            CapitalPlan plan = CapitalPlan.read(planTags.getCompound(i));
            plans.put(plan.id(), plan);
        }
    }

    public CapitalPlan get(String id) {
        return plans.get(id);
    }

    public java.util.Collection<CapitalPlan> allPlans() {
        return java.util.Collections.unmodifiableCollection(plans.values());
    }

    public void put(CapitalPlan plan) {
        plans.put(plan.id(), plan);
        setDirty();
    }

    /** Marks the storage dirty after any plan mutation (the crash-resume contract). */
    public void planChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, Provider registries) {
        ListTag planTags = new ListTag();
        for (CapitalPlan plan : plans.values()) {
            planTags.add(plan.write());
        }
        tag.put("plans", planTags);
        return tag;
    }

    private static CapitalPlanSavedData load(CompoundTag tag, Provider registries) {
        return new CapitalPlanSavedData(tag);
    }
}
