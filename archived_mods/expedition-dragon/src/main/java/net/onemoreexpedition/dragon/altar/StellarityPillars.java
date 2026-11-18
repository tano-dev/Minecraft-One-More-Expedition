package net.onemoreexpedition.dragon.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact pillar coordinates and cage mechanics from Stellarity (entity/dragon/spawn/animation.mcfunction).
 */
public final class StellarityPillars {

    public record Pillar(int crystalX, int crystalY, int crystalZ, boolean caged) {
        public BlockPos crystalBlockPos() {
            return new BlockPos(crystalX, crystalY, crystalZ);
        }

        public BlockPos basePos() {
            return new BlockPos(crystalX, crystalY - 1, crystalZ);
        }

        public AABB crystalSearchBox() {
            return new AABB(
                    crystalX - 4.0D, crystalY - 4.0D, crystalZ - 4.0D,
                    crystalX + 5.0D, crystalY + 5.0D, crystalZ + 5.0D
            );
        }
    }

    public static final List<Pillar> PILLARS = List.of(
            new Pillar(63, 101, 0, false),
            new Pillar(50, 106, 36, false),
            new Pillar(18, 95, 59, true),
            new Pillar(-19, 107, 59, false),
            new Pillar(-51, 106, 36, false),
            new Pillar(-63, 94, 0, true),
            new Pillar(-51, 101, -39, false),
            new Pillar(-19, 97, -60, true),
            new Pillar(18, 88, -60, false),
            new Pillar(50, 96, -39, true)
    );

    private StellarityPillars() {
    }

    public static List<EndCrystal> findExistingPillarCrystals(ServerLevel level) {
        List<EndCrystal> result = new ArrayList<>();
        if (level == null) {
            return result;
        }
        for (Pillar pillar : PILLARS) {
            List<EndCrystal> found = level.getEntitiesOfClass(EndCrystal.class, pillar.crystalSearchBox());
            for (EndCrystal crystal : found) {
                if (!crystal.isRemoved() && !AltarManager.get(level.getServer()).isLockedCrystal(crystal)) {
                    result.add(crystal);
                }
            }
        }
        return result;
    }

    public static void clearPillarCrystals(ServerLevel level) {
        if (level == null) {
            return;
        }
        for (Pillar pillar : PILLARS) {
            List<EndCrystal> existing = level.getEntitiesOfClass(EndCrystal.class, pillar.crystalSearchBox());
            for (EndCrystal crystal : existing) {
                if (!crystal.isRemoved() && !AltarManager.get(level.getServer()).isLockedCrystal(crystal)) {
                    crystal.discard();
                }
            }
        }
    }

    public static List<EndCrystal> spawnStellarityPillarCrystals(ServerLevel level, boolean repairCages) {
        List<EndCrystal> spawned = new ArrayList<>();
        if (level == null) {
            return spawned;
        }
        for (Pillar pillar : PILLARS) {
            // Load chunk for pillar
            level.getChunkAt(pillar.crystalBlockPos());

            // Discard any old crystal on this pillar
            List<EndCrystal> existing = level.getEntitiesOfClass(EndCrystal.class, pillar.crystalSearchBox());
            for (EndCrystal old : existing) {
                if (!old.isRemoved() && !AltarManager.get(level.getServer()).isLockedCrystal(old)) {
                    old.discard();
                }
            }

            if (repairCages && pillar.caged()) {
                repairIronBarCage(level, pillar.basePos());
            }

            EndCrystal crystal = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.EVENT);
            if (crystal != null) {
                double cx = pillar.crystalX() + 0.5D;
                double cy = pillar.crystalY();
                double cz = pillar.crystalZ() + 0.5D;
                crystal.snapTo(cx, cy, cz, 0.0F, 0.0F);
                crystal.setShowBottom(true);
                // Removed invalid invulnerability call
                crystal.addTag(AltarManager.STELLARITY_END_CRYSTAL_TAG);
                if (level.addFreshEntity(crystal)) {
                    spawned.add(crystal);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    level.playSound(null, pillar.crystalBlockPos(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.73F);
                }
            }
        }
        return spawned;
    }

    /**
     * Replicates Stellarity's entity/dragon/spawn/repair_cage.mcfunction:
     * Hollow 5x4x5 iron_bars cage around basePos.
     */
    public static void repairIronBarCage(ServerLevel level, BlockPos basePos) {
        if (level == null || basePos == null) {
            return;
        }
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean isWallOrRoof = (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3);
                    BlockPos pos = basePos.offset(dx, dy, dz);
                    BlockState cur = level.getBlockState(pos);
                    if (isWallOrRoof) {
                        if (cur.isAir()) {
                            level.setBlockAndUpdate(pos, bars);
                        }
                    } else {
                        if (cur.getBlock() instanceof IronBarsBlock) {
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }
}
