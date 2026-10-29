package org.bitwisemadness.tensurafactionspawn.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Capital selection math: nearest-to-world-spawn picking. Pure JVM.
 */
class NearestSelectionTest {

    @Test
    void picksTheNearestCandidate() {
        int[][] candidates = {{100, 0}, {10, 0}, {-50, 0}};
        assertEquals(1, NearestSelection.nearestIndex(0, 0, candidates));
    }

    @Test
    void comparesByCombinedHorizontalDistance() {
        // (30,30) is 1800 squared, (40,0) is 1600 squared, so the latter wins
        // even though it is the larger single-axis offset.
        int[][] candidates = {{30, 30}, {40, 0}};
        assertEquals(1, NearestSelection.nearestIndex(0, 0, candidates));
    }

    @Test
    void returnsMinusOneWhenThereAreNoCandidates() {
        assertEquals(-1, NearestSelection.nearestIndex(0, 0, new int[0][]));
    }

    @Test
    void tiesKeepTheLowestIndex() {
        int[][] candidates = {{10, 0}, {-10, 0}};
        assertEquals(0, NearestSelection.nearestIndex(0, 0, candidates));
    }

    @Test
    void doesNotOverflowOnFarCoordinates() {
        int[][] candidates = {{Integer.MAX_VALUE, Integer.MAX_VALUE}, {0, 0}};
        assertEquals(1, NearestSelection.nearestIndex(0, 0, candidates));
    }
}
