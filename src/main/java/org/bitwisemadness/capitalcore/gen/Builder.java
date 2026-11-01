package org.bitwisemadness.capitalcore.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The construction primitives for the PoC: direct block setting under the
 * budget controller (the Option B technique, scoped to the planner's own
 * layout work - roads, gates and terrain; jigsaw owns district interiors
 * later). Each method places a small, atomic batch so a budget cut between
 * items never leaves a half-built district.
 */
public final class Builder {

    private Builder() {
    }

    /** Builds one district: a levelled 16x16 platform, a perimeter wall, a marker spire. */
    public static void buildDistrict(ServerLevel level, int cx, int cz) {
        int ground = groundY(level, cx, cz);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                level.setBlock(new BlockPos(cx + dx, ground, cz + dz),
                        Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
            }
        }
        for (int i = -8; i <= 8; i++) {
            wall(level, cx + i, ground, cz - 8);
            wall(level, cx + i, ground, cz + 8);
            wall(level, cx - 8, ground, cz + i);
            wall(level, cx + 8, ground, cz + i);
        }
        BlockPos spire = new BlockPos(cx, ground + 1, cz);
        for (int dy = 0; dy < 6; dy++) {
            level.setBlock(spire.offset(0, dy, 0), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(spire.offset(0, 6, 0), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
    }

    /** Builds one road segment: a 3-wide cobblestone path between two centers. */
    public static void buildRoad(ServerLevel level, int x1, int z1, int x2, int z2) {
        int y = groundY(level, x1, z1);
        int dx = Integer.signum(x2 - x1);
        int dz = Integer.signum(z2 - z1);
        int x = x1;
        int z = z1;
        while (x != x2 || z != z2) {
            for (int w = -1; w <= 1; w++) {
                int px = dx != 0 ? x : x + w;
                int pz = dz != 0 ? z : z + w;
                level.setBlock(new BlockPos(px, y, pz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
            x += dx;
            z += dz;
        }
        for (int w = -1; w <= 1; w++) {
            int px = dx != 0 ? x2 : x2 + w;
            int pz = dz != 0 ? z2 : z2 + w;
            level.setBlock(new BlockPos(px, y, pz), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
    }

    private static int groundY(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    private static void wall(ServerLevel level, int x, int ground, int z) {
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(x, ground + dy, z), Blocks.ANDESITE.defaultBlockState(), 3);
        }
    }
}
