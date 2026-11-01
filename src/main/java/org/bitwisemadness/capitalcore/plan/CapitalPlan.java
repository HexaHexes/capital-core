package org.bitwisemadness.capitalcore.plan;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * A persisted capital generation plan: the stable identity, the dimension and
 * center it belongs to, and every work item with its completion state. The
 * plan is written to {@code SavedData} before construction starts and is the
 * unit of crash-resume (§5.3/§5.8).
 */
public final class CapitalPlan {

    /** The two work-item kinds the PoC engine emits. */
    public enum Kind {
        DISTRICT, ROAD
    }

    /** One unit of construction work: a district build or a road segment. */
    public static final class Item {

        private final Kind kind;
        private final int x;
        private final int z;
        private final int x2;
        private final int z2;
        private boolean done;

        public Item(Kind kind, int x, int z, int x2, int z2, boolean done) {
            this.kind = kind;
            this.x = x;
            this.z = z;
            this.x2 = x2;
            this.z2 = z2;
            this.done = done;
        }

        public Kind kind() {
            return kind;
        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        public int endX() {
            return x2;
        }

        public int endZ() {
            return z2;
        }

        public boolean done() {
            return done;
        }

        public void markDone() {
            this.done = true;
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", kind.name());
            tag.putInt("x", x);
            tag.putInt("z", z);
            tag.putInt("x2", x2);
            tag.putInt("z2", z2);
            tag.putBoolean("done", done);
            return tag;
        }

        static Item read(CompoundTag tag) {
            Kind kind = Kind.valueOf(tag.getString("kind"));
            return new Item(kind, tag.getInt("x"), tag.getInt("z"),
                    tag.getInt("x2"), tag.getInt("z2"), tag.getBoolean("done"));
        }
    }

    private final String id;
    private final String dimension;
    private final BlockPos center;
    private final List<Item> items;
    private boolean running;

    public CapitalPlan(String id, String dimension, BlockPos center, List<Item> items) {
        this.id = id;
        this.dimension = dimension;
        this.center = center;
        this.items = new ArrayList<>(items);
    }

    public String id() {
        return id;
    }

    public String dimension() {
        return dimension;
    }

    public BlockPos center() {
        return center;
    }

    public List<Item> items() {
        return items;
    }

    public boolean running() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long doneCount() {
        return items.stream().filter(Item::done).count();
    }

    public boolean complete() {
        return doneCount() == items.size();
    }

    /** Marks the first pending item done and reports whether one was found. */
    public boolean completeNext() {
        for (Item item : items) {
            if (!item.done()) {
                item.markDone();
                return true;
            }
        }
        return false;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("dimension", dimension);
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());
        tag.putBoolean("running", running);
        ListTag itemsTag = new ListTag();
        for (Item item : items) {
            itemsTag.add(item.write());
        }
        tag.put("items", itemsTag);
        return tag;
    }

    public static CapitalPlan read(CompoundTag tag) {
        String id = tag.getString("id");
        String dimension = tag.getString("dimension");
        BlockPos center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));
        List<Item> items = new ArrayList<>();
        ListTag itemsTag = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            items.add(Item.read(itemsTag.getCompound(i)));
        }
        CapitalPlan plan = new CapitalPlan(id, dimension, center, items);
        plan.setRunning(tag.getBoolean("running"));
        return plan;
    }
}
