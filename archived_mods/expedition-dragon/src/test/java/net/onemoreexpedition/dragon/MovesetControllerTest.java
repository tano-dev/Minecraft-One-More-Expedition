package net.onemoreexpedition.dragon;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.combat.MovesetController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovesetControllerTest {

    @Test
    @DisplayName("Cooldown & damage scaling handle normal and invalid multipliers safely")
    void scalingHelpersHandleNormalAndInvalidValues() {
        assertEquals(130, MovesetController.scaleCooldownTicks(260, 0.5, 20));
        assertEquals(20, MovesetController.scaleCooldownTicks(100, 0.05, 20));
        assertEquals(260, MovesetController.scaleCooldownTicks(260, Double.NaN, 20));
        assertEquals(260, MovesetController.scaleCooldownTicks(260, -1.0, 20));

        assertEquals(27.0, MovesetController.scaleDamage(18.0, 1.5), 1e-6);
        assertEquals(18.0, MovesetController.scaleDamage(18.0, Double.NaN), 1e-6);
        assertEquals(0.0, MovesetController.scaleDamage(-5.0, 1.5), 1e-6);
    }

    @Test
    @DisplayName("Triple Dive triggers inclusively at <= 75% HP and never at 0% or > 75%")
    void tripleDiveThresholdBehavior() {
        assertTrue(MovesetController.shouldTriggerTripleDive(0.75, 0.75));
        assertTrue(MovesetController.shouldTriggerTripleDive(0.50, 0.75));
        assertFalse(MovesetController.shouldTriggerTripleDive(0.7501, 0.75));
        assertFalse(MovesetController.shouldTriggerTripleDive(0.0, 0.75));
        assertFalse(MovesetController.shouldTriggerTripleDive(Double.NaN, 0.75));
    }

    @Test
    @DisplayName("Shockwave damage applies full damage inside half radius and linear falloff to outer radius")
    void shockwaveDamageFalloff() {
        assertEquals(18.0, MovesetController.computeShockwaveDamage(18.0, 0.0, 12.0), 1e-6);
        assertEquals(18.0, MovesetController.computeShockwaveDamage(18.0, 6.0, 12.0), 1e-6);
        assertEquals(13.5, MovesetController.computeShockwaveDamage(18.0, 9.0, 12.0), 1e-6);
        assertEquals(9.0, MovesetController.computeShockwaveDamage(18.0, 12.0, 12.0), 1e-6);
        assertEquals(0.0, MovesetController.computeShockwaveDamage(18.0, 12.1, 12.0), 1e-6);
    }

    @Test
    @DisplayName("Ray/AABB intersection, ring generation, dive velocity, and fireball salvo directions behave accurately")
    void geometryHelpersBehaveAccurately() {
        AABB target = new AABB(-0.3, 64.0, -0.3, 0.3, 65.8, 0.3);
        assertTrue(MovesetController.rayHitsAabb(new Vec3(-10, 65, 0), new Vec3(10, 65, 0), target, 0.5));
        assertFalse(MovesetController.rayHitsAabb(new Vec3(-10, 72, 0), new Vec3(10, 72, 0), target, 0.5));

        List<Vec3> ring = MovesetController.computeRingPoints(1.0, 64.0, -2.0, 5.0, 16);
        assertEquals(16, ring.size());
        for (Vec3 pt : ring) {
            double dx = pt.x - 1.0;
            double dz = pt.z + 2.0;
            assertEquals(5.0, Math.sqrt(dx * dx + dz * dz), 1e-6);
            assertEquals(64.0, pt.y, 1e-6);
        }

        Vec3 vel = MovesetController.computeDiveVelocity(new Vec3(0, 100, 0), new Vec3(30, 60, 0), 2.0);
        assertEquals(2.0, vel.length(), 1e-6);
        assertEquals(1.2, vel.x, 1e-6);
        assertEquals(-1.6, vel.y, 1e-6);

        List<Vec3> salvo = MovesetController.computeFireballSalvoDirections(new Vec3(0, 0, 1), 3, 0.15);
        assertEquals(3, salvo.size());
        for (Vec3 dir : salvo) {
            assertEquals(1.0, dir.length(), 1e-6);
        }
        assertTrue(salvo.get(0).x > 0.0 || salvo.get(2).x > 0.0);
    }

    @Test
    @DisplayName("rayHitsAabb, diveVelocity, salvo directions, laser yaw, and abort guards handle edge cases safely")
    void edgeCasesAndLaserYawAlignment() {
        AABB unitBox = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        // Zero-length ray on max faces must be inclusive
        assertTrue(MovesetController.rayHitsAabb(new Vec3(1.0, 0.5, 0.5), new Vec3(1.0, 0.5, 0.5), unitBox, 0.0));
        assertTrue(MovesetController.rayHitsAabb(new Vec3(1.0, 1.0, 1.0), new Vec3(1.0, 1.0, 1.0), unitBox, 0.0));
        assertFalse(MovesetController.rayHitsAabb(new Vec3(Double.NaN, 0.5, 0.5), new Vec3(1.0, 0.5, 0.5), unitBox, 0.5));

        // NaN / Infinity / huge finite vectors never propagate NaN
        Vec3 nanDive = MovesetController.computeDiveVelocity(new Vec3(Double.NaN, Double.NaN, Double.NaN), new Vec3(1, 2, 3), 2.0);
        assertEquals(Vec3.ZERO, nanDive);
        Vec3 hugeDive = MovesetController.computeDiveVelocity(Vec3.ZERO, new Vec3(1e200, 0, 0), 2.0);
        assertTrue(Double.isFinite(hugeDive.x) && Double.isFinite(hugeDive.y) && Double.isFinite(hugeDive.z));
        assertEquals(2.0, hugeDive.length(), 1e-6);

        List<Vec3> nanSalvo = MovesetController.computeFireballSalvoDirections(new Vec3(Double.NaN, Double.NaN, Double.NaN), 3, 0.15);
        assertEquals(3, nanSalvo.size());
        for (Vec3 dir : nanSalvo) {
            assertTrue(Double.isFinite(dir.x) && Double.isFinite(dir.y) && Double.isFinite(dir.z));
            assertEquals(1.0, dir.length(), 1e-6);
        }

        // EnderDragon head faces (+sin(yRot), -cos(yRot)); tickLaser rayDir uses (cos(laserYaw+90), sin(laserYaw+90))
        for (float laserYaw : new float[]{-180.0F, -90.0F, -35.0F, 0.0F, 45.0F, 135.0F}) {
            float dragonYaw = MovesetController.computeLaserDragonYaw(laserYaw);
            double dragonHeadX = Math.sin(Math.toRadians(dragonYaw));
            double dragonHeadZ = -Math.cos(Math.toRadians(dragonYaw));
            double rayX = Math.cos(Math.toRadians(laserYaw + 90.0F));
            double rayZ = Math.sin(Math.toRadians(laserYaw + 90.0F));
            assertEquals(rayX, dragonHeadX, 1e-5);
            assertEquals(rayZ, dragonHeadZ, 1e-5);
        }
        assertEquals(0.0F, MovesetController.computeLaserDragonYaw(Float.NaN));

        MovesetController controller = new MovesetController();
        assertFalse(controller.isMovementOverridden(null));
        assertDoesNotThrow(() -> controller.abortActiveMove(null));
    }
}
