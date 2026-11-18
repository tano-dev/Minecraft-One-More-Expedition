package net.onemoreexpedition.e2e.harness;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Idempotent Minecraft 26.3 registry/bootstrap initializer for plain JUnit 5 tests
 * (running without fabric-loader-junit so broken/missing target mod JARs never crash startup).
 */
public final class BootstrapSupport {
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

    private BootstrapSupport() {
    }

    public static void ensureBootstrapped() {
        if (BOOTSTRAPPED.compareAndSet(false, true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}
