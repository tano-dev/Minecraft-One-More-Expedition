package net.onemoreexpedition.e2e.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Self-check GameTest verifying headless MC 26.3 GameTestServer initializes
 * Level.END, EnderDragonFight, and dedicated server semantics cleanly.
 */
public class SelfCheckGameTest {

    @GameTest(dimension = "minecraft:the_end", maxTicks = 40)
    public void verifyEndDimensionAndDedicatedServer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.dimension().equals(Level.END), Component.literal("Expected Level.END"));
        helper.assertTrue(level.getServer().isDedicatedServer(), Component.literal("Expected GameTestServer.isDedicatedServer() == true"));
        helper.assertTrue(level.getDragonFight() != null, Component.literal("Expected non-null EnderDragonFight in Level.END"));
        helper.succeed();
    }
}
