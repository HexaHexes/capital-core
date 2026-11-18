package org.bitwisemadness.capitalcore.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * Optional Create integration (issue #9): when Create is installed, district
 * corners gain a brass machinery-casing accent. The block is looked up by
 * resource location at runtime, so Create is never a dependency - a soft
 * reference, gated on the mod being loaded.
 */
public final class CreateIntegration {

    private static final ResourceLocation BRASS_CASING = ResourceLocation.parse("create:brass_casing");

    private CreateIntegration() {
    }

    /** Whether Create is installed at runtime. */
    public static boolean isAvailable() {
        return ModList.get().isLoaded("create");
    }

    /** The district-corner accent block: Create brass casing when installed. */
    public static BlockState districtAccent() {
        if (isAvailable()) {
            Block block = BuiltInRegistries.BLOCK.get(BRASS_CASING);
            if (block != Blocks.AIR) {
                return block.defaultBlockState();
            }
        }
        return Blocks.POLISHED_ANDESITE.defaultBlockState();
    }
}