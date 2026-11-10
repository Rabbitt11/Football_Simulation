package entities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import world.PlaySpace;

/*
 * Reads a team preset file. The format is:
 *
 *   Goalkeeper,1        <- exactly six player lines, in this role order
 *   Defender,2
 *   Defender,3
 *   Midfielder,4
 *   Midfielder,5
 *   Attacker,6
 *   formationShift=0.10 <- optional tactics, one key=value per line
 *   pressing=0.15
 *   ...
 */
public class TeamLoader {

    private static final String[] EXPECTED_ROLES = {
            "Goalkeeper", "Defender", "Defender",
            "Midfielder", "Midfielder", "Attacker"
    };

    private static final List<String> TACTIC_KEYS = List.of(
            "formationShift", "pressing", "shootingRange", "passUnderPressure");

    public static Player[] loadTeam(
            String filename,
            PlaySpace playSpace,
            boolean leftSide) throws IOException {

        int[] numbers = new int[EXPECTED_ROLES.length];

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            for (int i = 0; i < EXPECTED_ROLES.length; i++) {
                String line = reader.readLine();

                if (line == null) {
                    throw new IllegalArgumentException(
                            "The team file must contain six players.");
                }

                String[] parts = line.split(",", -1);

                if (parts.length != 2 ||
                        !parts[0].trim().equals(EXPECTED_ROLES[i])) {
                    throw new IllegalArgumentException(
                            "Invalid player role or format on line " + (i + 1));
                }

                numbers[i] = Integer.parseInt(parts[1].trim());

                if (numbers[i] <= 0) {
                    throw new IllegalArgumentException(
                            "Player numbers must be positive.");
                }

                for (int j = 0; j < i; j++) {
                    if (numbers[i] == numbers[j]) {
                        throw new IllegalArgumentException(
                                "Player numbers must be unique within a team.");
                    }
                }
            }

            // After the six players, only blank lines or key=value tactic lines are allowed.
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank() && !line.contains("=")) {
                    throw new IllegalArgumentException(
                            "Unexpected line after the six players: " + line);
                }
            }
        }

        Player[] players;

        if (leftSide) {
            players = TeamComp.createLeftTeam(playSpace);
        } else {
            players = TeamComp.createRightTeam(playSpace);
        }

        for (int i = 0; i < players.length; i++) {
            players[i].number = numbers[i];
        }

        return players;
    }

    /*
     * Reads the key=value lines of a preset into a Tactics object.
     * The tactic name is the file name, e.g. "aggressive" from aggressive.txt.
     *
     * Stream pipeline: lines -> keep "key=value" lines -> split -> collect into a Map.
     */
    public static Tactics loadTactics(String filename) throws IOException {
        Path path = Path.of(filename);
        String name = path.getFileName().toString().replaceFirst("\\.txt$", "");

        Map<String, Double> values;

        try {
            values = Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.contains("="))
                    .map(line -> line.split("=", 2))
                    .collect(Collectors.toMap(
                            pair -> pair[0].trim(),
                            pair -> Double.parseDouble(pair[1].trim())));

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A tactic value in " + filename + " is not a number", e);
        } catch (IllegalStateException e) {
            // Collectors.toMap throws this when the same key appears twice.
            throw new IllegalArgumentException("A tactic is listed twice in " + filename, e);
        }

        // Catch typos like "presing=0.2" instead of silently ignoring them.
        values.keySet().stream()
                .filter(key -> !TACTIC_KEYS.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new IllegalArgumentException("Unknown tactic '" + key + "' in " + filename);
                });

        return Tactics.fromMap(name, values);
    }
}