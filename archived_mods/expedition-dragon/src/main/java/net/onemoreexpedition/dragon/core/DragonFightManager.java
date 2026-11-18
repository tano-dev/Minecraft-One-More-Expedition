package net.onemoreexpedition.dragon.core;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import net.onemoreexpedition.dragon.ExpeditionDragonMod;
import net.onemoreexpedition.dragon.altar.AltarManager;
import net.onemoreexpedition.dragon.altar.EnderDragonFightBridge;
import net.onemoreexpedition.dragon.altar.StellarityPillars;
import net.onemoreexpedition.dragon.combat.DragonShield;
import net.onemoreexpedition.dragon.combat.MovesetController;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.rewards.DragonRewardManager;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DragonFightManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("expedition-dragon");
    private static final ConcurrentHashMap<MinecraftServer, DragonFightManager> MANAGERS = new ConcurrentHashMap<>();
    private static final String ADV_KILL_DRAGON = "minecraft:end/kill_dragon";
    private static final String ADV_GOD_SLAYER = "stellarity:dragons_den/god_slayer";
    private static final String FN_PORTAL_ACTIVATE = "stellarity:structure/exit_portal/activated/generate";
    private static final String FN_GATEWAY_CHOOSE = "stellarity:structure/exit_portal/gateway/choose_location";
    private static final String FN_ALTAR_SOUND = "stellarity:entity/dragon/death/activate_altar_of_the_accursed_sound";
    private static final String FN_ALTAR_ACTIVATE = "stellarity:entity/dragon/death/activate_altar_of_the_accursed";

    private final MinecraftServer server;
    private final MovesetController movesetController = new MovesetController();
    private volatile FightState activeFight;
    private volatile VariantBehavior activeBehavior;
    private volatile MinionHooks minionHooks = MinionHooks.NO_OP;
    private volatile ServerBossEvent bossBar;
    private volatile ResourceKey<Level> fightDimension = Level.END;
    private volatile UUID dyingDragonUuid;
    private volatile boolean deathBurstPlayed;
    private int pendingAltarSoundTicks = -1;
    private int pendingAltarActivateTicks = -1;
    private ResourceKey<Level> pendingAltarDimension = Level.END;

    private DragonFightManager(MinecraftServer server) {
        this.server = server;
    }

    public static DragonFightManager get(MinecraftServer server) {
        return MANAGERS.computeIfAbsent(server, DragonFightManager::new);
    }

    public static void remove(MinecraftServer server) {
        if (server != null) {
            MANAGERS.remove(server);
        }
    }

    public void setMinionHooks(MinionHooks minionHooks) {
        this.minionHooks = minionHooks != null ? minionHooks : MinionHooks.NO_OP;
    }

    public FightState getActiveFight() {
        return activeFight;
    }

    public VariantBehavior getActiveBehavior() {
        return activeBehavior;
    }

    public ServerBossEvent getBossBar() {
        return bossBar;
    }

    public MovesetController getMovesetController() {
        return movesetController;
    }

    public boolean isCustomDragon(EnderDragon dragon) {
        FightState state = this.activeFight;
        return state != null && !state.isEnded() && dragon != null && dragon.getUUID().equals(state.getDragonUuid());
    }

    public boolean isCustomOrDyingDragon(EnderDragon dragon) {
        if (dragon == null) {
            return false;
        }
        return isCustomDragon(dragon) || dragon.getUUID().equals(dyingDragonUuid);
    }

    public boolean isDyingInProgress() {
        return this.dyingDragonUuid != null;
    }

    public synchronized FightState startCustomFight(ServerLevel endLevel, DragonVariant variant, boolean fromAltar) {
        if (endLevel == null) {
            return null;
        }
        DragonVariant resolvedVariant = variant != null ? variant : DragonVariant.INFERNAL;
        if (activeFight != null && !activeFight.isEnded()) {
            ServerLevel prevLevel = server.getLevel(fightDimension);
            onFightEnded(prevLevel != null ? prevLevel : endLevel, false);
        }

        ExpeditionDragonMod.widenMaxHealthLimit();

        DragonConfig config = DragonConfig.get();
        DragonConfig.VariantConfig variantConfig = config.getVariantConfig(resolvedVariant);

        boolean forcedChunk = false;
        boolean committed = false;
        EnderDragon dragon = null;
        try {
            forcedChunk = endLevel.setChunkForced(0, 0, true);
            endLevel.getChunkAt(new BlockPos(0, 128, 0));
            dragon = EntityTypes.ENDER_DRAGON.create(
                    endLevel,
                    fromAltar ? EntitySpawnReason.EVENT : EntitySpawnReason.COMMAND
            );
            if (dragon == null) {
                LOGGER.error("Failed to create EnderDragon entity for variant {}", resolvedVariant.id());
                return null;
            }

            EnderDragonFight vanillaFight = endLevel.getDragonFight();
            if (fromAltar && vanillaFight != null) {
                dragon.setDragonFight(vanillaFight);
            } else {
                dragon.setDragonFight(null);
            }
            dragon.setFightOrigin(BlockPos.ZERO);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            float yaw = endLevel.getRandom().nextFloat() * 360.0F;
            dragon.snapTo(0.5D, 128.0D, 0.5D, yaw, 0.0F);

            double rawBaseHp = variantConfig.baseHp;
            if (!Double.isFinite(rawBaseHp) || rawBaseHp <= 0.0D) {
                rawBaseHp = resolvedVariant.defaultBaseHp();
            }
            double maxHp = Math.min(ExpeditionDragonMod.WIDENED_MAX_HEALTH, Math.max(1.0D, rawBaseHp));
            AttributeInstance maxHealthAttr = dragon.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(maxHp);
            }
            dragon.setHealth((float) maxHp);

            AttributeInstance knockbackAttr = dragon.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                knockbackAttr.setBaseValue(1.0D);
            }

            if (!endLevel.addFreshEntity(dragon)) {
                LOGGER.error("Failed to add EnderDragon entity to level {}", endLevel.dimension());
                return null;
            }

            if (fromAltar && vanillaFight instanceof EnderDragonFightBridge bridge) {
                bridge.expeditionDragon$bindCustomDragon(dragon.getUUID());
            }

            double effectiveMaxHp = dragon.getMaxHealth();
            FightState state = new FightState(dragon.getUUID(), resolvedVariant, fromAltar, effectiveMaxHp);
            state.setChunkForcedByFight(forcedChunk);

            BossEvent.BossBarColor barColor = DragonVariant.parseBossBarColor(
                    variantConfig.bossBarColor,
                    resolvedVariant.defaultBossBarColor()
            );
            ServerBossEvent bar = new ServerBossEvent(
                    UUID.randomUUID(),
                    Component.literal(variantConfig.bossBarTitle),
                    barColor,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            bar.setProgress(1.0F);
            bar.setVisible(true);
            for (ServerPlayer player : endLevel.players()) {
                bar.addPlayer(player);
            }

            VariantBehavior behavior = VariantBehavior.create(resolvedVariant);
            this.dyingDragonUuid = null;
            this.deathBurstPlayed = false;
            this.activeFight = state;
            this.activeBehavior = behavior;
            this.bossBar = bar;
            this.fightDimension = endLevel.dimension();
            committed = true;

            behavior.onFightStart(state, endLevel, dragon);
            dispatchMinionFightStart(state, endLevel, dragon);
            applyVariantTeam(dragon, resolvedVariant, behavior.getOutlineColor());

            setupPillarAnchors(endLevel, state, behavior, fromAltar);
            movesetController.onFightStart(endLevel, dragon, resolvedVariant);

            LOGGER.info("Started {} dragon fight (UUID={}, maxHp={}, fromAltar={}, anchors={})",
                    resolvedVariant.id(), dragon.getUUID(), effectiveMaxHp, fromAltar, state.getActiveAnchorCount());
            return state;
        } finally {
            if (!committed) {
                if (dragon != null && !dragon.isRemoved()) {
                    dragon.discard();
                }
                if (forcedChunk) {
                    try {
                        endLevel.setChunkForced(0, 0, false);
                    } catch (RuntimeException e) {
                        LOGGER.warn("Failed to release forced chunk (0,0) after aborted fight start", e);
                    }
                }
            }
        }
    }

    private void setupPillarAnchors(ServerLevel endLevel, FightState state, VariantBehavior behavior, boolean fromAltar) {
        if (behavior.hasCustomShieldAnchors()) {
            StellarityPillars.clearPillarCrystals(endLevel);
            return;
        }

        if (fromAltar) {
            // Do not spawn crystals immediately; they will be spawned by the Altar ritual animation.
            // But we must mark that anchors are expected so the shield stays active.
            state.markAnchorsExpected(StellarityPillars.PILLARS.size());
            return;
        }

        List<EndCrystal> spawned = StellarityPillars.spawnStellarityPillarCrystals(endLevel, true);
        for (EndCrystal crystal : spawned) {
            state.addAnchor(crystal.getUUID());
            dispatchCrystalSpawned(state, endLevel, crystal);
        }
    }

    private void applyVariantTeam(EnderDragon dragon, DragonVariant variant, net.minecraft.ChatFormatting color) {
        Scoreboard sb = server.getScoreboard();
        String teamName = "dragon_" + variant.id();
        PlayerTeam team = sb.getPlayerTeam(teamName);
        if (team == null) {
            team = sb.addPlayerTeam(teamName);
            team.setColor(Optional.of(mapFormattingToTeamColor(color)));
        }
        sb.addPlayerToTeam(dragon.getScoreboardName(), team);
        dragon.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
    }

    private net.minecraft.world.scores.TeamColor mapFormattingToTeamColor(net.minecraft.ChatFormatting format) {
        return switch (format) {
            case RED -> TeamColor.RED;
            case LIGHT_PURPLE -> TeamColor.LIGHT_PURPLE;
            case GREEN -> TeamColor.GREEN;
            case YELLOW -> TeamColor.YELLOW;
            case BLUE -> TeamColor.BLUE;
            default -> TeamColor.WHITE;
        };
    }

    private void cleanupVariantTeam(EnderDragon dragon) {
        Scoreboard sb = server.getScoreboard();
        sb.removePlayerFromTeam(dragon.getScoreboardName());
        dragon.removeEffect(MobEffects.GLOWING);
    }

    public void onServerTick() {
        tickPendingAltarActivation();

        FightState state = this.activeFight;
        if (state == null || state.isEnded()) {
            UUID dyingUuid = this.dyingDragonUuid;
            ServerLevel endLevel = server.getLevel(fightDimension);
            if (dyingUuid != null && endLevel != null) {
                Entity dying = endLevel.getEntity(dyingUuid);
                if (!(dying instanceof EnderDragon) || dying.isRemoved()) {
                    this.dyingDragonUuid = null;
                }
            }
            if (this.dyingDragonUuid == null && endLevel != null && !endLevel.players().isEmpty()
                    && server.getTickCount() % 40 == 0) {
                for (EnderDragon stray : endLevel.getDragons()) {
                    if (!stray.isRemoved()) {
                        stray.discard();
                    }
                }
                cleanupEndPortalArtifacts(endLevel);
            }
            return;
        }
        ServerLevel level = server.getLevel(fightDimension);
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(state.getDragonUuid());
        if (!(entity instanceof EnderDragon dragon) || entity.isRemoved()) {
            onFightEnded(level, false);
            return;
        }

        boolean isDying = !dragon.isAlive()
                || dragon.isDeadOrDying()
                || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
        if (isDying) {
            movesetController.abortActiveMove(dragon);
            ServerBossEvent bar = this.bossBar;
            if (bar != null) {
                bar.setProgress(0.0F);
            }
            return;
        }

        long tick = state.incrementTick();

        if (tick % 20L == 0L) {
            pruneDeadAnchors(state, level);
        }

        float maxHp = Math.max(1.0F, dragon.getMaxHealth());
        float progress = Math.max(0.0F, Math.min(1.0F, dragon.getHealth() / maxHp));
        ServerBossEvent bar = this.bossBar;
        if (bar != null) {
            bar.setProgress(progress);
        }

        double prevRatio = state.getLastHealthRatio();
        double newRatio = progress;
        VariantBehavior behavior = this.activeBehavior;
        if (newRatio < prevRatio) {
            if (behavior != null) {
                behavior.onHealthThreshold(state, level, dragon, prevRatio, newRatio);
            }
            dispatchMinionHealthThreshold(state, level, dragon, prevRatio, newRatio);
        }
        state.setLastHealthRatio(newRatio);

        movesetController.onTick(state, level, dragon, behavior);

        if (behavior != null) {
            behavior.onTick(state, level, dragon);
        }
        dispatchMinionTick(state, level, dragon);
    }

    private void pruneDeadAnchors(FightState state, ServerLevel level) {
        for (UUID uuid : state.getActiveAnchors()) {
            Entity anchor = level.getEntity(uuid);
            if (anchor != null && (anchor.isRemoved() || !anchor.isAlive())) {
                state.removeAnchor(uuid);
            }
        }
    }

    public void onAnchorDestroyed(ServerLevel level, UUID anchorUuid, BlockPos pos, DamageSource source) {
        FightState state = this.activeFight;
        if (state == null || state.isEnded() || anchorUuid == null) {
            return;
        }
        if (!state.getActiveAnchors().contains(anchorUuid)) {
            return;
        }
        state.removeAnchor(anchorUuid);

        ServerPlayer attacker = DragonShield.resolvePlayerAttacker(source);
        if (attacker != null) {
            state.recordCrystalOrGuardianBreak(attacker.getUUID());
        }

        VariantBehavior behavior = this.activeBehavior;
        if (behavior != null && level != null) {
            behavior.onCrystalDestroyed(state, level, pos, source);
        }
        if (level != null) {
            dispatchCrystalDestroyed(state, level, anchorUuid, pos, source);
        }
    }

    public void onDragonDyingStarted(ServerLevel level, EnderDragon dragon) {
        if (!isCustomDragon(dragon)) {
            return;
        }
        movesetController.abortActiveMove(dragon);
        this.dyingDragonUuid = dragon.getUUID();
        ServerBossEvent bar = this.bossBar;
        if (bar != null) {
            bar.setProgress(0.0F);
        }
        if (!this.deathBurstPlayed) {
            this.deathBurstPlayed = true;
            movesetController.onDragonDeathBurst(level, dragon);
        }
    }

    public void onDragonDeathTick(ServerLevel level, EnderDragon dragon, int deathTime) {
        if (isCustomOrDyingDragon(dragon)) {
            movesetController.onDeathTick(level, dragon, deathTime);
            if (deathTime >= 200 && dragon.getUUID().equals(this.dyingDragonUuid)) {
                this.dyingDragonUuid = null;
            }
        }
    }

    public void onDragonKilled(ServerLevel level, EnderDragon dragon) {
        FightState state = this.activeFight;
        if (state == null || state.isEnded() || dragon == null) {
            return;
        }
        if (!dragon.getUUID().equals(state.getDragonUuid())) {
            return;
        }
        this.dyingDragonUuid = dragon.getUUID();
        if (!this.deathBurstPlayed) {
            this.deathBurstPlayed = true;
            movesetController.onDragonDeathBurst(level, dragon);
        }
        if (level != null) {
            try {
                DragonRewardManager.distributeFightRewards(state, level);
            } catch (Exception e) {
                LOGGER.error("Error distributing dragon fight rewards", e);
            }
        }
        boolean fromAltar = state.isFromAltar();
        onFightEnded(level, true);
        if (level != null) {
            grantKillAdvancements(level);
            if (fromAltar) {
                triggerStellarityPortalReopen(level);
            }
            scheduleStellarityAltarActivation(level);
        }
    }

    private void grantKillAdvancements(ServerLevel level) {
        AdvancementHolder killAdv = server.getAdvancements().get(Identifier.parse(ADV_KILL_DRAGON));
        AdvancementHolder godSlayerAdv = server.getAdvancements().get(Identifier.parse(ADV_GOD_SLAYER));
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            double dx = player.getX();
            double dy = player.getY() - 80.0D;
            double dz = player.getZ();
            if ((dx * dx + dy * dy + dz * dz) <= 128.0D * 128.0D) {
                awardAdvancement(player, killAdv);
                awardAdvancement(player, godSlayerAdv);
            }
        }
    }

    private static void awardAdvancement(ServerPlayer player, AdvancementHolder holder) {
        if (player == null || holder == null) {
            return;
        }
        for (String criterion : holder.value().criteria().keySet()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private void triggerStellarityPortalReopen(ServerLevel level) {
        Marker marker = AltarManager.get(server).findStellarityExitPortalMarker(level);
        if (marker == null) {
            return;
        }
        Optional<CommandFunction<CommandSourceStack>> activatePortal =
                server.getFunctions().get(Identifier.parse(FN_PORTAL_ACTIVATE));
        activatePortal.ifPresent(fn -> {
            CommandSourceStack src = server.getFunctions().getGameLoopSender()
                    .withLevel(level)
                    .withEntity(marker)
                    .withPosition(marker.position());
            server.getFunctions().execute(fn, src);
        });

        Optional<CommandFunction<CommandSourceStack>> chooseGateway =
                server.getFunctions().get(Identifier.parse(FN_GATEWAY_CHOOSE));
        chooseGateway.ifPresent(fn -> {
            CommandSourceStack src = server.getFunctions().getGameLoopSender()
                    .withLevel(level)
                    .withEntity(marker)
                    .withPosition(marker.position());
            server.getFunctions().execute(fn, src);
        });
    }

    private void scheduleStellarityAltarActivation(ServerLevel level) {
        this.pendingAltarDimension = level.dimension();
        this.pendingAltarSoundTicks = 100;
        this.pendingAltarActivateTicks = 160;
    }

    private void tickPendingAltarActivation() {
        if (pendingAltarSoundTicks < 0 && pendingAltarActivateTicks < 0) {
            return;
        }
        ServerLevel level = server.getLevel(pendingAltarDimension);
        if (level == null) {
            pendingAltarSoundTicks = -1;
            pendingAltarActivateTicks = -1;
            return;
        }
        if (pendingAltarSoundTicks > 0) {
            pendingAltarSoundTicks--;
            if (pendingAltarSoundTicks == 0) {
                pendingAltarSoundTicks = -1;
                executeFunction(level, FN_ALTAR_SOUND);
            }
        }
        if (pendingAltarActivateTicks > 0) {
            pendingAltarActivateTicks--;
            if (pendingAltarActivateTicks == 0) {
                pendingAltarActivateTicks = -1;
                executeFunction(level, FN_ALTAR_ACTIVATE);
            }
        }
    }

    private void executeFunction(ServerLevel level, String functionId) {
        Optional<CommandFunction<CommandSourceStack>> fn = server.getFunctions().get(Identifier.parse(functionId));
        fn.ifPresent(f -> server.getFunctions().execute(
                f,
                server.getFunctions().getGameLoopSender().withLevel(level)
        ));
    }

    /**
     * Cleans up floating vanilla End Exit Portal / dirt artifacts at (0, 240..255, 0)
     * and any leftover structure_block / redstone_block beneath Stellarity's Exit Portal at (0, 56..61, 0).
     */
    public static void cleanupEndPortalArtifacts(ServerLevel endLevel) {
        if (endLevel == null || !endLevel.hasChunk(0, 0)) {
            return;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int y = 240; y <= 255; y++) {
                    BlockPos pos = new BlockPos(dx, y, dz);
                    if (!endLevel.getBlockState(pos).isAir()) {
                        endLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 56; y <= 61; y++) {
                    BlockPos pos = new BlockPos(dx, y, dz);
                    var st = endLevel.getBlockState(pos);
                    if (st.is(Blocks.STRUCTURE_BLOCK) || st.is(Blocks.REDSTONE_BLOCK)) {
                        endLevel.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
                    }
                }
            }
        }
    }

    public boolean handleDragonDamageEntity(ServerLevel level, Entity target, DamageSource source, float amount) {
        FightState state = this.activeFight;
        if (state == null || state.isEnded() || level == null || target == null) {
            return false;
        }
        if (minionHooks.onDragonDamageEntity(state, level, target, source, amount)) {
            return true;
        }
        for (MinionHooks hook : MinionHooks.registered()) {
            if (hook.onDragonDamageEntity(state, level, target, source, amount)) {
                return true;
            }
        }
        return false;
    }

    public void syncBossBarPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerBossEvent bar = this.bossBar;
        FightState state = this.activeFight;
        if (bar == null) {
            return;
        }
        if (state != null && !state.isEnded() && player.level().dimension().equals(fightDimension)) {
            bar.addPlayer(player);
        } else {
            bar.removePlayer(player);
        }
    }

    public void removeBossBarPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerBossEvent bar = this.bossBar;
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    public void recordDamage(UUID playerUuid, float amount) {
        FightState state = this.activeFight;
        if (state != null) {
            state.recordDamage(playerUuid, amount);
        }
    }

    public void recordCrystalOrGuardianBreak(UUID playerUuid) {
        FightState state = this.activeFight;
        if (state != null) {
            state.recordCrystalOrGuardianBreak(playerUuid);
        }
    }

    public boolean isDragonProtected(ServerLevel level, EnderDragon dragon) {
        FightState state = this.activeFight;
        if (state == null || state.isEnded() || dragon == null) {
            return false;
        }
        if (!dragon.getUUID().equals(state.getDragonUuid())) {
            return false;
        }
        return state.hasActiveAnchors();
    }

    public synchronized void onFightEnded(ServerLevel level, boolean dragonKilled) {
        FightState state = this.activeFight;
        if (state == null || state.isEnded()) {
            return;
        }
        state.markEnded();
        DragonShield.clearCooldowns();

        try {
            movesetController.cleanup(level);
        } catch (Exception e) {
            LOGGER.error("Error cleaning up moveset controller", e);
        }

        VariantBehavior behavior = this.activeBehavior;
        if (behavior != null && level != null) {
            try {
                behavior.cleanup(state, level);
            } catch (Exception e) {
                LOGGER.error("Error cleaning up variant behavior", e);
            }
        }
        if (level != null) {
            dispatchMinionFightEnded(state, level, dragonKilled);
            for (UUID minionUuid : state.getTrackedMinions()) {
                Entity minion = level.getEntity(minionUuid);
                if (minion != null && !minion.isRemoved()) {
                    minion.discard();
                }
            }
            if (!dragonKilled) {
                Entity dragon = level.getEntity(state.getDragonUuid());
                if (dragon instanceof EnderDragon ed) {
                    cleanupVariantTeam(ed);
                    if (!dragon.isRemoved()) {
                        dragon.discard();
                    }
                }
                if (state.isFromAltar() && level.getDragonFight() instanceof EnderDragonFightBridge bridge) {
                    bridge.expeditionDragon$clearCustomDragon(state.getDragonUuid());
                }
            } else {
                Entity dragon = level.getEntity(state.getDragonUuid());
                if (dragon instanceof EnderDragon ed) {
                    cleanupVariantTeam(ed);
                }
            }
        }
        if (state.isChunkForcedByFight()) {
            state.setChunkForcedByFight(false);
            if (level != null) {
                try {
                    level.setChunkForced(0, 0, false);
                } catch (RuntimeException e) {
                    LOGGER.warn("Failed to unforce chunk (0,0) on fight end", e);
                }
            }
        }

        ServerBossEvent bar = this.bossBar;
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
        this.bossBar = null;
        this.activeBehavior = null;
        this.activeFight = null;
        if (!dragonKilled) {
            this.dyingDragonUuid = null;
        }

        LOGGER.info("Ended {} dragon fight (dragonKilled={})", state.getVariant().id(), dragonKilled);
    }

    public synchronized void shutdown() {
        this.pendingAltarSoundTicks = -1;
        this.pendingAltarActivateTicks = -1;
        this.dyingDragonUuid = null;
        FightState state = this.activeFight;
        if (state != null && !state.isEnded()) {
            ServerLevel level = server.getLevel(fightDimension);
            onFightEnded(level, false);
        }
    }

    private void dispatchMinionFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
        minionHooks.onFightStart(state, level, dragon);
        for (MinionHooks hook : MinionHooks.registered()) {
            hook.onFightStart(state, level, dragon);
        }
    }

    private void dispatchMinionTick(FightState state, ServerLevel level, EnderDragon dragon) {
        minionHooks.onTick(state, level, dragon);
        for (MinionHooks hook : MinionHooks.registered()) {
            hook.onTick(state, level, dragon);
        }
    }

    private void dispatchMinionHealthThreshold(FightState state, ServerLevel level, EnderDragon dragon, double prevRatio, double newRatio) {
        minionHooks.onHealthThreshold(state, level, dragon, prevRatio, newRatio);
        for (MinionHooks hook : MinionHooks.registered()) {
            hook.onHealthThreshold(state, level, dragon, prevRatio, newRatio);
        }
    }

    public void dispatchCrystalSpawned(FightState state, ServerLevel level, EndCrystal crystal) {
        minionHooks.onCrystalSpawned(state, level, crystal);
        for (MinionHooks hook : MinionHooks.registered()) {
            hook.onCrystalSpawned(state, level, crystal);
        }
    }

    private void dispatchCrystalDestroyed(FightState state, ServerLevel level, UUID anchorUuid, BlockPos pos, DamageSource source) {
        minionHooks.onCrystalDestroyed(state, level, anchorUuid, pos, source);
        for (MinionHooks hook : MinionHooks.registered()) {
            hook.onCrystalDestroyed(state, level, anchorUuid, pos, source);
        }
    }

    private void dispatchMinionFightEnded(FightState state, ServerLevel level, boolean dragonKilled) {
        try {
            minionHooks.onFightEnded(state, level, dragonKilled);
        } catch (Exception e) {
            LOGGER.error("Error cleaning up primary minion hooks", e);
        }
        for (MinionHooks hook : MinionHooks.registered()) {
            try {
                hook.onFightEnded(state, level, dragonKilled);
            } catch (Exception e) {
                LOGGER.error("Error cleaning up registered minion hook", e);
            }
        }
    }
}
