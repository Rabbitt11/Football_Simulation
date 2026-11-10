package app;

import java.awt.*;

import java.io.IOException;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.Timer;

import entities.Ball;
import entities.Player;
import entities.Tactics;
import entities.Team;
import entities.TeamComp;
import entities.TeamLoader;

import simulation.Match;
import simulation.MatchEngine;
import simulation.Ruleset;

import ui.Scoreboard;
import ui.SimulationPanel;
import ui.StatsBoard;

import world.GameMap;
import world.MapPreset;
import world.PlaySpace;

/*
 * Main - ONE match you can watch.
 *
 * All the game rules live in MatchEngine. Main only builds the teams, opens the
 * window, and calls engine.tick() 50 times a second. BatchMain uses the very
 * same engine with no window to play hundreds of matches.
 */
public class Main {

    // A short match so the showcase can see both halves: 2.5 minutes each.
    private static final int HALF_LENGTH_SECONDS = 150;

    public static void main(String[] args) {

        GameMap map = new GameMap(MapPreset.DEFAULT);
        PlaySpace playSpace = map.getPlaySpace();

        // Change these two lines to try different match-ups.
        String redPreset = "teams/presets/aggressive.txt";
        String bluePreset = "teams/presets/defensive.txt";

        Player[] redPlayers = loadTeamOrDefault(redPreset, playSpace, true);
        Player[] bluePlayers = loadTeamOrDefault(bluePreset, playSpace, false);
        Team redTeam = new Team("Red Team", Color.RED, redPlayers, loadTacticsOrDefault(redPreset));
        Team blueTeam = new Team("Blue Team", Color.BLUE, bluePlayers, loadTacticsOrDefault(bluePreset));

        Ball ball = new Ball(
                playSpace.getLength() / 2,
                playSpace.getWidth() / 2,
                0.0);

        Ruleset shortMatch = new Ruleset(
                HALF_LENGTH_SECONDS, HALF_LENGTH_SECONDS, 60, 6, false, false);

        // new Random() with no seed: a different match every time you run it.
        MatchEngine engine = new MatchEngine(map, redTeam, blueTeam, ball, shortMatch, new Random());
        Match match = engine.getMatch();

        // ---------- Observers (the coaches are already registered by the engine) ----------
        Scoreboard scoreboard = new Scoreboard(
                match.getGameState(), engine.getRedCoach(), engine.getBlueCoach());
        StatsBoard statsBoard = new StatsBoard();

        match.addListener(scoreboard);
        match.addListener(statsBoard);
        // A listener can also be a lambda, because MatchListener is a functional interface.
        match.addListener((goal, state) -> System.out.println("GOAL  " + goal + "   Score: " + state.getScore()));

        EventQueue.invokeLater(() -> {
            // Ensures that the GUI is created on the Event Dispatch Thread (EDT)
            JFrame frame = new JFrame("Football Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SimulationPanel panel = new SimulationPanel(map, redTeam, blueTeam, ball);
            panel.addOverlay(scoreboard);
            panel.addOverlay(statsBoard);

            // Set the preferred size of the panel to 1280x720 pixels due to map scaling issues
            panel.setPreferredSize(new Dimension(1280, 720));

            frame.add(panel);
            frame.pack();
            frame.setVisible(true);

            engine.start();

            // 50 ticks per second of real time. After the final whistle tick() does nothing,
            // but we keep repainting so the full-time banner and stats stay on screen.
            int tickMillis = (int) Math.round(MatchEngine.TICK_SECONDS * 1000);
            Timer gameLoop = new Timer(tickMillis, e -> {
                engine.tick(MatchEngine.TICK_SECONDS);
                panel.repaint();
            });
            gameLoop.start();
        });
    }

    private static Tactics loadTacticsOrDefault(String filename) {
        try {
            return TeamLoader.loadTactics(filename);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load tactics from " + filename + ": " + e.getMessage());
            System.out.println("Using balanced tactics.");
            return Tactics.balanced();
        }
    }

    private static Player[] loadTeamOrDefault(
            String filename,
            PlaySpace playSpace,
            boolean leftSide) {

        try {
            return TeamLoader.loadTeam(filename, playSpace, leftSide);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load " + filename + ": " + e.getMessage());
            System.out.println("Using the default team.");
        }

        if (leftSide) {
            return TeamComp.createLeftTeam(playSpace);
        } else {
            return TeamComp.createRightTeam(playSpace);
        }
    }
}