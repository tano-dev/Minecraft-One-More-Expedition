package net.onemoreexpedition.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.MinionHooks;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HooksContractTest {

    @Test
    @DisplayName("VariantBehavior.defaultFor and register/create work cleanly for all variants")
    void variantBehaviorRegistryAndDefaults() {
        for (DragonVariant variant : DragonVariant.values()) {
            VariantBehavior def = VariantBehavior.defaultFor(variant);
            assertEquals(variant, def.variant());
            assertFalse(def.hasCustomShieldAnchors());
            assertFalse(def.suppressCrystalSpawns());
            assertArrayEquals(new double[]{0.75D, 0.50D, 0.25D}, def.getEndermanCallThresholds());
            assertEquals(2, def.getShadowPhantomsPerCrystal(new DragonConfig.MinionConfig()));
            assertEquals(200.0, def.modifyMovesetCooldown(200.0), 1e-6);
            assertEquals(4, def.modifyChainFireballCount(4));
            Vec3 v = new Vec3(1.0, 2.0, 3.0);
            assertSame(v, def.modifyFireballVelocity(null, v, null));
        }
        assertEquals(DragonVariant.INFERNAL, VariantBehavior.defaultFor(null).variant());

        VariantBehavior custom = new VariantBehavior() {
            @Override
            public DragonVariant variant() {
                return DragonVariant.CHRONOS;
            }

            @Override
            public int modifyChainFireballCount(int baseCount) {
                return baseCount + 2;
            }
        };
        try {
            VariantBehavior.register(DragonVariant.CHRONOS, () -> custom);
            VariantBehavior created = VariantBehavior.create(DragonVariant.CHRONOS);
            assertSame(custom, created);
            assertEquals(6, created.modifyChainFireballCount(4));
        } finally {
            VariantBehavior.REGISTRY.remove(DragonVariant.CHRONOS);
        }
    }

    @Test
    @DisplayName("MinionHooks.NO_OP and multi-listener register/unregister operate safely")
    void minionHooksMultiListenerRegistry() {
        FightState state = new FightState(UUID.randomUUID(), DragonVariant.INFERNAL, false, 1500.0);
        assertDoesNotThrow(() -> {
            MinionHooks.NO_OP.onFightStart(state, null, null);
            MinionHooks.NO_OP.onTick(state, null, null);
            MinionHooks.NO_OP.onHealthThreshold(state, null, null, 1.0, 0.5);
            MinionHooks.NO_OP.onCrystalSpawned(state, null, null);
            MinionHooks.NO_OP.onCrystalDestroyed(state, null, UUID.randomUUID(), BlockPos.ZERO, null);
            assertFalse(MinionHooks.NO_OP.onDragonDamageEntity(state, null, null, null, 10.0F));
            assertFalse(MinionHooks.NO_OP.isPlayerFlightRestricted(state, null));
            MinionHooks.NO_OP.onFightEnded(state, null, true);
        });

        AtomicInteger calls = new AtomicInteger();
        MinionHooks listener = new MinionHooks() {
            @Override
            public void onCrystalDestroyed(FightState s, ServerLevel level, BlockPos pos, DamageSource source) {
                calls.incrementAndGet();
            }

            @Override
            public void onFightStart(FightState s, ServerLevel level, EnderDragon dragon) {
                calls.incrementAndGet();
            }
        };
        try {
            MinionHooks.register(MinionHooks.NO_OP);
            assertFalse(MinionHooks.registered().contains(MinionHooks.NO_OP));

            MinionHooks.register(listener);
            MinionHooks.register(listener);
            assertEquals(1, MinionHooks.registered().stream().filter(h -> h == listener).count());

            listener.onCrystalDestroyed(state, null, UUID.randomUUID(), BlockPos.ZERO, null);
            assertEquals(1, calls.get());
        } finally {
            MinionHooks.unregister(listener);
        }
        assertFalse(MinionHooks.registered().contains(listener));
    }

    @Test
    @DisplayName("MinionHooks.register deduplicates atomically under concurrent registration")
    void minionHooksConcurrentRegisterDeduplicates() throws Exception {
        MinionHooks listener = new MinionHooks() {};
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int trial = 0; trial < 200; trial++) {
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(threads);
                for (int t = 0; t < threads; t++) {
                    pool.submit(() -> {
                        try {
                            start.await();
                            MinionHooks.register(listener);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }
                start.countDown();
                assertTrue(done.await(5, TimeUnit.SECONDS));
                long count = MinionHooks.registered().stream().filter(h -> h == listener).count();
                assertEquals(1L, count, "Concurrent register must not insert duplicate listener");
                MinionHooks.unregister(listener);
            }
        } finally {
            MinionHooks.unregister(listener);
            pool.shutdownNow();
        }
    }
}
