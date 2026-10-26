package org.bitwisemadness.tensurafactionspawn.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;

import java.util.Optional;

/**
 * Operator command for inspecting and forcing faction-capital placement.
 *
 * <ul>
 *   <li>{@code /tensurafactionspawn rules} - list the loaded rules.</li>
 *   <li>{@code /tensurafactionspawn locate} - report where the caller's race
 *       would be sent, without moving them.</li>
 *   <li>{@code /tensurafactionspawn relocate} - force the move now.</li>
 * </ul>
 */
public final class FactionSpawnCommand {

    private FactionSpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tensurafactionspawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("rules").executes(context -> listRules(context.getSource())))
                .then(Commands.literal("locate").executes(context -> locate(context.getSource())))
                .then(Commands.literal("relocate").executes(context -> relocate(context.getSource()))));
    }

    private static int listRules(CommandSourceStack source) {
        var rules = SpawnRuleManager.rules();
        if (rules.isEmpty()) {
            source.sendFailure(Component.literal("No faction spawn rules are loaded."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Faction spawn rules (" + rules.size() + "):"), false);
        for (SpawnRule rule : rules) {
            source.sendSuccess(() -> Component.literal(" - " + rule.id()
                    + " race=#" + rule.race().location()
                    + " structures=#" + rule.structures().location()
                    + " dim=" + rule.dimension().location()), false);
        }
        return rules.size();
    }

    private static int locate(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<ManasRaceInstance> race = FactionSpawnService.currentRace(player);
        if (race.isEmpty()) {
            source.sendFailure(Component.literal("You have no Tensura race."));
            return 0;
        }
        Optional<SpawnRule> rule = SpawnRuleManager.select(race.get());
        if (rule.isEmpty()) {
            source.sendFailure(Component.literal("No faction spawn rule matches your race."));
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(rule.get().dimension());
        if (level == null) {
            source.sendFailure(Component.literal("Rule targets unknown dimension " + rule.get().dimension().location()));
            return 0;
        }
        Optional<BlockPos> target = FactionSpawnService.findNearest(level, rule.get(), level.getSharedSpawnPos());
        if (target.isEmpty()) {
            source.sendFailure(Component.literal("No capital found for " + rule.get().structures().location()));
            return 0;
        }
        BlockPos pos = target.get();
        source.sendSuccess(() -> Component.literal("Capital for " + race.get().getDisplayName().getString()
                + " -> " + pos.toShortString() + " in " + level.dimension().location()), false);
        return 1;
    }

    private static int relocate(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<BlockPos> moved = FactionSpawnService.relocate(player);
        if (moved.isEmpty()) {
            source.sendFailure(Component.literal("No faction capital to relocate to."));
            return 0;
        }
        BlockPos pos = moved.get();
        source.sendSuccess(() -> Component.literal("Relocated to " + pos.toShortString()), false);
        return 1;
    }
}
