package io.github.jackmacca06.ggtlifesteal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class GgtCommands {
    private GgtCommands() {}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(Commands.literal("ggtls")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal(
                                    "/ggtls withdraw_heart <count> | claim_hearts | status; OP: set_hearts <player> <count> | release_end | release_nether | release_enchants | start_fury_event | limit_playtime <hours>"), false);
                            return 1;
                        })
                        .then(Commands.literal("withdraw_heart")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(c -> withdraw(c.getSource(), IntegerArgumentType.getInteger(c, "count")))))
                        .then(Commands.literal("claim_hearts").executes(c -> claim(c.getSource())))
                        .then(Commands.literal("status").executes(c -> status(c.getSource())))
                        .then(Commands.literal("set_hearts").requires(GgtCommands::operator)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(HeartData.MIN_HEARTS, HeartData.MAX_HEARTS))
                                                .executes(c -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(c, "player");
                                                    int count = IntegerArgumentType.getInteger(c, "count");
                                                    HeartData.setHearts(player, count);
                                                    c.getSource().sendSuccess(() -> Component.literal("Set ")
                                                            .append(player.getDisplayName())
                                                            .append("'s heart capacity to " + count + "."), true);
                                                    player.sendSystemMessage(Component.literal("Your heart capacity was set to " + count + "."));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("start_fury_event").requires(GgtCommands::operator)
                                .executes(c -> {
                                    boolean started = FuryVaultRules.start(c.getSource().getServer(), true);
                                    if (!started) return fail(c.getSource(), "No event started: an event is active, the queue is empty, or no safe spawn location exists.");
                                    c.getSource().sendSuccess(() -> Component.literal("Dragon's Fury vault placed. The 10-minute countdown has begun."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("release_enchants").requires(GgtCommands::operator)
                                .executes(c -> {
                                    boolean open = ModState.enchantsOpen(c.getSource().getServer());
                                    ModState.releaseEnchants(c.getSource().getServer());
                                    c.getSource().sendSuccess(() -> Component.translatable(open
                                            ? "message.ggtlifesteal.enchants_already_open"
                                            : "message.ggtlifesteal.enchants_released"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("release_nether").requires(GgtCommands::operator)
                                .executes(c -> {
                                    boolean open = ModState.netherOpen(c.getSource().getServer());
                                    ModState.releaseNether(c.getSource().getServer());
                                    c.getSource().sendSuccess(() -> Component.translatable(open
                                            ? "message.ggtlifesteal.nether_already_open"
                                            : "message.ggtlifesteal.nether_released"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("release_end").requires(GgtCommands::operator)
                                .executes(c -> {
                                    boolean alreadyOpen = ModState.endOpen(c.getSource().getServer());
                                    ModState.releaseEnd(c.getSource().getServer());
                                    c.getSource().sendSuccess(() -> Component.literal(
                                            alreadyOpen ? "The End is already open." : "The End is now open."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("limit_playtime").requires(GgtCommands::operator)
                                .then(Commands.argument("duration_hrs", IntegerArgumentType.integer(0))
                                        .executes(c -> {
                                            int hours = IntegerArgumentType.getInteger(c, "duration_hrs");
                                            ModState.dailyHours(c.getSource().getServer(), hours);
                                            c.getSource().sendSuccess(() -> Component.literal(hours == 0
                                                    ? "Daily playtime is unlimited."
                                                    : "Daily playtime limited to " + hours + " hour(s), resetting at midnight ACST."), true);
                                            return 1;
                                        })))));
    }
    private static boolean operator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
    private static int withdraw(CommandSourceStack source, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isAlive() || player.isSpectator()) return fail(source, "You cannot withdraw Hearts right now.");
        if (CombatRules.active(player)) return fail(source, "You cannot withdraw Hearts during combat.");
        if (count > HeartData.withdrawableHearts(player)) return fail(source,
                "You can withdraw " + HeartData.withdrawableHearts(player) + " Heart(s). Withdrawals must leave at least 10 hearts; Makeshift-restored hearts cannot be withdrawn.");
        if (HeartRewards.room(player) < count) return fail(source, "Make enough inventory space before withdrawing Hearts.");
        if (!HeartData.withdraw(player, count)) return fail(source, "No Hearts were withdrawn.");
        int given = HeartRewards.give(player, count);
        // Single server-thread transaction; retain any unexpected insertion remainder rather than lose it.
        if (given < count) ModState.pending(source.getServer(), player.getUUID(),
                ModState.pending(source.getServer(), player.getUUID()) + count - given);
        source.sendSuccess(() -> Component.literal("Withdrew " + count + " Heart(s)."), false);
        return count;
    }
    private static int claim(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isAlive() || player.isSpectator()) return fail(source, "You cannot claim Hearts right now.");
        int pending = ModState.pending(source.getServer(), player.getUUID());
        if (pending == 0) return fail(source, "You have no stored Hearts.");
        if (HeartRewards.room(player) == 0) return fail(source, "Your inventory is full. Make room and try again.");
        int given = HeartRewards.give(player, pending);
        ModState.pending(source.getServer(), player.getUUID(), pending - given);
        source.sendSuccess(() -> Component.literal("Claimed " + given + " Heart(s); " + (pending - given) + " still stored."), false);
        return given;
    }
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long remaining = PlaytimeRules.remainingMillis(player);
        String time = remaining == Long.MAX_VALUE ? "unlimited" : ((remaining + 59_999) / 60_000) + " minute(s)";
        source.sendSuccess(() -> Component.literal("Hearts: " + HeartData.getHearts(player) + "/20 | Stored: "
                + ModState.pending(source.getServer(), player.getUUID()) + " | Playtime left: " + time
                + " | Combat: " + CombatRules.secondsLeft(player) + "s"), false);
        return 1;
    }
}
