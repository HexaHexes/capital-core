package org.bitwisemadness.capitalcore;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.bitwisemadness.capitalcore.gen.Planner;
import org.bitwisemadness.capitalcore.plan.CapitalPlan;
import org.bitwisemadness.capitalcore.plan.CapitalPlanSavedData;

/**
 * The {@code /capitalgen} operator surface: plan, run and inspect generation.
 * {@code run} only flips the persisted running flag - the budget controller
 * takes over on the next tick and resumes after crashes on its own.
 */
public final class CapitalCoreCommands {

    /** Registers the commands on the mod bus. */
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("capitalgen")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("plan")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(this::plan)))
                .then(Commands.literal("run")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(this::run)))
                .then(Commands.literal("status").executes(this::status));
        event.getDispatcher().register(root);
    }

    private int plan(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id");
        CommandSourceStack source = ctx.getSource();
        BlockPos center = BlockPos.containing(source.getPosition());
        CapitalPlanSavedData data = CapitalPlanSavedData.get(source.getServer());
        if (data.get(id) != null) {
            source.sendFailure(Component.literal("plan '" + id + "' already exists"));
            return Command.SINGLE_SUCCESS;
        }
        CapitalPlan plan = Planner.plan(id, source.getLevel().dimension().location().toString(),
                source.getLevel(), center);
        data.put(plan);
        source.sendSuccess(() -> Component.literal(
                "planned '" + id + "': " + plan.items().size() + " work items at " + center.toShortString()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id");
        CommandSourceStack source = ctx.getSource();
        CapitalPlanSavedData data = CapitalPlanSavedData.get(source.getServer());
        CapitalPlan plan = data.get(id);
        if (plan == null) {
            source.sendFailure(Component.literal("no plan '" + id + "' - run /capitalgen plan first"));
            return Command.SINGLE_SUCCESS;
        }
        plan.setRunning(true);
        data.planChanged();
        source.sendSuccess(() -> Component.literal("generation running for '" + id + "' ("
                + plan.doneCount() + "/" + plan.items().size() + " already built)"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int status(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CapitalPlanSavedData data = CapitalPlanSavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal("capital generation plans: "
                + data.allPlans().size()), false);
        for (CapitalPlan plan : data.allPlans()) {
            source.sendSuccess(() -> Component.literal("  '" + plan.id() + "' "
                    + plan.doneCount() + "/" + plan.items().size()
                    + (plan.running() ? " [running]" : (plan.complete() ? " [complete]" : " [paused]"))),
                    false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
