package net.onemoreexpedition.dragon.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.variants.DragonVariant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface VariantBehavior {
    Map<DragonVariant, Supplier<? extends VariantBehavior>> REGISTRY = new ConcurrentHashMap<>();

    static void register(DragonVariant variant, Supplier<? extends VariantBehavior> factory) {
        if (variant != null && factory != null) {
            REGISTRY.put(variant, factory);
        }
    }

    static VariantBehavior create(DragonVariant variant) {
        DragonVariant resolved = variant != null ? variant : DragonVariant.INFERNAL;
        Supplier<? extends VariantBehavior> factory = REGISTRY.get(resolved);
        if (factory != null) {
            VariantBehavior created = factory.get();
            if (created != null) {
                return created;
            }
        }
        return defaultFor(resolved);
    }

    DragonVariant variant();

    default void onFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
    }

    default void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
    }

    default void onCrystalDestroyed(FightState state, ServerLevel level, BlockPos crystalPos, DamageSource source) {
    }

    default void onHealthThreshold(FightState state, ServerLevel level, EnderDragon dragon, double prevRatio, double newRatio) {
    }

    default void modifyFireballCloud(AreaEffectCloud cloud) {
    }

    default Vec3 modifyFireballVelocity(DragonFireball fireball, Vec3 baseVelocity, ServerLevel level) {
        return baseVelocity;
    }

    default void cleanup(FightState state, ServerLevel level) {
    }

    default boolean hasCustomShieldAnchors() {
        return false;
    }

    default boolean suppressCrystalSpawns() {
        return hasCustomShieldAnchors();
    }

    default double[] getEndermanCallThresholds() {
        return new double[]{0.75D, 0.50D, 0.25D};
    }

    default void applyCalledEndermanBuff(Enderman enderman) {
    }

    default int getShadowPhantomsPerCrystal(DragonConfig.MinionConfig config) {
        return config != null ? config.shadowPhantomsPerCrystal : 2;
    }

    default double modifyMovesetCooldown(double baseTicks) {
        return baseTicks;
    }

    default int modifyChainFireballCount(int baseCount) {
        return baseCount;
    }

    static VariantBehavior defaultFor(DragonVariant variant) {
        DragonVariant resolved = variant != null ? variant : DragonVariant.INFERNAL;
        return () -> resolved;
    }
}
