package net.onemoreexpedition.dragon.altar;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AltarManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("expedition-dragon");
    private static final ConcurrentHashMap<MinecraftServer, AltarManager> MANAGERS = new ConcurrentHashMap<>();

    public static final int REQUIRED_CRYSTALS = 4;
    public static final double ALTAR_RADIUS_SQ = 36.0D;
    public static final int MIN_ALTAR_Y = 45;
    public static final int MAX_ALTAR_Y = 110;
    public static final AABB ALTAR_SCAN_AABB = new AABB(-6.5D, MIN_ALTAR_Y, -6.5D, 7.5D, MAX_ALTAR_Y + 1.0D, 7.5D);
    public static final String STELLARITY_EXIT_PORTAL_TAG = "stellarity.exit_portal";
    public static final String STELLARITY_END_CRYSTAL_TAG = "stellarity.end_crystal";
    private static final String FN_DEACTIVATE_PORTAL = "stellarity:structure/exit_portal/deactivated/generate";

    private final MinecraftServer server;
    private final Map<UUID, BlockPos> altarCrystals = new LinkedHashMap<>();
    private final Set<UUID> lockedCrystalUuids = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private volatile boolean respawnInProgress;
    private int pendingInteractionSweepTicks;
    private int ritualTick = -1;

    private AltarManager(MinecraftServer server) {
        this.server = server;
    }

    public static AltarManager get(MinecraftServer server) {
        return MANAGERS.computeIfAbsent(server, AltarManager::new);
    }

    public static void remove(MinecraftServer server) {
        if (server != null) {
            MANAGERS.remove(server);
        }
    }

    public static boolean isAltarPosition(BlockPos pos) {
        return pos != null && isAltarPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isAltarPosition(int x, int y, int z) {
        if (y < MIN_ALTAR_Y || y > MAX_ALTAR_Y) {
            return false;
        }
        return isWithinAltarRadius(x, z);
    }

    public static boolean isWithinAltarRadius(double x, double z) {
        return (x * x + z * z) <= ALTAR_RADIUS_SQ;
    }

    public static String formatCrystalPlacedMessage(String playerName, int count) {
        String safeName = (playerName == null || playerName.isBlank()) ? "Unknown" : playerName;
        int clamped = Math.max(1, Math.min(REQUIRED_CRYSTALS, count));
        return "§5☬ §e" + safeName + " §dplaced an End Crystal! §7(" + clamped + "/4)";
    }

    public synchronized void onServerStarted(MinecraftServer srv) {
        ServerLevel endLevel = srv.getLevel(Level.END);
        if (endLevel != null) {
            resyncAltarCrystals(endLevel);
        }
    }

    public synchronized void onServerTick() {
        ServerLevel endLevel = server.getLevel(Level.END);
        if (pendingInteractionSweepTicks > 0) {
            pendingInteractionSweepTicks--;
            if (endLevel != null) {
                discardAltarInteractions(endLevel, ALTAR_SCAN_AABB);
            }
        }
        if (ritualTick >= 0) {
            if (endLevel != null) {
                tickAltarSpawnRitual(endLevel);
            } else {
                LOGGER.warn("Ritual active but Level.END is null!");
            }
        }
    }

    private void tickAltarSpawnRitual(ServerLevel level) {
        if (ritualTick < 620) {
            ritualTick++;
            
            // Screenshake and sounds
            if (ritualTick == 1) {
                level.playSound(null, 0.5D, 80.0D, 0.5D, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.5F);
            }
            
            // Visual pulses/particles
            if (ritualTick >= 100 && ritualTick % 20 == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, 0.5D, 64.0D, 0.5D, 15, 2.0D, 1.0D, 2.0D, 0.1D);
            }

            // Spawn crystals (roughly matching ticks)
            if (ritualTick == 230) spawnCrystal(level, 0);
            if (ritualTick == 260) spawnCrystal(level, 1);
            if (ritualTick == 290) { spawnCrystal(level, 2); StellarityPillars.repairIronBarCage(level, StellarityPillars.PILLARS.get(2).basePos()); }
            if (ritualTick == 320) spawnCrystal(level, 3);
            if (ritualTick == 350) spawnCrystal(level, 4);
            if (ritualTick == 380) { spawnCrystal(level, 5); StellarityPillars.repairIronBarCage(level, StellarityPillars.PILLARS.get(5).basePos()); }
            if (ritualTick == 410) spawnCrystal(level, 6);
            if (ritualTick == 440) { spawnCrystal(level, 7); StellarityPillars.repairIronBarCage(level, StellarityPillars.PILLARS.get(7).basePos()); }
            if (ritualTick == 470) spawnCrystal(level, 8);
            if (ritualTick == 500) { spawnCrystal(level, 9); StellarityPillars.repairIronBarCage(level, StellarityPillars.PILLARS.get(9).basePos()); }

            // Sonic Boom at the end
            if (ritualTick == 531) {
                level.playSound(null, 0.5D, 80.0D, 0.5D, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.BLOCKS, 2.0F, 0.5F);
            }
            
        } else {
            Marker marker = findStellarityExitPortalMarker(level);
            if (marker != null) {
                marker.addTag("stellarity.respawn_dragon");
            }
            ritualTick = -1;
            onRespawnSequenceComplete(level);
        }
    }

    private void spawnCrystal(ServerLevel level, int index) {
        StellarityPillars.Pillar pillar = StellarityPillars.PILLARS.get(index);
        
        // Beam visual: From Altar center (0, 64, 0) to Pillar
        double startX = 0.5D;
        double startY = 64.0D;
        double startZ = 0.5D;
        double endX = pillar.crystalX() + 0.5D;
        double endY = pillar.crystalY() + 0.5D;
        double endZ = pillar.crystalZ() + 0.5D;
        
        // Play particles along the path
        for (int i = 0; i < 20; i++) {
            double progress = i / 20.0D;
            double px = startX + (endX - startX) * progress;
            double py = startY + (endY - startY) * progress;
            double pz = startZ + (endZ - startZ) * progress;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        EndCrystal crystal = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.EVENT);
        if (crystal != null) {
            crystal.snapTo(pillar.crystalX() + 0.5D, pillar.crystalY(), pillar.crystalZ() + 0.5D, 0.0F, 0.0F);
            crystal.setShowBottom(true);
            crystal.addTag(STELLARITY_END_CRYSTAL_TAG);
            if (level.addFreshEntity(crystal)) {
                level.playSound(null, pillar.crystalBlockPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, endX, endY, endZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                
                FightState state = DragonFightManager.get(server).getActiveFight();
                if (state != null) {
                    state.addAnchor(crystal.getUUID());
                    DragonFightManager.get(server).dispatchCrystalSpawned(state, level, crystal);
                }
            }
        }
    }

    private boolean isAnyRespawnInProgress(ServerLevel level) {
        if (this.respawnInProgress) {
            return true;
        }
        return level != null
                && level.getDragonFight() instanceof EnderDragonFightBridge bridge
                && bridge.expeditionDragon$isRespawnInProgress();
    }

    public synchronized boolean canPlaceAltarCrystal(ServerLevel level) {
        if (level == null || level.dimension() != Level.END) {
            return false;
        }
        resyncAltarCrystals(level);
        if (isAnyRespawnInProgress(level)) {
            return false;
        }
        DragonFightManager fightManager = DragonFightManager.get(server);
        FightState active = fightManager.getActiveFight();
        if ((active != null && !active.isEnded()) || fightManager.isDyingInProgress()) {
            return false;
        }
        return altarCrystals.size() < REQUIRED_CRYSTALS;
    }

    public synchronized void onAltarCrystalPlaced(ServerLevel level, BlockPos pos, EndCrystal crystal, Player player) {
        if (level == null || crystal == null || level.dimension() != Level.END) {
            return;
        }
        if (altarCrystals.containsKey(crystal.getUUID())) {
            return;
        }
        resyncAltarCrystals(level);

        // Add explosion effect to mimic activation
        level.explode(crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 1.0F, Level.ExplosionInteraction.NONE);

        crystal.setPermanentlyInvulnerable(true);
        lockedCrystalUuids.add(crystal.getUUID());
        altarCrystals.put(crystal.getUUID(), pos.immutable());

        discardAltarInteractions(level, crystal.getBoundingBox().inflate(2.5D));
        pendingInteractionSweepTicks = 2;

        int count = Math.min(REQUIRED_CRYSTALS, altarCrystals.size());
        String playerName = player != null ? player.getName().getString() : "Unknown";
        String msg = formatCrystalPlacedMessage(playerName, count);
        server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);

        LOGGER.info("Altar End Crystal placed by {} at {} ({}/{})", playerName, pos, count, REQUIRED_CRYSTALS);

        if (count >= REQUIRED_CRYSTALS && !isAnyRespawnInProgress(level)) {
            igniteRespawn(level);
        }
    }

    private void igniteRespawn(ServerLevel level) {
        List<EndCrystal> crystals = new ArrayList<>(REQUIRED_CRYSTALS);
        for (UUID uuid : altarCrystals.keySet()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof EndCrystal crystal && !crystal.isRemoved()) {
                // Explode altar crystals to show consumption
                level.explode(crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
                crystal.discard();
                crystals.add(crystal);
            }
        }
        
        altarCrystals.clear();
        lockedCrystalUuids.clear();

        respawnInProgress = true;
        ritualTick = 0;
        LOGGER.info("Triggered 4/4 Stellarity + True Ending Ender Dragon spawn ritual via End Altar");
    }

    public synchronized void onRespawnSequenceComplete(ServerLevel level) {
        respawnInProgress = false;
        for (UUID uuid : new ArrayList<>(lockedCrystalUuids)) {
            Entity entity = level != null ? level.getEntity(uuid) : null;
            if (entity instanceof EndCrystal crystal) {
                crystal.setPermanentlyInvulnerable(false);
            }
        }
        altarCrystals.clear();
        lockedCrystalUuids.clear();

        if (level == null) {
            return;
        }

        deactivateStellarityPortalIfPresent(level);
        DragonVariant rolled = DragonConfig.get().rollVariant(random);
        DragonFightManager.get(server).startCustomFight(level, rolled, true);
    }

    public synchronized void onRespawnAborted(ServerLevel level) {
        respawnInProgress = false;
        resyncAltarCrystals(level);
    }

    public synchronized void resyncAltarCrystals(ServerLevel level) {
        if (level == null) {
            return;
        }
        altarCrystals.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof EndCrystal) || entity.isRemoved()) {
                lockedCrystalUuids.remove(entry.getKey());
                return true;
            }
            return false;
        });

        FightState active = DragonFightManager.get(server).getActiveFight();
        if (!isAnyRespawnInProgress(level) && (active == null || active.isEnded())) {
            List<EndCrystal> found = level.getEntitiesOfClass(EndCrystal.class, ALTAR_SCAN_AABB);
            for (EndCrystal crystal : found) {
                if (!crystal.isRemoved() && isAltarPosition(crystal.blockPosition())) {
                    if (!altarCrystals.containsKey(crystal.getUUID()) && altarCrystals.size() < REQUIRED_CRYSTALS) {
                        crystal.setPermanentlyInvulnerable(true);
                        lockedCrystalUuids.add(crystal.getUUID());
                        altarCrystals.put(crystal.getUUID(), crystal.blockPosition().immutable());
                    }
                }
            }
            discardAltarInteractions(level, ALTAR_SCAN_AABB);
        }
    }

    public static boolean isLockedCrystal(ServerLevel level, EndCrystal crystal) {
        if (level == null || crystal == null) {
            return false;
        }
        return get(level.getServer()).isLockedCrystal(crystal);
    }

    public boolean isLockedCrystal(EndCrystal crystal) {
        return crystal != null && lockedCrystalUuids.contains(crystal.getUUID());
    }

    public boolean isRespawnInProgress() {
        return respawnInProgress;
    }

    public synchronized int getPlacedCrystalCount() {
        return altarCrystals.size();
    }

    public Marker findStellarityExitPortalMarker(ServerLevel level) {
        if (level == null) {
            return null;
        }
        List<Marker> markers = level.getEntities(
                EntityTypes.MARKER,
                ALTAR_SCAN_AABB,
                marker -> marker.entityTags().contains(STELLARITY_EXIT_PORTAL_TAG)
        );
        return markers.isEmpty() ? null : markers.get(0);
    }

    public boolean hasStellarityExitPortal(ServerLevel level) {
        return findStellarityExitPortalMarker(level) != null;
    }

    private void deactivateStellarityPortalIfPresent(ServerLevel level) {
        Marker marker = findStellarityExitPortalMarker(level);
        if (marker == null) {
            return;
        }
        Optional<CommandFunction<CommandSourceStack>> fn = server.getFunctions().get(Identifier.parse(FN_DEACTIVATE_PORTAL));
        fn.ifPresent(f -> {
            CommandSourceStack src = server.getFunctions().getGameLoopSender()
                    .withLevel(level)
                    .withEntity(marker)
                    .withPosition(marker.position());
            server.getFunctions().execute(f, src);
        });
    }

    private static void discardAltarInteractions(ServerLevel level, AABB box) {
        List<Interaction> interactions = level.getEntitiesOfClass(Interaction.class, box);
        for (Interaction interaction : interactions) {
            if (!interaction.isRemoved()
                    && (interaction.entityTags().contains(STELLARITY_END_CRYSTAL_TAG)
                    || isAltarPosition(interaction.blockPosition()))) {
                interaction.discard();
            }
        }
    }

    public InteractionResult handleAttackEntity(Player player, Level level, Entity entity) {
        return checkLockedInteractionOrCrystal(level, entity);
    }

    public InteractionResult handleUseEntity(Player player, Level level, Entity entity) {
        return checkLockedInteractionOrCrystal(level, entity);
    }

    private InteractionResult checkLockedInteractionOrCrystal(Level level, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.END || entity == null) {
            return InteractionResult.PASS;
        }
        if (entity instanceof EndCrystal crystal && isLockedCrystal(crystal)) {
            return InteractionResult.FAIL;
        }
        if (entity instanceof Interaction interaction) {
            boolean nearLocked = false;
            if (!lockedCrystalUuids.isEmpty() && ALTAR_SCAN_AABB.contains(interaction.position())) {
                nearLocked = true;
            }
            if (nearLocked || (interaction.entityTags().contains(STELLARITY_END_CRYSTAL_TAG)
                    && isAltarPosition(interaction.blockPosition()))) {
                interaction.discard();
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}
