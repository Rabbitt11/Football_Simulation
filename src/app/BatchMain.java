package app;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import entities.Tactics;
import entities.TeamLoader;
import simulation.BatchSimulator;
import simulation.MatchResult;

/*
 * BatchMain - answers the question "which tactic actually wins?"
 *
 * Plays hundreds of 5-minute matches with no window, in parallel, and prints:
 *   1. how much faster the parallel run is, and proof it gives identical results
 *   2. win / draw / loss for each match-up
 *   3. a race condition demo (unsafe counter vs AtomicLong vs stream sum)
 *
 * Run it like Main. Optional argument: number of matches per match-up (default 200).
 */
public class BatchMain {

    private static final long SEED = 42;

    public static void main(String[] args) throws IOException {
        int matches = args.length > 0 ? Integer.parseInt(args[0]) : 200;

        // Load each preset ONCE. Tactics is immutable, so every thread can share these safely.
        Map<String, Tactics> presets = new LinkedHashMap<>();
        for (String name : List.of("aggressive", "average", "defensive")) {
            presets.put(name, TeamLoader.loadTactics("teams/presets/" + name + ".txt"));
        }
        Tactics aggressive = presets.get("aggressive");
        Tactics average = presets.get("average");
        Tactics defensive = presets.get("defensive");

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Football Simulation - batch mode");
        System.out.println(matches + " matches per match-up, 2 x " + 150 + "s halves, " + cores + " CPU cores");
        System.out.println();

        // ---------- 1. Sequential vs parallel: speed and correctness ----------
        long start = System.nanoTime();
        List<MatchResult> sequential = BatchSimulator.playMany(aggressive, defensive, matches, SEED, false);
        double sequentialSeconds = (System.nanoTime() - start) / 1e9;

        start = System.nanoTime();
        List<MatchResult> parallel = BatchSimulator.playMany(aggressive, defensive, matches, SEED, true);
        double parallelSeconds = (System.nanoTime() - start) / 1e9;

        boolean identical = sequential.toString().equals(parallel.toString());

        System.out.printf("Sequential: %.2f s   Parallel: %.2f s   Speed-up: %.1fx%n",
                sequentialSeconds, parallelSeconds, sequentialSeconds / parallelSeconds);
        System.out.println("Parallel results identical to sequential: " + identical);
        System.out.println();

        // ---------- 2. Which tactic wins? ----------
        System.out.println("Results (from the first team's point of view):");
        System.out.println("  " + BatchSimulator.summarise(
                BatchSimulator.playMany(average, average, matches, SEED, true)) + "   <- control");
        System.out.println("  " + BatchSimulator.summarise(parallel));
        System.out.println("  " + BatchSimulator.summarise(
                BatchSimulator.playMany(aggressive, average, matches, SEED, true)));
        System.out.println("  " + BatchSimulator.summarise(
                BatchSimulator.playMany(defensive, average, matches, SEED, true)));
        System.out.println();

        // ---------- 3. Race condition demo ----------
        System.out.println(BatchSimulator.raceConditionDemo(average, average, Math.max(20, matches / 4)));
    }
}