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
 * <p>Placement is unrotated in this milestone (rotated centering is the next
 * refinement); the interiors are designed orientation-agnostic. A missing
 * template falls back to the procedural spire so generation never stalls.</p>
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
        Optional<StructureTemplate> template = level.getStructureManager().get(id);
        if (template.isEmpty()) {
            CapitalCore.LOGGER.warn("district interior {} is missing - falling back to the procedural spire", id);
            return false;
        }
        StructureTemplate t = template.get();
        BlockPos origin = platformCenter.offset(-(t.getSize().getX() / 2), 1, -(t.getSize().getZ() / 2));
        t.placeInWorld(level, origin, origin,
                new StructurePlaceSettings().setRotation(Rotation.NONE).setIgnoreEntities(true),
                level.random, 3);
        return true;
    }
}
