package net.onemoreexpedition.dragon.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public interface MinionHooks {
    MinionHooks NO_OP = new MinionHooks() {};
    CopyOnWriteArrayList<MinionHooks> LISTENERS = new CopyOnWriteArrayList<>();

    static void register(MinionHooks listener) {
        if (listener != null && listener != NO_OP) {
            LISTENERS.addIfAbsent(listener);
        }
    }

    static void unregister(MinionHooks listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    static List<MinionHooks> registered() {
        return Collections.unmodifiableList(LISTENERS);
    }

    default void onFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
    }

    default void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
    }

    default void onHealthThreshold(FightState state, ServerLevel level, EnderDragon dragon, double prevRatio, double newRatio) {
    }

    default void onCrystalSpawned(FightState state, ServerLevel level, EndCrystal crystal) {
    }

    default void onCrystalDestroyed(FightState state, ServerLevel level, BlockPos crystalPos, DamageSource source) {
    }

    default void onCrystalDestroyed(FightState state, ServerLevel level, UUID anchorUuid, BlockPos crystalPos, DamageSource source) {
        onCrystalDestroyed(state, level, crystalPos, source);
    }

    default boolean onDragonDamageEntity(FightState state, ServerLevel level, Entity target, DamageSource source, float amount) {
        return false;
    }

    default boolean isPlayerFlightRestricted(FightState state, ServerPlayer player) {
        return false;
    }

    default void onFightEnded(FightState state, ServerLevel level, boolean dragonKilled) {
    }
}
