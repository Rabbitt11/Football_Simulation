package simulation;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import entities.Player;
import entities.PlayerStats;
import entities.Team;

/*
 * MatchStats - turns the players' running stats into reports, using collectors.
 *
 *  1. toTeamReport(team)  - a CUSTOM collector built with Collector.of(...)
 *  2. distanceByTeam(...) - built-in groupingBy + summarizingDouble
 *  3. hardWorkers(...)    - built-in partitioningBy + mapping
 */
public final class MatchStats {

    private MatchStats() {
        // utility class
    }

    /*
     * The mutable "bucket" the custom collector fills up, one player at a time.
     * It is private: outside code only ever sees the finished, immutable TeamReport.
     */
    private static final class Accumulator {
        int players = 0;
        double possessionSeconds = 0.0;
        int passes = 0;
        int shots = 0;
        double distanceMetres = 0.0;
        int topRunnerNumber = -1;
        double topRunnerMetres = -1.0;

        // accumulator: add one player into this bucket
        void add(Player player) {
            PlayerStats stats = player.getStats();
            players++;
            possessionSeconds += stats.getPossessionSeconds();
            passes += stats.getPasses();
            shots += stats.getShots();
            distanceMetres += stats.getDistanceMetres();

            if (stats.getDistanceMetres() > topRunnerMetres) {
                topRunnerMetres = stats.getDistanceMetres();
                topRunnerNumber = player.getNumber();
            }
        }

        // combiner: merge two buckets. Only used by PARALLEL streams, where
        // each thread fills its own bucket and the results are merged at the end.
        Accumulator merge(Accumulator other) {
            players += other.players;
            possessionSeconds += other.possessionSeconds;
            passes += other.passes;
            shots += other.shots;
            distanceMetres += other.distanceMetres;

            if (other.topRunnerMetres > topRunnerMetres) {
                topRunnerMetres = other.topRunnerMetres;
                topRunnerNumber = other.topRunnerNumber;
            }
            return this;
        }
    }

    /*
     * Custom collector: Stream<Player>  ->  TeamReport
     *
     *   supplier     Accumulator::new         make an empty bucket
     *   accumulator  Accumulator::add         put one player in
     *   combiner     Accumulator::merge       join two buckets (parallel streams)
     *   finisher     bucket -> new TeamReport freeze the result into an immutable report
     */
    public static Collector<Player, ?, TeamReport> toTeamReport(Team team) {
        return Collector.of(
                Accumulator::new,
                Accumulator::add,
                Accumulator::merge,
                bucket -> new TeamReport(
                        team.getName(),
                        team.getTactics().getName(),
                        bucket.players,
                        bucket.possessionSeconds,
                        bucket.passes,
                        bucket.shots,
                        bucket.distanceMetres,
                        bucket.topRunnerNumber,
                        bucket.topRunnerMetres));
    }

    public static TeamReport reportFor(Team team) {
        return Arrays.stream(team.getPlayers()).collect(toTeamReport(team));
    }

    // Built-in collectors: count, sum, min, average and max distance for each team in one pass.
    public static Map<Team, DoubleSummaryStatistics> distanceByTeam(Team... teams) {
        return allPlayers(teams).collect(Collectors.groupingBy(
                Player::getTeam,
                LinkedHashMap::new, // keep the teams in the order given
                Collectors.summarizingDouble(player -> player.getStats().getDistanceMetres())));
    }

    // Built-in collectors: split players into two groups (ran more than X metres, or not)
    // and turn each player into a short label like "Red #4".
    public static Map<Boolean, List<String>> hardWorkers(double metres, Team... teams) {
        return allPlayers(teams).collect(Collectors.partitioningBy(
                player -> player.getStats().getDistanceMetres() > metres,
                Collectors.mapping(
                        player -> player.getTeam().getName().split(" ")[0] + " #" + player.getNumber(),
                        Collectors.toList())));
    }

    private static Stream<Player> allPlayers(Team... teams) {
        return Arrays.stream(teams).flatMap(team -> Arrays.stream(team.getPlayers()));
    }

    // Lines of text for the on-screen stats box and the console.
    public static List<String> reportLines(Team red, Team blue) {
        TeamReport redReport = reportFor(red);
        TeamReport blueReport = reportFor(blue);

        Map<Team, DoubleSummaryStatistics> distance = distanceByTeam(red, blue);

        double matchAverage = allPlayers(red, blue)
                .mapToDouble(player -> player.getStats().getDistanceMetres())
                .average()
                .orElse(0.0);
        Map<Boolean, List<String>> workers = hardWorkers(matchAverage, red, blue);

        return List.of(
                redReport.summaryAgainst(blueReport),
                blueReport.summaryAgainst(redReport),
                String.format("Average distance per player:  %s %.0f m   %s %.0f m",
                        red.getName(), distance.get(red).getAverage(),
                        blue.getName(), distance.get(blue).getAverage()),
                String.format("Ran more than the match average (%.0f m):  %s",
                        matchAverage, String.join(", ", workers.get(true))));
    }
}