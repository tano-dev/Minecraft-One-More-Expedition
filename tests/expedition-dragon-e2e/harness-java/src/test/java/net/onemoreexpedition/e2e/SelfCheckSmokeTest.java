package net.onemoreexpedition.e2e;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.e2e.harness.BootstrapSupport;
import net.onemoreexpedition.e2e.harness.E2EFixtures;
import net.onemoreexpedition.e2e.harness.E2EOracles;
import net.onemoreexpedition.e2e.harness.ModJarBridge;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("selfcheck")
public class SelfCheckSmokeTest {

    @Test
    void mc263BootstrapAndRegistriesAvailable() {
        BootstrapSupport.ensureBootstrapped();
        assertNotNull(Items.END_CRYSTAL);
        assertNotNull(EntityTypes.ENDER_DRAGON);
        assertNotNull(EntityTypes.ELDER_GUARDIAN);
        assertNotNull(EntityTypes.WARDEN);
        assertNotNull(Attributes.KNOCKBACK_RESISTANCE);
        assertEquals(Items.END_CRYSTAL, BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("end_crystal")));
    }

    @Test
    void missingTargetJarFailsCleanlyWithoutCrash() {
        ModJarBridge missing = new ModJarBridge(Path.of("d:/Minecraft-One-More-Expedition/tests/expedition-dragon-e2e/nonexistent-mod.jar"));
        assertFalse(missing.exists());
        AssertionError err = assertThrows(AssertionError.class, missing::requireJar);
        assertTrue(err.getMessage().contains("Target mod JAR missing"));
    }

    @Test
    void oraclesEnforceRequirementBoundaries() {
        Map<String, Double> equalWeights = new LinkedHashMap<>();
        for (String v : E2EFixtures.VARIANTS) {
            equalWeights.put(v, 0.20);
        }
        E2EOracles.assertWeightDistributionValid(equalWeights);

        Map<String, Double> nanWeights = new LinkedHashMap<>(equalWeights);
        nanWeights.put("chronos", Double.NaN);
        assertThrows(AssertionError.class, () -> E2EOracles.assertWeightDistributionValid(nanWeights));

        Map<String, Double> infWeights = new LinkedHashMap<>(equalWeights);
        infWeights.put("chronos", Double.POSITIVE_INFINITY);
        assertThrows(AssertionError.class, () -> E2EOracles.assertWeightDistributionValid(infWeights));

        E2EOracles.assertCrystalBroadcastMatches("§5☬ §eSteve §dplaced an End Crystal! §7(1/4)", "Steve", 1);

        assertTrue(E2EOracles.qualifiesForBaseReward(20.0, 2000.0));
        assertTrue(E2EOracles.qualifiesForBaseReward(15.0, 1500.0)); // 1%
        assertFalse(E2EOracles.qualifiesForBaseReward(10.0, 1500.0)); // <1% and <20

        // Pillared player (deltaY = 10 > 5) with 5.5 horizontal teleport must pass
        E2EOracles.assertWardenTeleportValid(new Vec3(10.0, 80.0, 10.0), new Vec3(15.5, 70.0, 10.0));
        assertThrows(AssertionError.class, () ->
                E2EOracles.assertWardenTeleportValid(new Vec3(10.0, 80.0, 10.0), new Vec3(11.0, 70.0, 10.0)));
    }
}
