package org.bitwisemadness.tensurafactionspawn.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalEntry;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalLocator;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalRegistry;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;

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
                .then(Commands.literal("capitals").executes(context -> listCapitals(context.getSource())))
                .then(Commands.literal("locate-all").executes(context -> locateAll(context.getSource())))
                .then(Commands.literal("locate").executes(context -> locate(context.getSource())))
                .then(Commands.literal("relocate").executes(context -> relocate(context.getSource()))));
    }

    private static int locateAll(CommandSourceStack source) {
        int queued = CapitalLocator.startLocating(source.getServer());
        if (queued == 0) {
            source.sendSuccess(() -> Component.literal(
                    "All registered capitals are already located (or no spawn rules are loaded)."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Locating " + queued + " capital tag(s) - one search per server tick, results accumulate in the"
                        + " registry. Query with /tensurafactionspawn capitals."), false);
        return queued;
    }

    private static int listCapitals(CommandSourceStack source) {
        var all = CapitalRegistry.get(source.getServer().overworld().getDataStorage()).all();
        if (all.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No capitals located yet. Run /tensurafactionspawn locate-all to start the search."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Located capitals (" + all.size() + "):"), false);
        for (CapitalEntry entry : all.values()) {
            source.sendSuccess(() -> Component.literal(" - " + entry.tagLocation()
                    + " at " + entry.position().toShortString()
                    + " in " + entry.dimension().location()), false);
        }
        if (CapitalLocator.isRunning()) {
            source.sendSuccess(() -> Component.literal("(a locate pass is still running)"), false);
        }
        return all.size();
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
                    + " dim=" + rule.dimension().map(key -> key.location().toString()).orElse("deferred")
                    + " respawn=" + rule.respawnMode().name().toLowerCase()), false);
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
        ServerLevel level = source.getServer().getLevel(
                FactionSpawnService.resolveDimension(player, race.get(), rule.get()));
        if (level == null) {
            source.sendFailure(Component.literal("Rule targets an unknown or missing dimension."));
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
