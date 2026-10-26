package org.bitwisemadness.tensurafactionspawn.spawn;

/**
 * Minecraft-free nearest-candidate selection, extracted so the capital choice is
 * unit-testable on a plain JVM.
 */
public final class NearestSelection {

    private NearestSelection() {
    }

    /**
     * Index of the candidate nearest to {@code (originX, originZ)} by squared
     * horizontal distance, or {@code -1} when there are no candidates. Ties keep
     * the lowest index, so the result is deterministic.
     *
     * @param candidates each entry is {@code {x, z}}
     */
    public static int nearestIndex(int originX, int originZ, int[][] candidates) {
        int best = -1;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < candidates.length; i++) {
            long dx = (long) candidates[i][0] - originX;
            long dz = (long) candidates[i][1] - originZ;
            long distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }
}
