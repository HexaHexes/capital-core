package org.bitwisemadness.capitalcore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.bitwisemadness.capitalcore.gen.GenerationDriver;
import org.slf4j.Logger;

/**
 * The capital generation engine (SPIKE-CAPITAL-GEN-01, Option C hybrid).
 *
 * <p>Core owns the layout graph, roads, gates and terrain: it plans a capital
 * as a set of work items, persists the plan before construction, and builds it
 * across server ticks under a time budget, resuming after crashes. District
 * interiors are jigsaw's job (engine A), filled in a later milestone.</p>
 */
@Mod(CapitalCore.MOD_ID)
public final class CapitalCore {

    public static final String MOD_ID = "capital_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CapitalCore(IEventBus modBus) {
        GenerationDriver driver = new GenerationDriver();
        NeoForge.EVENT_BUS.addListener(driver::onServerTick);
        NeoForge.EVENT_BUS.addListener(new CapitalCoreCommands()::onRegisterCommands);
        LOGGER.info("Capital Core initialized");
    }
}
