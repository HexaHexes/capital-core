package org.bitwisemadness.capitalcore.gen;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Rotation;
import org.bitwisemadness.capitalcore.CapitalCore;

/**
 * Drops an authored district interior onto the planner's platform (Option C:
 * authored pieces fill the interiors the planner lays out). The three
 * interiors ship in this mod's datapack and are selected by the district's
 * stable ordinal - identical across crash-resumes.
 *
 * <p>The ordinal also drives the rotation (none / cw90 / 180 / ccw90, cycling
 * by ordinal), so adjacent districts never repeat the same orientation; the
 * placement offset compensates for the rotated bounding box so every interior
 * stays centered on its platform. A missing template falls back to the
 * procedural spire so generation never stalls.</p>
 */
public final class TemplateDistricts {

    private static final ResourceLocation[] INTERIORS = {
            ResourceLocation.parse("capital_core:district/hall"),
            ResourceLocation.parse("capital_core:district/market"),
            ResourceLocation.parse("capital_core:district/garden"),
    };

    private TemplateDistricts() {
    }

    /** Places the interior for the district ordinal; returns false when it fell back. */
    public static boolean placeInterior(ServerLevel level, int ordinal, BlockPos platformCenter) {
        ResourceLocation id = INTERIORS[Math.floorMod(ordinal, INTERIORS.length)];
        Rotation rotation = Rotation.values()[Math.floorMod(ordinal, Rotation.values().length)];
        Optional<StructureTemplate> template = level.getStructureManager().get(id);
        if (template.isEmpty()) {
            CapitalCore.LOGGER.warn("district interior {} is missing - falling back to the procedural spire", id);
            return false;
        }
        StructureTemplate t = template.get();
        BlockPos origin = centeredOrigin(platformCenter, t.getSize(), rotation);
        t.placeInWorld(level, origin, origin,
                new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true),
                level.random, 3);
        return true;
    }

    /**
     * The placement origin that centers the rotated template on the platform:
     * the rotation moves the template's bounding box relative to the origin,
     * so the origin shifts against the rotated extent.
     */
    public static BlockPos centeredOrigin(BlockPos center, net.minecraft.core.Vec3i size, Rotation rotation) {
        int sx = size.getX();
        int sz = size.getZ();
        int ox;
        int oz;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                ox = (sz - 1) / 2;
                oz = -(sx - 1) / 2;
            }
            case CLOCKWISE_180 -> {
                ox = (sx - 1) / 2;
                oz = (sz - 1) / 2;
            }
            case COUNTERCLOCKWISE_90 -> {
                ox = -(sz - 1) / 2;
                oz = (sx - 1) / 2;
            }
            default -> {
                ox = -(sx - 1) / 2;
                oz = -(sz - 1) / 2;
            }
        }
        return center.offset(ox, 1, oz);
    }
}
