package io.github.glocation87.nature7.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegionTest {

    private final Region region = new Region(new BlockPoint(0, 0, 0), new BlockPoint(9, 9, 9));

    @Test
    void containsPointsInside() {
        assertTrue(region.contains(5, 5, 5));
        assertTrue(region.contains(0, 0, 0));
    }

    @Test
    void includesTheWholeFarBlock() {
        assertTrue(region.contains(9.99, 9.99, 9.99));
        assertFalse(region.contains(10, 5, 5));
    }

    @Test
    void excludesPointsOutside() {
        assertFalse(region.contains(-0.01, 5, 5));
        assertFalse(region.contains(5, 20, 5));
    }

    @Test
    void normalizesCornersGivenInAnyOrder() {
        Region flipped = new Region(new BlockPoint(9, 0, 9), new BlockPoint(0, 9, 0));

        assertEquals(region, flipped);
    }
}
