package simulation;

import java.awt.Color;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import entities.Ball;
import entities.Tactics;
import entities.Team;
import entities.TeamComp;
import world.GameMap;
import world.MapPreset;
import world.PlaySpace;

/*
 * BatchSimulator - plays many matches with no window, as fast as the CPU allows,
 * optionally spread across all CPU cores with a parallel stream.
 *
 * WHY THIS IS THREAD-SAFE
 *   - Each match builds its OWN map, ball, teams, players, coaches and Match.
 *     No thread ever touches another thread's objects.
 *   - The only thing shared between matches is Tactics, which is immutable.
 *   - Each match has its own Random with its own seed (no shared random generator).
 *   - Results come back through the stream and are gathered by a collector,
 *     never by threads writing into one shared variable.
 */
public final class BatchSimulator {

    private static final int HALF_LENGTH_SECONDS = 150;

    private BatchSimulator() {
        // utility class
    }

    // Build a brand-new, completely independent match.
    private static MatchEngine newEngine(Tactics redTactics, Tactics blueTactics, long seed) {
        GameMap map = new GameMap(MapPreset.DEFAULT);
        PlaySpace playSpace = map.getPlaySpace();

        Team red = new Team("Red", Color.RED, TeamComp.createLeftTeam(playSpace), redTactics);
        Team blue = new Team("Blue", Color.BLUE, TeamComp.createRightTeam(playSpace), blueTactics);
        Ball ball = new Ball(playSpace.getLength() / 2, playSpace.getWidth() / 2, 0.0);
        Ruleset ruleset = new Ruleset(HALF_LENGTH_SECONDS, HALF_LENGTH_SECONDS, 60, 6, false, false);

        return new MatchEngine(map, red, blue, ball, ruleset, new Random(seed));
    }

    // Play one whole match with no window: just call tick() until the final whistle.
    public static MatchResult playOneMatch(Tactics redTactics, Tactics blueTactics, long seed) {
        MatchEngine engine = newEngine(redTactics, blueTactics, seed);
        engine.start();

        while (!engine.isFinished()) {
            engine.tick(MatchEngine.TICK_SECONDS);
        }
        return engine.result();
    }

    /*
     * Play `count` matches between two tactics and return every result from the
     * FIRST tactic's point of view.
     *
     * Fair test: the first tactic plays the red (left) side in even-numbered matches
     * and the blue (right) side in odd-numbered ones, like teams swapping ends. Any
     * advantage built into one side of the pitch cancels out.
     *
     * Reproducible: match i always uses seed firstSeed + i, so the sequential and
     * parallel runs play EXACTLY the same matches. If the parallel results equal the
     * sequential ones, the parallel version is correct.
     */
    public static List<MatchResult> playMany(Tactics first, Tactics second,
            int count, long firstSeed, boolean parallel) {

        IntStream matchNumbers = IntStream.range(0, count);
        if (parallel) {
            matchNumbers = matchNumbers.parallel(); // the common ForkJoinPool splits the range across cores
        }

        return matchNumbers
                .mapToObj(i -> i % 2 == 0
                        ? playOneMatch(first, second, firstSeed + i)
                        : playOneMatch(second, first, firstSeed + i).swapped())
                .collect(Collectors.toList()); // thread-safe gathering, keeps match order
    }

    // One line summary of a batch, from the first tactic's point of view.
    public static String summarise(List<MatchResult> results) {
        Map<MatchResult.Outcome, Long> outcomes = results.stream()
                .collect(Collectors.groupingBy(
                        MatchResult::outcome,
                        () -> new EnumMap<>(MatchResult.Outcome.class),
                        Collectors.counting()));

        double total = results.size();
        double redGoals = results.stream().mapToInt(MatchResult::getRedGoals).average().orElse(0.0);
        double blueGoals = results.stream().mapToInt(MatchResult::getBlueGoals).average().orElse(0.0);
        double possession = results.stream()
                .collect(Collectors.averagingDouble(MatchResult::redPossessionPercent));

        MatchResult first = results.get(0);

        return String.format(
                "%-10s vs %-10s  win %3.0f%%  draw %3.0f%%  loss %3.0f%%   avg goals %.2f - %.2f   possession %2.0f%%",
                first.getRedTactics(), first.getBlueTactics(),
                100 * outcomes.getOrDefault(MatchResult.Outcome.RED_WIN, 0L) / total,
                100 * outcomes.getOrDefault(MatchResult.Outcome.DRAW, 0L) / total,
                100 * outcomes.getOrDefault(MatchResult.Outcome.BLUE_WIN, 0L) / total,
                redGoals, blueGoals, possession);
    }

    // ------------------------------------------------------------------
    // RACE CONDITION DEMO (Week 12). Counts every tick of every match three ways.
    // ------------------------------------------------------------------

    // Shared by all threads and NOT protected. This is the bug we want to show.
    private static long unsafeTickCount = 0;

    public static String raceConditionDemo(Tactics redTactics, Tactics blueTactics, int matches) {
        unsafeTickCount = 0;
        AtomicLong atomicTickCount = new AtomicLong();

        long streamTickCount = IntStream.range(0, matches)
                .parallel()
                .mapToLong(i -> {
                    MatchEngine engine = newEngine(redTactics, blueTactics, 1000 + i);
                    engine.start();

                    while (!engine.isFinished()) {
                        engine.tick(MatchEngine.TICK_SECONDS);

                        // UNSAFE: "read, add 1, write back" is three steps. Two threads can read
                        // the same old value and both write old+1, so one update is lost.
                        unsafeTickCount++;

                        // SAFE: incrementAndGet is one indivisible (atomic) operation.
                        atomicTickCount.incrementAndGet();
                    }

                    // SAFEST: no shared variable at all. Each match returns its own count
                    // and the stream adds the numbers together.
                    return engine.getTicks();
                })
                .sum();

        long lost = streamTickCount - unsafeTickCount;

        return String.format(
                "Ticks counted across %d parallel matches:%n"
                        + "  shared long (unsafe)   %,d   <- lost %,d updates%n"
                        + "  AtomicLong             %,d%n"
                        + "  stream sum (no sharing) %,d",
                matches, unsafeTickCount, lost, atomicTickCount.get(), streamTickCount);
    }
}