package net.onemoreexpedition.skyblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import net.onemoreexpedition.skyblock.config.SkyblockConfig;
import net.onemoreexpedition.skyblock.data.PlayerReturnLocation;
import net.onemoreexpedition.skyblock.data.SkyblockDataManager;
import net.onemoreexpedition.skyblock.data.SkyblockIsland;
import net.onemoreexpedition.skyblock.island.IslandMath;
import net.onemoreexpedition.skyblock.listener.SkyblockEventListener;

import java.util.Collections;
import java.util.Optional;

public final class SkyblockCommands {
    private SkyblockCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(SkyblockCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext buildContext,
                                         Commands.CommandSelection environment) {
        // /is root command
        dispatcher.register(Commands.literal("is")
                .executes(SkyblockCommands::teleportHome)
                .then(Commands.literal("home").executes(SkyblockCommands::teleportHome))
                .then(Commands.literal("return").executes(SkyblockCommands::executeReturn))
                .then(Commands.literal("sethome").executes(SkyblockCommands::setHome))
                .then(Commands.literal("lock").executes(SkyblockCommands::lockIsland))
                .then(Commands.literal("unlock").executes(SkyblockCommands::unlockIsland))
                .then(Commands.literal("pvpcooldown")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(SkyblockCommands::setPvpCooldown)))
                .then(Commands.literal("kick")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                        builder.suggest(p.getGameProfile().name());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(SkyblockCommands::kickPlayer)))
        );

        // /return standalone command
        dispatcher.register(Commands.literal("return")
                .executes(SkyblockCommands::executeReturn)
        );

        // /visit <player>
        dispatcher.register(Commands.literal("visit")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            SkyblockDataManager dm = SkyblockDataManager.getInstance();
                            if (dm != null) {
                                for (SkyblockIsland island : dm.getAllIslands()) {
                                    if (island.getOwnerName() != null) {
                                        builder.suggest(island.getOwnerName());
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(SkyblockCommands::visitPlayer))
        );

        // /invite <player>
        dispatcher.register(Commands.literal("invite")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                builder.suggest(p.getGameProfile().name());
                            }
                            return builder.buildFuture();
                        })
                        .executes(SkyblockCommands::invitePlayer))
        );
    }

    private static int teleportHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        MinecraftServer server = context.getSource().getServer();

        boolean inSkyblock = player.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION);

        if (!inSkyblock) {
            // Check PvP combat cooldown (default 60s)
            int pvpRemaining = SkyblockEventListener.getRemainingPvpSeconds(player.getUUID());
            if (pvpRemaining > 0) {
                player.sendSystemMessage(Component.literal(String.format("§cYou cannot teleport while in PvP combat! ⚔ %ds remaining.", pvpRemaining)));
                return 0;
            }

            // Check mob/environmental damage cooldown
            long lastDamage = SkyblockEventListener.getLastDamageTimestamp(player.getUUID());
            long now = System.currentTimeMillis();
            long mobCooldownMillis = SkyblockConfig.get().mobDamageCooldownSeconds * 1000L;
            if (now - lastDamage < mobCooldownMillis) {
                double remaining = (mobCooldownMillis - (now - lastDamage)) / 1000.0;
                player.sendSystemMessage(Component.literal(String.format("§cYou cannot teleport while in combat! Cooldown: %.1fs remaining.", remaining)));
                return 0;
            }

            // Check falling status
            if (player.fallDistance > 1.5f || (!player.onGround() && player.getDeltaMovement().y < -0.15)) {
                player.sendSystemMessage(Component.literal("§cYou cannot teleport while falling!"));
                return 0;
            }

            // Save return location from main world
            dataManager.saveReturnLocation(player);
        }

        ServerLevel skyblockLevel = server.getLevel(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION);
        if (skyblockLevel == null) {
            player.sendSystemMessage(Component.literal("§cSkyblock dimension is not currently loaded!"));
            return 0;
        }

        SkyblockIsland island = dataManager.getOrCreateIsland(player);

        player.setDeltaMovement(0, 0, 0);
        player.resetFallDistance();
        player.teleportTo(
                skyblockLevel,
                island.getHomeX(),
                island.getHomeY(),
                island.getHomeZ(),
                Collections.emptySet(),
                island.getHomeYaw(),
                island.getHomePitch(),
                true
        );

        player.sendSystemMessage(Component.literal("§aTeleported to your island home."));
        return 1;
    }

    private static int executeReturn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();

        if (!player.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            player.sendSystemMessage(Component.literal("§cYou are already in the main world!"));
            return 0;
        }

        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        PlayerReturnLocation loc = dataManager.getReturnLocation(player.getUUID());

        ServerLevel targetLevel = null;
        double targetX, targetY, targetZ;
        float targetYaw = 0f, targetPitch = 0f;

        if (loc != null && loc.getDimension() != null) {
            try {
                Identifier dimId = Identifier.parse(loc.getDimension());
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
                targetLevel = server.getLevel(dimKey);
            } catch (Exception ignored) {}

            targetX = loc.getX();
            targetY = loc.getY();
            targetZ = loc.getZ();
            targetYaw = loc.getYaw();
            targetPitch = loc.getPitch();
        } else {
            targetX = 0;
            targetY = 64;
            targetZ = 0;
        }

        if (targetLevel == null) {
            targetLevel = server.overworld();
            BlockPos spawn = targetLevel.getRespawnData().pos();
            targetX = spawn.getX() + 0.5;
            targetY = spawn.getY();
            targetZ = spawn.getZ() + 0.5;
        }

        player.setDeltaMovement(0, 0, 0);
        player.resetFallDistance();
        player.teleportTo(
                targetLevel,
                targetX,
                targetY,
                targetZ,
                Collections.emptySet(),
                targetYaw,
                targetPitch,
                true
        );

        player.sendSystemMessage(Component.literal("§aReturned to your previous location."));
        return 1;
    }

    private static int setHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (!player.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            player.sendSystemMessage(Component.literal("§cYou can only set your island home inside the Skyblock dimension!"));
            return 0;
        }

        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        SkyblockIsland island = dataManager.getIslandByOwner(player.getUUID());

        if (island == null) {
            player.sendSystemMessage(Component.literal("§cYou do not have a Skyblock island yet! Run /is first."));
            return 0;
        }

        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();

        if (!island.isWithinBounds(blockX, blockZ)) {
            player.sendSystemMessage(Component.literal("§cYou can only set your home within your 256x256 island bounds!"));
            return 0;
        }

        island.setHomeX(player.getX());
        island.setHomeY(player.getY());
        island.setHomeZ(player.getZ());
        island.setHomeYaw(player.getYRot());
        island.setHomePitch(player.getXRot());
        dataManager.save();

        player.sendSystemMessage(Component.literal("§aIsland home location updated."));
        return 1;
    }

    private static int visitPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer visitor = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");
        MinecraftServer server = context.getSource().getServer();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();

        // Find target island by online player or offline name
        ServerPlayer targetOnline = server.getPlayerList().getPlayerByName(targetName);
        SkyblockIsland targetIsland = null;

        if (targetOnline != null) {
            targetIsland = dataManager.getIslandByOwner(targetOnline.getUUID());
        }
        if (targetIsland == null) {
            Optional<SkyblockIsland> opt = dataManager.findIslandByOwnerName(targetName);
            if (opt.isPresent()) {
                targetIsland = opt.get();
            }
        }

        if (targetIsland == null) {
            visitor.sendSystemMessage(Component.literal("§cPlayer §e" + targetName + " §chas no Skyblock island!"));
            return 0;
        }

        // Check if visitor is owner
        boolean isOwner = targetIsland.isOwner(visitor.getUUID());
        boolean isOp = server.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(visitor.getGameProfile()));

        if (!isOwner && !isOp && targetIsland.isLocked()) {
            if (!targetIsland.isInvited(visitor.getUUID())) {
                visitor.sendSystemMessage(Component.literal("§cThat player's island is locked (invite-only). You need an invitation."));
                return 0;
            }
        }

        if (!visitor.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            dataManager.saveReturnLocation(visitor);
        }

        ServerLevel skyblockLevel = server.getLevel(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION);
        if (skyblockLevel == null) {
            visitor.sendSystemMessage(Component.literal("§cSkyblock dimension is not currently loaded!"));
            return 0;
        }

        visitor.setDeltaMovement(0, 0, 0);
        visitor.resetFallDistance();
        visitor.teleportTo(
                skyblockLevel,
                targetIsland.getHomeX(),
                targetIsland.getHomeY(),
                targetIsland.getHomeZ(),
                Collections.emptySet(),
                targetIsland.getHomeYaw(),
                targetIsland.getHomePitch(),
                true
        );

        visitor.sendSystemMessage(Component.literal("§aVisiting §e" + targetIsland.getOwnerName() + "§a's island."));
        if (targetOnline != null && !isOwner) {
            targetOnline.sendSystemMessage(Component.literal("§e" + visitor.getGameProfile().name() + " §ais now visiting your island."));
        }

        return 1;
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");
        MinecraftServer server = context.getSource().getServer();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();

        SkyblockIsland island = dataManager.getIslandByOwner(owner.getUUID());
        if (island == null) {
            owner.sendSystemMessage(Component.literal("§cYou do not have a Skyblock island yet! Run /is first."));
            return 0;
        }

        ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetName);
        if (targetPlayer == null) {
            owner.sendSystemMessage(Component.literal("§cPlayer §e" + targetName + " §cis not online!"));
            return 0;
        }

        if (targetPlayer.getUUID().equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.literal("§cYou cannot invite yourself!"));
            return 0;
        }

        island.invite(targetPlayer.getUUID());
        dataManager.save();

        owner.sendSystemMessage(Component.literal("§aInvited §e" + targetPlayer.getGameProfile().name() + " §ato your island."));
        targetPlayer.sendSystemMessage(Component.literal("§a" + owner.getGameProfile().name() + " invited you to their island! Run §e/visit " + owner.getGameProfile().name() + " §ato visit."));
        return 1;
    }

    private static int lockIsland(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        SkyblockIsland island = dataManager.getIslandByOwner(owner.getUUID());

        if (island == null) {
            owner.sendSystemMessage(Component.literal("§cYou do not have a Skyblock island yet! Run /is first."));
            return 0;
        }

        island.setLocked(true);
        dataManager.save();
        owner.sendSystemMessage(Component.literal("§aYour island is now §clocked §a(invite-only)."));
        return 1;
    }

    private static int unlockIsland(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        SkyblockIsland island = dataManager.getIslandByOwner(owner.getUUID());

        if (island == null) {
            owner.sendSystemMessage(Component.literal("§cYou do not have a Skyblock island yet! Run /is first."));
            return 0;
        }

        island.setLocked(false);
        dataManager.save();
        owner.sendSystemMessage(Component.literal("§aYour island is now §2unlocked §a(public)."));
        return 1;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");
        MinecraftServer server = context.getSource().getServer();
        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();

        SkyblockIsland island = dataManager.getIslandByOwner(owner.getUUID());
        if (island == null) {
            owner.sendSystemMessage(Component.literal("§cYou do not have a Skyblock island yet!"));
            return 0;
        }

        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            owner.sendSystemMessage(Component.literal("§cPlayer §e" + targetName + " §cis not online!"));
            return 0;
        }

        if (target.getUUID().equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.literal("§cYou cannot kick yourself!"));
            return 0;
        }

        boolean onIsland = target.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)
                && island.isWithinBounds(target.getBlockX(), target.getBlockZ());

        if (!onIsland) {
            owner.sendSystemMessage(Component.literal("§cPlayer §e" + targetName + " §cis not currently on your island!"));
            return 0;
        }

        island.uninvite(target.getUUID());
        dataManager.save();

        // Evict target
        SkyblockIsland targetOwnIsland = dataManager.getIslandByOwner(target.getUUID());
        ServerLevel skyblockLevel = server.getLevel(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION);

        if (targetOwnIsland != null && skyblockLevel != null) {
            target.setDeltaMovement(0, 0, 0);
            target.resetFallDistance();
            target.teleportTo(
                    skyblockLevel,
                    targetOwnIsland.getHomeX(),
                    targetOwnIsland.getHomeY(),
                    targetOwnIsland.getHomeZ(),
                    Collections.emptySet(),
                    targetOwnIsland.getHomeYaw(),
                    targetOwnIsland.getHomePitch(),
                    true
            );
        } else {
            PlayerReturnLocation returnLoc = dataManager.getReturnLocation(target.getUUID());
            ServerLevel targetLevel = null;
            double tx, ty, tz;
            if (returnLoc != null && returnLoc.getDimension() != null) {
                try {
                    Identifier id = Identifier.parse(returnLoc.getDimension());
                    targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                } catch (Exception ignored) {}
                tx = returnLoc.getX();
                ty = returnLoc.getY();
                tz = returnLoc.getZ();
            } else {
                tx = 0; ty = 64; tz = 0;
            }
            if (targetLevel == null) {
                targetLevel = server.overworld();
                BlockPos p = targetLevel.getRespawnData().pos();
                tx = p.getX() + 0.5;
                ty = p.getY();
                tz = p.getZ() + 0.5;
            }
            target.setDeltaMovement(0, 0, 0);
            target.resetFallDistance();
            target.teleportTo(targetLevel, tx, ty, tz, Collections.emptySet(), 0f, 0f, true);
        }

        owner.sendSystemMessage(Component.literal("§aKicked §e" + target.getGameProfile().name() + " §afrom your island."));
        target.sendSystemMessage(Component.literal("§cYou were kicked from §e" + owner.getGameProfile().name() + "§c's island."));
        return 1;
    }

    private static int setPvpCooldown(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        SkyblockConfig.get().pvpCombatCooldownSeconds = seconds;
        SkyblockConfig.save();
        context.getSource().sendSuccess(() -> Component.literal(String.format("§a[Skyblock] PvP combat cooldown set to §e%d seconds§a.", seconds)), true);
        return seconds;
    }
}
