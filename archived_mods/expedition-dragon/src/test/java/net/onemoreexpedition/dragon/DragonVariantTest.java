package net.onemoreexpedition.dragon;

import net.minecraft.world.BossEvent;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DragonVariantTest {

    @Test
    @DisplayName("All 5 variants exist with 0.20 default weights summing to 1.0 and HP in 1000..2000")
    void allVariantsHaveValidDefaults() {
        DragonVariant[] values = DragonVariant.values();
        assertEquals(5, values.length);

        double weightSum = 0.0;
        for (DragonVariant variant : values) {
            assertEquals(0.20, variant.defaultWeight(), 1e-6);
            weightSum += variant.defaultWeight();
            assertTrue(variant.defaultBaseHp() >= 1000.0 && variant.defaultBaseHp() <= 2000.0,
                    "Base HP out of range for " + variant.id());
            assertNotNull(variant.defaultBossBarTitle());
            assertFalse(variant.defaultBossBarTitle().isBlank());
            assertNotNull(variant.defaultBossBarColor());
        }
        assertEquals(1.0, weightSum, 1e-6);
    }

    @Test
    @DisplayName("Variant BossBar colors and base HPs match specification")
    void variantColorsAndHpMatchSpec() {
        assertEquals(BossEvent.BossBarColor.RED, DragonVariant.INFERNAL.defaultBossBarColor());
        assertEquals(1500.0, DragonVariant.INFERNAL.defaultBaseHp());

        assertEquals(BossEvent.BossBarColor.PURPLE, DragonVariant.VOIDGLOOM.defaultBossBarColor());
        assertEquals(1400.0, DragonVariant.VOIDGLOOM.defaultBaseHp());

        assertEquals(BossEvent.BossBarColor.GREEN, DragonVariant.REVENANT.defaultBossBarColor());
        assertEquals(1600.0, DragonVariant.REVENANT.defaultBaseHp());

        assertEquals(BossEvent.BossBarColor.YELLOW, DragonVariant.ENLIGHTENED.defaultBossBarColor());
        assertEquals(1800.0, DragonVariant.ENLIGHTENED.defaultBaseHp());

        assertEquals(BossEvent.BossBarColor.BLUE, DragonVariant.CHRONOS.defaultBossBarColor());
        assertEquals(2000.0, DragonVariant.CHRONOS.defaultBaseHp());
    }

    @Test
    @DisplayName("fromString parses variant IDs and enum names case-insensitively")
    void fromStringParsesCaseInsensitively() {
        assertEquals(DragonVariant.INFERNAL, DragonVariant.fromString("infernal"));
        assertEquals(DragonVariant.VOIDGLOOM, DragonVariant.fromString("VOIDGLOOM"));
        assertEquals(DragonVariant.REVENANT, DragonVariant.fromString(" Revenant "));
        assertEquals(DragonVariant.ENLIGHTENED, DragonVariant.fromString("Enlightened"));
        assertEquals(DragonVariant.CHRONOS, DragonVariant.fromString("chronos"));

        assertNull(DragonVariant.fromString(null));
        assertNull(DragonVariant.fromString("   "));
        assertNull(DragonVariant.fromString("unknown_dragon"));
    }

    @Test
    @DisplayName("ids returns all 5 lowercase identifiers")
    void idsReturnsAllFiveIdentifiers() {
        assertEquals(
                List.of("infernal", "voidgloom", "revenant", "enlightened", "chronos"),
                DragonVariant.ids()
        );
    }

    @Test
    @DisplayName("parseBossBarColor handles valid names and falls back safely")
    void parseBossBarColorHandlesFallback() {
        assertEquals(BossEvent.BossBarColor.PURPLE,
                DragonVariant.parseBossBarColor("purple", BossEvent.BossBarColor.RED));
        assertEquals(BossEvent.BossBarColor.RED,
                DragonVariant.parseBossBarColor("not_a_color", BossEvent.BossBarColor.RED));
        assertEquals(BossEvent.BossBarColor.BLUE,
                DragonVariant.parseBossBarColor(null, BossEvent.BossBarColor.BLUE));
    }
}
